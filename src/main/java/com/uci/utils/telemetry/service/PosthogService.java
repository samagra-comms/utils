package com.uci.utils.telemetry.service;

import java.time.LocalDateTime;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class PosthogService {
	public WebClient getWebClient() {
		return WebClient.builder()
				.baseUrl(System.getenv("POSTHOG_TELEMETRY_URL"))
				.defaultHeader("Content-Type", "application/json")
				.build();
	}
	
	public Mono<String> sendDropOffEvent(String userId, String flow, Integer questionIndex, Long responseTime) {
		String distinctId = userId;
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode propertiesNode = mapper.createObjectNode();
		propertiesNode.put("question-index", questionIndex);
		propertiesNode.put("flow", flow);
		propertiesNode.put("response-time", responseTime);
		
		if(flow.equals("employerReg")) {
			distinctId = "recruiter:"+distinctId;
		} else if(flow.equals("candidateReg")) {
			distinctId = "candidate:"+distinctId;
		} 
		
		ObjectNode dataNode = mapper.createObjectNode();
		dataNode.put("properties", propertiesNode);
		dataNode.put("distinct_id", distinctId);
		
		return sendEvent("response", dataNode);
	}

	public Mono<String> sendTelemetryEvent(String userId, String telemetry) throws Exception{
		String distinctId = userId;
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(telemetry);
//			ObjectNode propertiesNode = mapper.createObjectNode();

		ObjectNode dataNode = mapper.createObjectNode();
		dataNode.put("properties", actualObj);
		dataNode.put("distinct_id", userId);

		return sendEvent("telemetry", dataNode);
	}
	
	@SuppressWarnings("deprecation")
	public Mono<String> sendEvent(String event, ObjectNode dataNode) {
		dataNode.put("api_key", System.getenv("POSTHOG_TELEMETRY_APIKEY"));
		dataNode.put("event", event);
		dataNode.put("type", "capture");
		
		log.info("json node:"+dataNode);
		
		return getWebClient().post()
			.uri(builder -> builder.path("batch/").build())
			.body(BodyInserters.fromObject(dataNode))
			.retrieve()
			.bodyToMono(String.class)
			.map(response -> {
				if (response != null) {
					log.info("Telemetry event sent successfully.");
				}
				return response;
			})
			.doOnError(throwable -> log.info("Error in sending telemetry event: " + throwable.getMessage()))
			.onErrorReturn("");
		
//		return Mono.just("");
		
	}
}
