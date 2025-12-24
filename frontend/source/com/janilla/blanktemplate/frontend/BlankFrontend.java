/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2025 Diego Schivo <diego.schivo@janilla.com>
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.net.Net;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;

public class BlankFrontend {

	public static final AtomicReference<BlankFrontend> INSTANCE = new AtomicReference<>();

	public static void main(String[] args) {
		try {
			BlankFrontend a;
			{
				var f = new DiFactory(Stream.of(BlankFrontend.class.getPackageName(), "com.janilla.web")
						.flatMap(x -> Java.getPackageClasses(x).stream()).toList(), INSTANCE::get);
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
				try (var x = Net.class.getResourceAsStream("localhost")) {
					c = Net.getSSLContext(Map.entry("JKS", x), "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty("blank-template.frontend.server.port"));
				s = a.diFactory.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected final Properties configuration;

	protected final DataFetching dataFetching;

	protected final DiFactory diFactory;

	protected final List<Path> files;

	protected final HttpHandler handler;

	protected final HttpClient httpClient;

	protected final IndexFactory indexFactory;

	protected final List<Invocable> invocables;

	protected final RenderableFactory renderableFactory;

	public BlankFrontend(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		configuration = diFactory.create(Properties.class, Collections.singletonMap("file", configurationFile));

		{
			SSLContext c;
			try (var x = Net.class.getResourceAsStream("localhost")) {
				c = Net.getSSLContext(Map.entry("JKS", x), "passphrase".toCharArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			httpClient = diFactory.create(HttpClient.class, Map.of("sslContext", c));
		}
		dataFetching = diFactory.create(DataFetching.class);
		indexFactory = diFactory.create(IndexFactory.class);

		files = Stream.of("com.janilla.frontend", "com.janilla.admin.frontend", BlankFrontend.class.getPackageName())
				.flatMap(x -> Java.getPackagePaths(x).stream().filter(Files::isRegularFile)).toList();
		invocables = types().stream()
				.flatMap(x -> Arrays.stream(x.getMethods())
						.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
						.map(y -> new Invocable(x, y)))
				.toList();
		renderableFactory = diFactory.create(RenderableFactory.class);
		{
			var f = diFactory.create(ApplicationHandlerFactory.class);
			handler = x -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			};
		}
	}

	public Properties configuration() {
		return configuration;
	}

	public DataFetching dataFetching() {
		return dataFetching;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public List<Path> files() {
		return files;
	}

	public HttpHandler handler() {
		return handler;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

	public List<Invocable> invocables() {
		return invocables;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public Collection<Class<?>> types() {
		return diFactory.types();
	}
}
