package com.janilla.blanktemplate.frontend;

import java.util.Map;
import java.util.stream.Stream;

import com.janilla.ioc.DiFactory;
import com.janilla.java.AnnotationAndElement;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

public class BlankRenderableFactory extends RenderableFactory {

	protected final Map<String, String> resourcePrefixes;

	public BlankRenderableFactory(ResourceMap resourceMap, DiFactory diFactory, Map<String, String> resourcePrefixes) {
		super(resourceMap, diFactory);
		this.resourcePrefixes = resourcePrefixes;
	}

	@Override
	protected Stream<String> resourceKeys(AnnotationAndElement<Render> render) {
		return super.resourceKeys(render).map(x -> {
			var y = x.startsWith("/") ? x
					: resourcePrefixes.get(((Class<?>) render.annotated()).getPackageName()) + "/" + x;
//			IO.println("BlankRenderableFactory.resourceKeys, x=" + x + ", y=" + y);
			return y;
		});
	}
}
