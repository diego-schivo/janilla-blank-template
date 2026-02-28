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

import java.net.URI;
import java.util.List;
import java.util.Properties;

import com.janilla.cms.User;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpCookie;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

public class BlankDataFetching {

	protected final String apiUrl;

	protected final HttpClient httpClient;

	protected final Converter converter;

	public BlankDataFetching(Properties configuration, String configurationKey, HttpClient httpClient,
			DiFactory diFactory) {
		apiUrl = configuration.getProperty(configurationKey + ".api.url");
		this.httpClient = httpClient;
		converter = diFactory.create(diFactory.actualType(Converter.class));
	}

	public User<?, ?> sessionUser(HttpCookie token) {
		var o = httpClient.getJson(URI.create(apiUrl + "/users/me"), token != null ? token.format() : null);
		return converter.convert(o, User.class);
	}

	public ListPortion<User<?, ?>> users(Long skip, Long limit) {
		var o = httpClient.getJson(URI
				.create(apiUrl + "/users?" + new UriQueryBuilder().append("skip", skip != null ? skip.toString() : null)
						.append("limit", limit != null ? limit.toString() : null)));
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(User.class)));
	}
}
