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
package com.janilla.blanktemplate.fullstack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.blanktemplate.backend.BackendHttpExchange;
import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.blanktemplate.frontend.BlankFrontend;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;

public class BlankFullstack {

	public static final ScopedValue<BlankFullstack> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DiFactory(Java.getPackageClasses(BlankFullstack.class.getPackageName(), true), "fullstack");
		serve(f, BlankFullstack.class, args.length > 0 ? args[0] : null);
	}

	protected static <T extends BlankFullstack> void serve(DiFactory diFactory, Class<T> applicationType,
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
			var p = a.configuration.getProperty(a.configurationKey() + ".fullstack.server.keystore.path");
			var w = a.configuration.getProperty(a.configurationKey() + ".fullstack.server.keystore.password");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			var f = Path.of(p);
			if (!Files.exists(f))
				Java.generateKeyPair(f, w);
			try (var s = Files.newInputStream(f)) {
				c = Java.sslContext(s, w.toCharArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey() + ".fullstack.server.port"));
			s = a.diFactory.create(HttpServer.class,
					Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final BlankBackend backend;

	protected final Properties configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final DiFactory diFactory;

	protected final BlankFrontend frontend;

	protected final HttpHandler handler;

	public BlankFullstack(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankFullstack(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.create(Properties.class, Collections.singletonMap("file", configurationFile));

		var cf = Optional.ofNullable(configurationFile).orElseGet(() -> {
			try {
				return Path.of(getClass().getResource("configuration.properties").toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});
		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DiFactory(backendTypes(), "backend");
			return f.create(BlankBackend.class,
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});
		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DiFactory(frontendTypes(), "frontend");
			return f.create(BlankFrontend.class,
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});

		handler = this::handle;
	}

	public BlankBackend backend() {
		return backend;
	}

	public Properties configuration() {
		return configuration;
	}

	public String configurationKey() {
		return configurationKey;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public BlankFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}

	protected List<Class<?>> backendTypes() {
		return Stream
				.of("com.janilla.web", "com.janilla.backend.cms", BlankBackend.class.getPackageName(),
						BlankFullstack.class.getPackageName())
				.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList();
	}

	protected List<Class<?>> frontendTypes() {
		return Stream.of("com.janilla.web", BlankFrontend.class.getPackageName(), BlankFullstack.class.getPackageName())
				.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList();
	}

	protected boolean handle(HttpExchange exchange) {
		var h = exchange instanceof BackendHttpExchange ? backend.handler() : frontend.handler();
		return h.handle(exchange);
	}
}
