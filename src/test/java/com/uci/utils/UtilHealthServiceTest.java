package com.uci.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.utils.kafka.KafkaConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = UtilsTestConfig.class)
class UtilHealthServiceTest {

	@Mock
	KafkaConfig kafkaConfig;

	@Mock
	BotService botService;

	@Mock
	ObjectMapper mapper;

	@InjectMocks
	UtilHealthService utilHealthService;

	@AfterAll
	static void teardown() {
		System.out.println("teardown");
	}

	@Test
	void getKafkaHealthNode() {
		Mockito.when(mapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
		Mockito.when(kafkaConfig.kafkaHealthIndicator()).thenReturn(() -> Health.up().build());
		JsonNode result = utilHealthService.getKafkaHealthNode().block();
		assertNotNull(result);
		assertEquals(result.get("status").textValue(), Status.UP.getCode());
	}

	@Test
	void getCampaignUrlHealthNode() {
		Mockito.when(mapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
		utilHealthService.campaignUrl = "NON_EXISTENT_URL";
		JsonNode result = utilHealthService.getCampaignUrlHealthNode().block();
		assertNotNull(result);
		assertEquals(result.get("status").textValue(), Status.DOWN.getCode());
	}
}
