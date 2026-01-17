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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.Index.Template;
import com.janilla.cms.CmsFrontend;
import com.janilla.frontend.Frontend;
import com.janilla.frontend.resources.FrontendResources;

public class IndexFactory {

	protected final Properties configuration;

	public IndexFactory(Properties configuration) {
		this.configuration = configuration;
	}

	public Index index(FrontendExchange exchange) {
		return new Index(imports(), configuration.getProperty("blank-template.api.url"), state(exchange), templates());
	}

	protected Map<String, Object> state(FrontendExchange exchange) {
		var x = new LinkedHashMap<String, Object>();
		x.put("user", exchange.sessionUser());
		return x;
	}

	protected Map<String, String> imports() {
		class A {
			private static Map<String, String> x;
		}
		if (A.x == null)
			synchronized (A.class) {
				if (A.x == null) {
					A.x = new LinkedHashMap<String, String>();
					Frontend.putImports(A.x);
					FrontendResources.putImports(A.x);
					CmsFrontend.putImports(A.x);
					Stream.of("app", "not-found", "page").forEach(x -> A.x.put(x, "/" + x + ".js"));
				}
			}
		return A.x;
	}

	protected List<Template> templates() {
		class A {
			private static List<Template> x;
		}
		if (A.x == null)
			synchronized (A.class) {
				if (A.x == null)
					A.x = Stream.of("app", "not-found", "page").map(x -> {
						try (var in = Index.class.getResourceAsStream(x + ".html")) {
							return new Template(x, new String(in.readAllBytes()));
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}).toList();
			}
		return A.x;
	}
}
