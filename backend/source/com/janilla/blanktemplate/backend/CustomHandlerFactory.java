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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.FileHandlerFactory;
import com.janilla.web.RenderableFactory;

public class CustomHandlerFactory extends ApplicationHandlerFactory {

	public CustomHandlerFactory(DiFactory diFactory, Collection<ClassAndMethod> methods,
			RenderableFactory renderableFactory, Collection<Path> files) {
		super(diFactory, methods, renderableFactory, files);
	}

	@Override
	protected List<HttpHandlerFactory> buildFactories() {
		var ff = new ArrayList<>(super.buildFactories());
		var i = IntStream.range(0, ff.size()).filter(x -> ff.get(x) instanceof FileHandlerFactory).findFirst()
				.getAsInt();
		ff.add(i, diFactory.create(CmsFileHandlerFactory.class));
		return ff;
	}
}
