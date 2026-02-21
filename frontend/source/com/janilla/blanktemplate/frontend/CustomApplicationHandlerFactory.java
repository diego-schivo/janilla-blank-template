package com.janilla.blanktemplate.frontend;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.ResourceHandlerFactory;

public class CustomApplicationHandlerFactory extends ApplicationHandlerFactory {

	public CustomApplicationHandlerFactory(DiFactory diFactory) {
		super(diFactory);
	}

	@Override
	protected List<HttpHandlerFactory> buildFactories() {
		return super.buildFactories().stream().flatMap(
				x -> x instanceof ResourceHandlerFactory ? Stream.of(x, buildDownloadHandlerFactory()) : Stream.of(x))
				.toList();
	}

	protected DownloadHandlerFactory buildDownloadHandlerFactory() {
		return Objects.requireNonNull(diFactory.create(diFactory.actualType(DownloadHandlerFactory.class)));
	}
}
