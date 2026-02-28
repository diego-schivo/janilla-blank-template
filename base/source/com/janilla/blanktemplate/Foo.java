package com.janilla.blanktemplate;

import java.util.function.UnaryOperator;

public interface Foo {

	public static final ScopedValue<UnaryOperator<String>> PROPERTY_GETTER = ScopedValue.newInstance();
}
