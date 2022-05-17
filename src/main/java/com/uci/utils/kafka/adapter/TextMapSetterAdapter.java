package com.uci.utils.kafka.adapter;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.header.Headers;

import io.opentelemetry.context.propagation.TextMapSetter;

public class TextMapSetterAdapter {
	public static TextMapSetter<Headers> setter = new TextMapSetter<Headers>() {
		@Override
		public void set(Headers headers, String key, String value) {
			headers.remove(key).add(key, value.getBytes(StandardCharsets.UTF_8));
		}
	};
}
