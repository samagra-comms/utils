package com.uci.utils.kafka.adapter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.Header;

import io.opentelemetry.context.propagation.TextMapGetter;

public class TextMapGetterAdapter {
	public static TextMapGetter<Headers> getter = new TextMapGetter<Headers>() {
		@Override
		public String get(Headers headers, String key) {
			Header header = headers.lastHeader(key);
			if (header == null) {
				return null;
			}
			byte[] value = header.value();
			if (value == null) {
				return null;
			}
			return new String(value, StandardCharsets.UTF_8);
		}

		@Override
		public Iterable<String> keys(Headers headers) {
			List<String> keyset = null;
			headers.forEach(h -> {
				String key = h.key();
				keyset.add(key);
			});
			return keyset;
		}
	};
}
