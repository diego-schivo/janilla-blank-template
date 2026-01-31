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
					diFactory.create(ReflectionJsonIterator.class, Map.of("object", value, "includeType", true)));
		}
	}

	@Render(template = "template")
	public record Template(String id, String content) {
	}
}
