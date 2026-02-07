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

import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;

public class BlankWebHandling {

	protected final BlankDataFetching dataFetching;

	protected final BlankIndexFactory indexFactory;

	public BlankWebHandling(BlankDataFetching dataFetching, BlankIndexFactory indexFactory) {
		this.dataFetching = dataFetching;
		this.indexFactory = indexFactory;
	}

	@Handle(method = "GET", path = "/admin(/[\\w\\d/-]*)?")
	public Object admin(String path, HttpExchange exchange) {
//		IO.println("WebHandling.admin, path=" + path);
//		if (path == null || path.isEmpty())
//			path = "/";
//		switch (path) {
//		case "/":
//			if (((BlankFrontendHttpExchange) exchange).sessionUser() == null)
//				return URI.create("/admin/login");
//			break;
//		case "/login":
//			if (((List<?>) dataFetching.users(0l, 1l)).isEmpty())
//				return URI.create("/admin/create-first-user");
//			break;
//		}
		return indexFactory.index(exchange);
	}

	@Handle(method = "GET", path = "/")
	public Object page(HttpExchange exchange) {
//		IO.println("WebHandling.page");
		return indexFactory.index(exchange);
	}
}
