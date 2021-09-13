package com.uci.utils.telemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.message.Message;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.uci.utils.CampaignService;
import com.uci.utils.telemetry.util.TelemetryEventNames;

public class LogTelemetryMessage implements Message {
	private final String requestBody;
	private final TelemetryEventNames eventName;
	private String botOrg = "";
	private String channel = "";
	private String provider = "";
	private String producerID = "";
	private String userID = "";
	private String campaign = "";
	private String conversationId = "";
	private String conversationOwnerId = "";
//	private CampaignService campaignService;
	
	public LogTelemetryMessage(String requestBody, TelemetryEventNames eventName, String botOrg, String channel, String provider, String producerID, String userID, String conversationId, String conversationOwnerId) {
		this.requestBody = requestBody;
		this.eventName = eventName;
		this.botOrg = botOrg;
		this.channel = channel;
		this.provider = provider;
		this.producerID = producerID;
		this.userID = userID;
		this.conversationId = conversationId;
		this.conversationOwnerId = conversationOwnerId;
	}

	public LogTelemetryMessage(String requestBody, TelemetryEventNames eventName, String botOrg, String channel, String provider, String producerID, String userID) {
		this.requestBody = requestBody;
		this.eventName = eventName;
		this.botOrg = botOrg;
		this.channel = channel;
		this.provider = provider;
		this.producerID = producerID;
		this.userID = userID;
	}
	
	public LogTelemetryMessage(String requestBody, TelemetryEventNames eventName) {
		this.requestBody = requestBody;
		this.eventName = eventName;
	}

	@Override
	public String getFormattedMessage() {
		LogTelemetryBuilder object = new LogTelemetryBuilder();
		
		String msgBody = object.build(requestBody, eventName, botOrg, channel, provider, producerID, userID, conversationId, conversationOwnerId);
		JSONObject jsonBody = new JSONObject(msgBody);

		return jsonBody.toString();
	}

	@Override
	public String getFormat() {
		return requestBody.toString();
	}

	@Override
	public Object[] getParameters() {
		return new Object[0];
	}

	@Override
	public Throwable getThrowable() {
		return null;
	}
}

