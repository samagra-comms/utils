package com.uci.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.uci.utils.kafka.KafkaConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = UtilsTestConfig.class)
class UtilHealthServiceTest {

	@Autowired
	UtilHealthService utilHealthService;

	@MockBean
	KafkaConfig kafkaConfig;

	@MockBean
	BotService botService;

	@AfterAll
	static void teardown() {
		System.out.println("teardown");
	}

	@Test
	void getKafkaHealthNode() {
		Mockito.when(kafkaConfig.kafkaHealthIndicator()).thenReturn(() -> Health.up().build());
		JsonNode result = utilHealthService.getKafkaHealthNode().block();
		assertNotNull(result);
		assertTrue(result.get("healthy").asBoolean());
	}

	@Test
	void getCampaignUrlHealthNode() {
		utilHealthService.campaignUrl = "NON_EXISTENT_URL";
		JsonNode result = utilHealthService.getCampaignUrlHealthNode().block();
		assertNotNull(result);
		assertFalse(result.get("healthy").asBoolean());
	}
}
