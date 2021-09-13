package com.uci.utils.telemetry.util;

public enum TelemetryEventTypes {
	CONVERSATION("conversation"), 
	SWITCHCONVERSATION("switch-conversation"),
	PRETRANSFORMER("pre-transformer"),
	POSTTRANSFORMER("post-transformer"),
	SENT("sent"),
	DELIVERED("delivered"),
	READ("read"),
	EXCEPTION("Exception");

	private String name;

	TelemetryEventTypes(String name) {
	    this.name = name;
	  }

	public String getName() {
		return name;
	}
}
