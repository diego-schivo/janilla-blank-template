/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.blanktemplate.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

public class BlankFrontend {

	public static final String[] DI_PACKAGES = { "com.janilla.web", "com.janilla.blanktemplate.frontend" };

	public static final ScopedValue<BlankFrontend> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageClasses(x, false).stream()).toList());
		serve(f, BlankFrontend.class, args.length > 0 ? args[0] : null);
	}

	protected static <T extends BlankFrontend> void serve(DiFactory diFactory, Class<T> applicationType,
			String configurationPath) {
		T a;
		{
			a = diFactory.create(applicationType,
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
		}

		var c = sslContext(a.configuration(), a.configurationKey);

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey + ".server.port"));
			s = a.diFactory.create(a.diFactory.actualType(HttpServer.class),
					Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected static <T extends BlankFrontend> SSLContext sslContext(Properties configuration,
			String configurationKey) {
		var p = configuration.getProperty(configurationKey + ".server.keystore.path");
		if (p == null)
			return HttpClient.sslContext("TLSv1.3");
		var w = configuration.getProperty(configurationKey + ".server.keystore.password");
		if (p.startsWith("~"))
			p = System.getProperty("user.home") + p.substring(1);
		var f = Path.of(p);
		if (!Files.exists(f)) {
			var cn = configuration.getProperty(configurationKey + ".server.keystore.common-name");
			var san = configuration.getProperty(configurationKey + ".server.keystore.subject-alternative-name");
			Java.generateKeyPair(cn != null ? cn : "localhost", f, w, san != null ? san : "dns:localhost,ip:127.0.0.1");
		}
		try (var s = Files.newInputStream(f)) {
			return Java.sslContext(s, w.toCharArray());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected final HttpHandlerFactory handlerFactory;

	protected final Properties configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final BlankDataFetching dataFetching;

	protected final DiFactory diFactory;

	protected final HttpHandler handler;

	protected final HttpClient httpClient;

	protected final BlankIndexFactory indexFactory;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	public BlankFrontend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.create(diFactory.actualType(Properties.class),
				Collections.singletonMap("file", configurationFile));

		httpClient = diFactory.create(diFactory.actualType(HttpClient.class),
				Map.of("sslContext", sslContext(configuration, configurationKey)));
		dataFetching = diFactory.create(diFactory.actualType(BlankDataFetching.class));

		resourceMap = diFactory.create(diFactory.actualType(ResourceMap.class), Map.of("paths", resourcePaths()));
		indexFactory = diFactory.create(diFactory.actualType(BlankIndexFactory.class));

		invocationResolver = diFactory.create(diFactory.actualType(InvocationResolver.class),
				Map.of("invocables",
						diFactory.types().stream()
								.flatMap(x -> Arrays.stream(x.getMethods())
										.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
										.map(y -> new Invocable(x, y)))
								.toList(),
						"instanceResolver", (Function<Class<?>, Object>) x -> {
							var y = diFactory.context();
//							IO.println("x=" + x + ", y=" + y);
							return x.isAssignableFrom(y.getClass()) ? diFactory.context()
									: diFactory.create(diFactory.actualType(x));
						}));
		renderableFactory = diFactory.create(diFactory.actualType(RenderableFactory.class));
		handlerFactory = diFactory.create(diFactory.actualType(ApplicationHandlerFactory.class));
		handler = this::handle;
	}

	public Properties configuration() {
		return configuration;
	}

	public String configurationKey() {
		return configurationKey;
	}

	public BlankDataFetching dataFetching() {
		return dataFetching;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public HttpHandler handler() {
		return handler;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public BlankIndexFactory indexFactory() {
		return indexFactory;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	protected boolean handle(HttpExchange exchange) {
		return ScopedValue.where(INSTANCE, this).call(() -> {
			var h = handlerFactory
					.createHandler(exchange.exception() != null ? exchange.exception() : exchange.request());
			if (h == null)
				throw new NotFoundException(exchange.request().getMethod() + " " + exchange.request().getTarget());
			return h.handle(exchange);
		});
	}

	protected Map<String, List<Path>> resourcePaths() {
		var pp1 = Java.getPackagePaths("com.janilla.frontend", false).filter(Files::isRegularFile).toList();
		var pp2 = Stream.of("com.janilla.frontend.cms", BlankFrontend.class.getPackageName())
				.flatMap(x -> Java.getPackagePaths(x, false).filter(Files::isRegularFile)).toList();
		return Map.of("/base", pp1, "", pp2);
	}
}
