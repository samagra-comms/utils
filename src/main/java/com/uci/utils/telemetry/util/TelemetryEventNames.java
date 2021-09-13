package com.uci.utils.telemetry.util;

public enum TelemetryEventNames {
	STARTCONVERSATION("Start of a Conversation"),
	ENDCONVERSATION("End of a Conversation"),
	SWITCHCONVERSATIONLOGIC("Switch from one Conversation Logic to another"),
	PRETRANSFORMER("Pre transformer"),
	POSTTRANSFORMER("Post transformer"),
	SENT("Sent"),
	DELIVERED("Delivered"),
	READ("Read"),
	AUDITEXCEPTIONS("Audit - Exceptions");
	
	private String name;

	TelemetryEventNames(String name) {
	    this.name = name;
	  }

	public String getName() {
		return name;
	}
}
