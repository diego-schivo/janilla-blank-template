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

import java.util.List;
import java.util.Map;

import com.janilla.ioc.DiFactory;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.web.Render;
import com.janilla.web.Renderer;

public interface Index {

	String title();

	Map<String, String> imports();

	String key();

	String apiUrl();

	Map<String, Object> state();

	List<Template> templates();

	public static class JsonRenderer<T> extends Renderer<T> {

		@Override
		public String apply(T value) {
			return Json.format(value);
		}
	}

	public static class StateRenderer<T> extends Renderer<T> {

		protected final DiFactory diFactory;

		public StateRenderer(DiFactory diFactory) {
			this.diFactory = diFactory;
		}

		@Override
		public String apply(T value) {
			return Json.format(
					diFactory.create(diFactory.actualType(ReflectionJsonIterator.class), Map.of("object", value, "includeType", true)));
		}
	}

	@Render(template = "template")
	public record Template(String id, String content) {
	}
}
