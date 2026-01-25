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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.Index.Template;
import com.janilla.frontend.ImportMaps;
import com.janilla.frontend.cms.CmsImportMaps;
import com.janilla.frontend.resources.FrontendResources;
import com.janilla.http.HttpExchange;
import com.janilla.web.DefaultResource;
import com.janilla.web.ResourceMap;

public class BlankIndexFactory {

	protected final Properties configuration;

	protected final String configurationKey;

	protected final BlankDataFetching dataFetching;

	protected final ResourceMap resourceMap;

	protected Map<String, String> imports;

	protected List<Template> templates;

	public BlankIndexFactory(Properties configuration, String configurationKey, BlankDataFetching dataFetching,
			ResourceMap resourceMap) {
		this.configuration = configuration;
		this.configurationKey = configurationKey;
		this.dataFetching = dataFetching;
		this.resourceMap = resourceMap;
	}

	public Index index(HttpExchange exchange) {
		return new BlankIndex(configuration.getProperty(configurationKey + ".title"), imports(),
				configuration.getProperty(configurationKey + ".api.url"), state(exchange), templates());
	}

	protected Map<String, Object> state(HttpExchange exchange) {
		var x = new LinkedHashMap<String, Object>();
		x.put("user", ((BlankFrontendHttpExchange) exchange).sessionUser());
		return x;
	}

	protected Map<String, String> imports() {
		if (imports == null)
			synchronized (this) {
				if (imports == null) {
					imports = new LinkedHashMap<String, String>();
					putImports(imports);
				}
			}
		return imports;
	}

	protected void putImports(Map<String, String> map) {
		ImportMaps.putImports(map);
		FrontendResources.putImports(map);
		CmsImportMaps.putImports(map);
		Stream.of("blank-app", "not-found", "page").forEach(x -> map.put(x, "/" + x + ".js"));
	}

	protected List<Template> templates() {
		if (templates == null)
			synchronized (this) {
				if (templates == null) {
					templates = new ArrayList<Template>();
					addTemplates(templates);
				}
			}
		return templates;
	}

	protected void addTemplates(List<Template> list) {
		Stream.of("app", "not-found", "page").map(this::template).forEach(list::add);
	}

	public Template template(String name) {
		var f = (DefaultResource) resourceMap.get("/" + name + ".html");
		try (var in = f != null ? f.newInputStream() : null) {
			return in != null ? new Template(name, new String(in.readAllBytes())) : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
