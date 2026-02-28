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
package com.janilla.blanktemplate.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.backend.cms.Cms;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.blanktemplate.Foo;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.Java;
import com.janilla.java.TypeResolver;
import com.janilla.persistence.Store;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;

public class BlankBackend {

	public static final String[] DI_PACKAGES = { "com.janilla.web", "com.janilla.backend.cms",
			"com.janilla.blanktemplate", "com.janilla.blanktemplate.backend" };

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageClasses(x, false).stream()).toList());
		serve(f, BlankBackend.class, args.length > 0 ? args[0] : null);
	}

	protected static <T extends BlankBackend> void serve(DiFactory diFactory, Class<T> applicationType,
			String configurationPath) {
		T a;
		{
			a = diFactory.create(applicationType,
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
		}

		SSLContext c;
		{
			var p = a.configuration.getProperty(a.configurationKey + ".server.keystore.path");
			if (p != null) {
				var w = a.configuration.getProperty(a.configurationKey + ".server.keystore.password");
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var f = Path.of(p);
				if (!Files.exists(f)) {
					var cn = a.configuration.getProperty(a.configurationKey + ".server.keystore.common-name");
					var san = a.configuration
							.getProperty(a.configurationKey + ".server.keystore.subject-alternative-name");
					Java.generateKeyPair(cn != null ? cn : "localhost", f, w,
							san != null ? san : "dns:localhost,ip:127.0.0.1");
				}
				try (var s = Files.newInputStream(f)) {
					c = Java.sslContext(s, w.toCharArray());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else
				c = HttpClient.sslContext("TLSv1.3");
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey + ".server.port"));
			s = a.diFactory.create(a.diFactory.actualType(HttpServer.class),
					Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final HttpHandlerFactory handlerFactory;

	protected final Properties configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final Predicate<HttpExchange> drafts = this::testDrafts;

	protected final DiFactory diFactory;

	protected final HttpHandler handler;

	protected final boolean includeType;

	protected final InvocationResolver invocationResolver;

	protected final Persistence persistence;

	protected final RenderableFactory renderableFactory;

	protected final List<Class<?>> resolvables;

	protected final List<Class<?>> storables;

	protected final TypeResolver typeResolver;

	public BlankBackend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
//		IO.println("BlankBackend, configurationFile=" + configurationFile + ", configurationKey=" + configurationKey);
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.create(diFactory.actualType(Properties.class),
				Collections.singletonMap("file", configurationFile));

		{
			Map<String, Class<?>> m = diFactory.types().stream()
					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
			resolvables = m.values().stream().toList();
		}
		typeResolver = diFactory.create(diFactory.actualType(DollarTypeResolver.class));

		storables = resolvables.stream().filter(x -> x.isAnnotationPresent(Store.class)).toList();
		{
			var f = configuration.getProperty(configurationKey + ".database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			var b = diFactory.create(diFactory.actualType(PersistenceBuilder.class),
					Map.of("databaseFile", Path.of(f)));
			persistence = b.build(diFactory);
		}

		includeType = true;
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
									: diFactory.create(diFactory.actualType(x),
											Map.of("invocationResolver", InvocationResolver.INSTANCE.get()));
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

	public Predicate<HttpExchange> drafts() {
		return drafts;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public HttpHandler handler() {
		return handler;
	}

	public boolean includeType() {
		return includeType;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public Persistence persistence() {
		return persistence;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public List<Class<?>> resolvables() {
		return resolvables;
	}

	public List<Class<?>> storables() {
		return storables;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		return Cms.schema(dataClass(), diFactory::actualType);
	}

	protected Class<?> dataClass() {
		return Data.class;
	}

	protected boolean handle(HttpExchange exchange) {
//		IO.println("BlankBackend.handle, exchange=" + exchange);
		return ScopedValue.where(Foo.PROPERTY_GETTER, x -> configuration.getProperty(configurationKey + "." + x))
				.call(() -> {
					var h = handlerFactory
							.createHandler(exchange.exception() != null ? exchange.exception() : exchange.request());
					if (h == null)
						throw new NotFoundException(
								exchange.request().getMethod() + " " + exchange.request().getTarget());
					return h.handle(exchange);
				});
	}

	protected boolean testDrafts(HttpExchange x) {
		var u = x instanceof BackendHttpExchange y ? y.sessionUser() : null;
		return u != null;
	}
}
