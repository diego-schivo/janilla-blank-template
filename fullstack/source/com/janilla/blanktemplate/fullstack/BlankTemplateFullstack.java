/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
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

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.blanktemplate.backend.BlankTemplateBackend;
import com.janilla.blanktemplate.frontend.BlankTemplateFrontend;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.net.Net;

public class BlankTemplateFullstack {

	public static final AtomicReference<BlankTemplateFullstack> INSTANCE = new AtomicReference<>();

	public static void main(String[] args) {
		try {
			BlankTemplateFullstack a;
			{
				var f = new DiFactory(Java.getPackageClasses(BlankTemplateFullstack.class.getPackageName()),
						BlankTemplateFullstack.INSTANCE::get, "fullstack");
				a = f.create(BlankTemplateFullstack.class,
						Java.hashMap("diFactory", f, "configurationFile",
								args.length > 0 ? Path.of(
										args[0].startsWith("~") ? System.getProperty("user.home") + args[0].substring(1)
												: args[0])
										: null));
			}

			HttpServer s;
			{
				SSLContext c;
				try (var x = Net.class.getResourceAsStream("testkeys")) {
					c = Net.getSSLContext(Map.entry("JKS", x), "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty("blank-template.fullstack.server.port"));
				s = a.diFactory.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected final BlankTemplateBackend backend;

	protected final Properties configuration;

	protected final DiFactory diFactory;

	protected final BlankTemplateFrontend frontend;

	protected final HttpHandler handler;

	public BlankTemplateFullstack(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		configuration = diFactory.create(Properties.class, Collections.singletonMap("file", configurationFile));

		var cf = Optional.ofNullable(configurationFile).orElseGet(() -> {
			try {
				return Path.of(BlankTemplateFullstack.class.getResource("configuration.properties").toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});
		backend = diFactory.create(BlankTemplateBackend.class,
				Java.hashMap("diFactory",
						new DiFactory(
								Stream.of("fullstack", "backend")
										.flatMap(x -> Java.getPackageClasses(BlankTemplateBackend.class.getPackageName()
												.replace(".backend", "." + x)).stream())
										.toList(),
								BlankTemplateBackend.INSTANCE::get, "backend"),
						"configurationFile", cf));
		frontend = diFactory.create(BlankTemplateFrontend.class,
				Java.hashMap("diFactory",
						new DiFactory(
								Stream.of("fullstack", "frontend")
										.flatMap(x -> Java.getPackageClasses(BlankTemplateFrontend.class
												.getPackageName().replace(".frontend", "." + x)).stream())
										.toList(),
								BlankTemplateFrontend.INSTANCE::get, "frontend"),
						"configurationFile", cf));

		handler = x -> {
//			IO.println("BlankTemplateFullstack, " + x.request().getPath());
//			var h = switch (Objects.requireNonNullElse(x.exception(), x.request())) {
//			case HttpRequest y -> y.getPath().startsWith("/api/") ? backend.handler() : frontend.handler();
//			case Exception _ -> backend.handler();
//			default -> null;
//			};
			var h = x instanceof com.janilla.blanktemplate.backend.CustomHttpExchange ? backend.handler()
					: frontend.handler();
			return h.handle(x);
		};
	}

	public BlankTemplateBackend backend() {
		return backend;
	}

	public Properties configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public BlankTemplateFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}
}
