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
package com.janilla.blanktemplate.backend;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.NullTypeResolver;
import com.janilla.java.TypeResolver;
import com.janilla.web.HandleException;
import com.janilla.web.Invocable;
import com.janilla.web.Invocation;
import com.janilla.web.InvocationHandlerFactory;
import com.janilla.web.RenderableFactory;

public class CustomMethodHandlerFactory extends InvocationHandlerFactory {

	public static final AtomicReference<CustomMethodHandlerFactory> INSTANCE = new AtomicReference<>();

	protected static final Set<String> USER_POST = Set.of("/api/users/first-register", "/api/users/forgot-password",
			"/api/users/login", "/api/users/reset-password");

	protected final Properties configuration;

	protected final DiFactory diFactory;

	public CustomMethodHandlerFactory(List<Invocable> invocables, Function<Class<?>, Object> instanceResolver,
			Comparator<Invocation> invocationComparator, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory, Properties configuration, DiFactory diFactory) {
		super(invocables, instanceResolver, invocationComparator, renderableFactory, rootFactory);
		this.configuration = configuration;
		this.diFactory = diFactory;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.request();
		if (rq.getPath().startsWith("/api/") && !rq.getMethod().equals("GET")) {
			if (rq.getMethod().equals("OPTIONS") || USER_POST.contains(rq.getPath()))
				;
			else
				((CustomHttpExchange) exchange).requireSessionEmail();
		}

		if (Boolean.parseBoolean(configuration.getProperty("blank-template.live-demo"))) {
			if (rq.getMethod().equals("GET") || USER_POST.contains(rq.getPath()))
				;
			else
				throw new HandleException(new MethodBlockedException());
		}

		var rs = exchange.response();
		rs.setHeaderValue("access-control-allow-origin", configuration.getProperty("blank-template.api.cors.origin"));
		rs.setHeaderValue("access-control-allow-credentials", "true");

//		if (r.getPath().startsWith("/api/"))
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		return super.handle(invocation, exchange);
	}

	@Override
	protected Converter converter(Class<? extends TypeResolver> type) {
		return diFactory
				.create(Converter.class,
						type != DollarTypeResolver.class
								? Collections.singletonMap("typeResolver",
										type != null && type != NullTypeResolver.class ? diFactory.create(type) : null)
								: null);
	}

	protected List<String> handleMethods(String path) {
		return invocationGroups(path).flatMap(x -> x.methods().keySet().stream()).toList();
	}
}
