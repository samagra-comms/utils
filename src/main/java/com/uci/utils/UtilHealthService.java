package com.uci.utils;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	
	/**
	 * Returns kafka health node with kafka health & details
	 * 
	 * @return JsonNode
	 */
	public Mono<JsonNode> getKafkaHealthNode() {
		return Mono.fromCallable(() -> {
			HealthIndicator kafkaHealthIndicator = kafkaConfig.kafkaHealthIndicator();
			Boolean kafkaHealthy = getIsKafkaHealthy(kafkaHealthIndicator);

			/* Result node */
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree("{\"healthy\":false}");

			/* Add data in result node */
			((ObjectNode) jsonNode).put("healthy", kafkaHealthy);
			if(kafkaHealthy) {
				JsonNode kafkaDetailNode = getKafkaHealthDetailsNode(kafkaHealthIndicator);
				((ObjectNode) jsonNode).set("details", kafkaDetailNode);
			}
			return jsonNode;
		});
	}
	
	/**
	 * Returns Campaign url health 
	 *
	 * @return Mono<JsonNode>
	 */
	public Mono<JsonNode> getCampaignUrlHealthNode() {
		return getIsCampaignHealthy();
	}

	/**
	 * Returns the combined health of kafka and campaign.
	 *
	 * @return Returns details of each service in `checks` key and
	 * the overall health status in `healthy` key.
	 */
	public Mono<JsonNode> getAllHealthNode() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resultNode = mapper.createObjectNode();

		/* Kafka health info */
		Mono<JsonNode> kafkaNode = getKafkaHealthNode().map(result -> {
			ObjectNode objectNode = mapper.createObjectNode();
			objectNode.put("name", "Kafka");
			objectNode.set("healthy", result.get("healthy"));
			objectNode.set("details", result.get("details"));
			return  objectNode;
		});

		/* Campaign health info. */
		Mono<JsonNode> campaignNode = getCampaignUrlHealthNode().map(result -> {
			ObjectNode objectNode = mapper.createObjectNode();
			objectNode.put("name", "campaign");
			objectNode.set("healthy", result.get("healthy"));
			return  objectNode;
		});

		return Mono.zip(kafkaNode, campaignNode).map(results -> {
			resultNode.putArray("checks")
					.add(results.getT1())
					.add(results.getT2());
			resultNode.put("healthy",
					results.getT1().get("healthy").booleanValue() &&
					results.getT2().get("healthy").booleanValue()
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
	
	/**
	 * Returns kafka health in boolean
	 * 
	 * @param kafkaHealthIndicator
	 * @return Boolean
	 */
	private Boolean getIsKafkaHealthy(HealthIndicator kafkaHealthIndicator) {
		return kafkaHealthIndicator.getHealth(true).getStatus().toString().equals("UP");
	}
	
	/**
	 * Get is campaign url healthy or not
	 * 
	 * @return Boolean
	 */
	private Mono<JsonNode> getIsCampaignHealthy() {
    	ObjectMapper mapper = new ObjectMapper();
		JsonNode failed = mapper.createObjectNode().put("healthy", "false");
    	try {
	    	WebClient webClient = WebClient.create(campaignUrl);
			return webClient.get()
	    			.uri(builder -> builder.path("admin/v1/health").build())
	    			.retrieve()
	                .bodyToMono(JsonNode.class)
					.onErrorResume(e -> Mono.just(mapper.createObjectNode().set("result", failed)))
					.map(jsonNode -> mapper.createObjectNode().set("healthy", jsonNode.path("result").get("healthy")));
		} catch (Exception e) {
			log.info(e.getMessage());
			return Mono.just(failed);
		}
    }
}
