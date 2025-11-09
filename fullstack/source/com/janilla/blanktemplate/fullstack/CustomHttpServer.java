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

import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import com.janilla.blanktemplate.backend.BlankTemplateBackend;
import com.janilla.blanktemplate.frontend.BlankTemplateFrontend;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.HttpServer;
import com.janilla.ioc.Context;

@Context("fullstack")
public class CustomHttpServer extends HttpServer {

	protected final BlankTemplateBackend backend;

	protected final BlankTemplateFrontend frontend;

	public CustomHttpServer(SSLContext sslContext, SocketAddress endpoint, HttpHandler handler,
			BlankTemplateBackend backend, BlankTemplateFrontend frontend) {
		super(sslContext, endpoint, handler);
		this.backend = backend;
		this.frontend = frontend;
	}

	@Override
	protected HttpExchange createExchange(HttpRequest request, HttpResponse response) {
//		IO.println("CustomHttpServer.createExchange, request.getPath()=" + request.getPath());
		var f = request.getPath().startsWith("/api/") ? backend.diFactory() : frontend.diFactory();
		return Optional.ofNullable(f.create(HttpExchange.class, Map.of("request", request, "response", response)))
				.orElseGet(() -> super.createExchange(request, response));
	}
}
