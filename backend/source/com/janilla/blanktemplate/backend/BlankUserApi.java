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

import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import com.janilla.backend.cms.UserApi;
import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;

@Handle(path = "/api/users")
public class BlankUserApi extends UserApi<Long, BlankUserRole, BlankUser> {

	public BlankUserApi(Predicate<HttpExchange> drafts, Persistence persistence, Properties configuration,
			String configurationKey) {
		super(BlankUser.class, drafts, persistence, configuration.getProperty(configurationKey + ".jwt.key"));
	}

	@Override
	public BlankUser firstRegister(CreateData<BlankUser> data, UserHttpExchange exchange) {
		var u = data.user().withRoles(Set.of(BlankUserRole.ADMIN));
		data = data.withUser(u);
		return super.firstRegister(data, exchange);
	}
}
