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
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.net.SecureServer;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

public class BlankFrontend {

	public static final ScopedValue<BlankFrontend> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		try {
			BlankFrontend a;
			{
				var f = new DiFactory(Stream.of("com.janilla.web", BlankFrontend.class.getPackageName())
						.flatMap(x -> Java.getPackageClasses(x, true).stream()).toList());
				a = f.create(BlankFrontend.class,
						Java.hashMap("diFactory", f, "configurationFile",
								args.length > 0 ? Path.of(
										args[0].startsWith("~") ? System.getProperty("user.home") + args[0].substring(1)
												: args[0])
										: null));
			}

			HttpServer s;
			{
				SSLContext c;
				try (var x = SecureServer.class.getResourceAsStream("localhost")) {
					c = Java.sslContext(x, "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey() + ".frontend.server.port"));
				s = a.diFactory.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected final Properties configuration;

	protected final String configurationKey;

	protected final BlankDataFetching dataFetching;

	protected final DiFactory diFactory;

	protected final HttpHandler handler;

	protected final HttpClient httpClient;

	protected final BlankIndexFactory indexFactory;

	protected final List<Invocable> invocables;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	public BlankFrontend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.create(Properties.class, Collections.singletonMap("file", configurationFile));

		{
			SSLContext c;
			try (var x = SecureServer.class.getResourceAsStream("localhost")) {
				c = Java.sslContext(x, "passphrase".toCharArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			httpClient = diFactory.create(HttpClient.class, Map.of("sslContext", c));
		}
		dataFetching = diFactory.create(BlankDataFetching.class);

		resourceMap = diFactory.create(ResourceMap.class, Map.of("paths", resourcePaths()));
		indexFactory = diFactory.create(BlankIndexFactory.class);

		invocables = diFactory.types().stream()
				.flatMap(x -> Arrays.stream(x.getMethods())
						.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
						.map(y -> new Invocable(x, y)))
				.toList();
		renderableFactory = diFactory.create(RenderableFactory.class);
		{
			var f = diFactory.create(ApplicationHandlerFactory.class);
			handler = x -> ScopedValue.where(INSTANCE, this).call(() -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			});
		}
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

	public List<Invocable> invocables() {
		return invocables;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	protected Map<String, List<Path>> resourcePaths() {
		return Map.of("", Stream.of("com.janilla.frontend", BlankFrontend.class.getPackageName())
				.flatMap(x -> Java.getPackagePaths(x, true).filter(Files::isRegularFile)).toList());
	}
}
