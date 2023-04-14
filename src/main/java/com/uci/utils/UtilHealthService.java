package com.uci.utils;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.utils.kafka.KafkaConfig;

import com.uci.utils.model.ApiResponse;
import com.uci.utils.model.ApiResponseParams;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Slf4j
@Service
public class UtilHealthService {
	@Value("${campaign.url}")
	String campaignUrl;
	
	@Autowired
	private KafkaConfig kafkaConfig;
	
	@Autowired
	private BotService botService;

	@Autowired
	private ObjectMapper mapper;
	
	/**
	 * Returns kafka health node with kafka health & details
	 * 
	 * @return JsonNode
	 */
	public Mono<JsonNode> getKafkaHealthNode() {
		return Mono.fromCallable(() -> {
			Health kafkaHealth = kafkaConfig.kafkaHealthIndicator().getHealth(true);
			ObjectNode result = mapper.createObjectNode();
			result.put("status", kafkaHealth.getStatus().toString());
			if (kafkaHealth.getStatus().equals(Status.DOWN)) {
				result.put("message", kafkaHealth.getDetails().get("error").toString());
			}
			return result;
		});
	}
	
	/**
	 * Returns Campaign url health 
	 *
	 * @return Mono<JsonNode>
	 */
	public Mono<JsonNode> getCampaignUrlHealthNode() {
		ObjectNode failed = mapper.createObjectNode().put("status", Status.DOWN.getCode());
		try {
			WebClient webClient = WebClient.create(campaignUrl);
			return webClient.get()
					.uri(builder -> builder.path("admin/v1/health").build())
					.retrieve()
					.bodyToMono(JsonNode.class)
					.onErrorResume(e -> {
						failed.put("message", e.getMessage());
						return Mono.just(mapper.createObjectNode().set("result", failed));
					})
					.map(jsonNode -> {
						ObjectNode result = mapper.createObjectNode();
						result.put("status", jsonNode.get("result").get("status").textValue());
						if (jsonNode.get("result").get("status").textValue().equals(Status.DOWN.getCode())) {
							result.set("message", jsonNode.get("result").get("message"));
						}
						return result;
					});
		} catch (Exception e) {
			log.info(e.getMessage());
			failed.put("message", e.getMessage());
			return Mono.just(failed);
		}
	}

	/**
	 * Returns the combined health of kafka and campaign.
	 *
	 * @return Returns details of each service in `details` key and
	 * the overall health status in `status` key.
	 */
	public Mono<JsonNode> getAllHealthNode() {
		ObjectNode resultNode = mapper.createObjectNode();

		/* Kafka health info */
		Mono<JsonNode> kafkaNode = getKafkaHealthNode();

		/* Campaign health info. */
		Mono<JsonNode> campaignNode = getCampaignUrlHealthNode();

		return Mono.zip(kafkaNode, campaignNode).map(results -> {
			ObjectNode detailsNode = mapper.createObjectNode();
			detailsNode.set("kafka", results.getT1());
			detailsNode.set("campaign", results.getT2());
			resultNode.set("details", detailsNode);
			resultNode.put("status",
					results.getT1().get("status").textValue().equals(Status.UP.getCode()) &&
					results.getT2().get("status").textValue().equals(Status.UP.getCode())
					? Status.UP.getCode() : Status.DOWN.getCode()
			);
			return resultNode;
		});
	}
	
	/**
	 * Returns kafka health details node
	 * 
	 * @param kafkaHealthIndicator
	 * @return JsonNode
	 */
	private JsonNode getKafkaHealthDetailsNode(HealthIndicator kafkaHealthIndicator) {
		Map<String, Object> kafkaDetails = kafkaHealthIndicator.health().getDetails();
		
		ObjectMapper mapper = new ObjectMapper();
        ObjectNode kafkaDetailNode = mapper.createObjectNode();
        for(Map.Entry<String, Object> entry : kafkaDetails.entrySet()){
        	kafkaDetailNode.put(entry.getKey(), entry.getValue().toString());
        };
        
        return kafkaDetailNode;
	}
}
