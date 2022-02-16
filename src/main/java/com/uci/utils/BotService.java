package com.uci.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.bot.util.BotUtil;

import ch.qos.logback.core.Context;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.api.ApplicationResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.cache.CacheMono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SuppressWarnings("ALL")
@Service
@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class BotService {

	public WebClient webClient;
	public FusionAuthClient fusionAuthClient;
	private Cache<Object, Object> cache;
	
//	public BotService(WebClient webClient, FusionAuthClient fusionAuthClient, Caffeine<Object, Object> caffeineCacheBuilder) {
//		this.webClient = webClient;
//		this.fusionAuthClient = fusionAuthClient;
//		this.caffeineCacheBuilder = caffeineCacheBuilder;
//		this.cache = caffeineCacheBuilder.build();
//	}

//	private Mono<Object> test(Cache cacheObj, String cacheName, Mono<Object> methodCall) {
//		return CacheMono.lookup(key -> Mono.justOrEmpty(this.cache.getIfPresent(key))
//				.map(Signal::next), cacheName)
//			.onCacheMissResume(() -> methodCall)
//			.andWriteWith((key, signal) -> Mono.fromRunnable(
//					() -> Optional.ofNullable(signal.get()).ifPresent(value -> this.cache.put(key, value))))
//			.log("cache");
//	}
	
	/**
	 * Retrieve Bot Node from Starting Message 
	 * @param startingMessage
	 * @return
	 */
	public Mono<JsonNode> getBotFromStartingMessage(String startingMessage) {
		String cacheKey = "bot-for-starting-message:" + startingMessage;
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache.getIfPresent(cacheKey) != null ? (JsonNode) cache.getIfPresent(key) : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
						.uri(builder -> builder.path("admin/v1/bot/getByParam/").queryParam("startingMessage", startingMessage).build())
						.retrieve().bodyToMono(String.class).map(response -> {
							if (response != null) {
								log.info(response);
								try {
									ObjectMapper mapper = new ObjectMapper();
									JsonNode root = mapper.readTree(response);
									String responseCode = root.path("responseCode").asText();
									if (isApiResponseOk(responseCode)) {
										return root;
									}
									return new ObjectMapper().createObjectNode();
								} catch (JsonProcessingException jsonMappingException) {
									return new ObjectMapper().createObjectNode();
								}

							} else {
								return new ObjectMapper().createObjectNode();
							}
						})
						.doOnError(throwable -> log.info("Error in getting campaign: " + throwable.getMessage()))
						.onErrorReturn(new ObjectMapper().createObjectNode())
						)
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
	}

	
	/**
	 * Retrieve Campaign Params From its Name
	 *
	 * @param startingMessage - Starting Message
	 * @return Application
	 */
	public Mono<String> getCampaignFromStartingMessage(String startingMessage) {
		String cacheKey = "bot-name-for-starting-message:" + startingMessage;
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache.getIfPresent(cacheKey) != null ? cache.getIfPresent(key).toString() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
						.uri(builder -> builder.path("admin/v1/bot/getByParam/").queryParam("startingMessage", startingMessage).build())
						.retrieve().bodyToMono(String.class).map(response -> {
							if (response != null) {
								log.info(response);
								ObjectMapper mapper = new ObjectMapper();
								try {
									JsonNode root = mapper.readTree(response);
									String responseCode = root.path("responseCode").asText();
									if (isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
										JsonNode name = root.path("result").path("data").path("name");
										return name.asText();
									}
									return "";
								} catch (JsonProcessingException jsonMappingException) {
									return "";
								}

							} else {
								return "";
							}
						})
						.doOnError(throwable -> log.info("Error in getting campaign: " + throwable.getMessage()))
						.onErrorReturn(""))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
	}

	public Mono<String> getCurrentAdapter(String botName) {
		String cacheKey = "adpater-of-bot: " + botName;
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache.getIfPresent(cacheKey) != null ? cache.getIfPresent(key).toString() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
						.uri(builder -> builder.path("admin/v1/bot/getByParam/").queryParam("name", botName).build()).retrieve()
						.bodyToMono(String.class).map(response -> {
							if (response != null) {
								ObjectMapper mapper = new ObjectMapper();
								try {
									JsonNode root = mapper.readTree(response);
									String responseCode = root.path("responseCode").asText();
									if (isApiResponseOk(responseCode)) {
										JsonNode name = root.path("result").path("data");
										if (name.has("name") && name.get("name").asText().equals(botName)) {
											return (((JsonNode) ((ArrayNode) name.path("logic"))).get(0).path("adapter"))
													.asText();
										}
									}
									return null;
								} catch (JsonProcessingException jsonMappingException) {
									return null;
								}
		
							} else {
							}
							return null;
						})
						.doOnError(throwable -> log.info("Error in getting adpater: " + throwable.getMessage()))
						.onErrorReturn(""))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
	}

	public Mono<String> getBotIDFromBotName(String botName) {
		log.info("B1 - Start");
		String cacheKey = "Bot-id-for-bot-name: " + botName;
		if(cache.getIfPresent(cacheKey) != null) {
			log.info("B2 - Cache present: "+cache.getIfPresent(cacheKey).toString());
		}
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache.getIfPresent(cacheKey) != null ? cache.getIfPresent(key).toString() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get().uri(new Function<UriBuilder, URI>() {
							@Override
							public URI apply(UriBuilder builder) {
								URI uri = builder.path("admin/v1/bot/getByParam/").queryParam("name", botName).build();
								return uri;
							}
						}).retrieve().bodyToMono(String.class).map(new Function<String, String>() {
							@Override
							public String apply(String response) {
								log.info("B3 - response: "+response);
								if (response != null) {
									ObjectMapper mapper = new ObjectMapper();
									try {
										JsonNode root = mapper.readTree(response);
										String responseCode = root.path("responseCode").asText();
										if (isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
											log.info("B4 - response ok & bot valid: "+response);
											JsonNode name = root.path("result").path("data");
											if (name.has("name") && name.get("name").asText().equals(botName)) {
												log.info("B5 - bot id: "+name.path("id").asText());
												return ((JsonNode) ((JsonNode) name.path("id"))).asText();
				
											}
										}
										return null;
									} catch (JsonProcessingException jsonMappingException) {
										log.info("B6 - Exception in json mapping: "+jsonMappingException.getMessage());										
										return null;
									}
				
								} else {
									log.info("B7 - response null");										
								}
								return null;
							}
						})
						.doOnError(throwable -> log.info("B8 - Error in getting bot: " + throwable.getMessage()))
						.onErrorReturn(""))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
	}
	
	/**
	 * Retrieve Campaign Params From its Identifier
	 *
	 * @param campaignID - Campaign Identifier
	 * @return Application
	 */
	public Mono<Map<String, String>> getGupshupAdpaterCredentials(String adapterID) {
		String cacheKey = "gupshup-credentials-for-adapter: " + adapterID;
		return CacheMono.lookup(key -> Mono.justOrEmpty((Map<String, String>) cache.getIfPresent(cacheKey))
				.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get().uri(builder -> builder.path("admin/v1/adapter/getCredentials/" + adapterID).build())
							.retrieve().bodyToMono(String.class).map(response -> {
								if (response != null) {
									ObjectMapper mapper = new ObjectMapper();
									try {
										Map<String, String> credentials = new HashMap<String, String>();
										JsonNode root = mapper.readTree(response);
										String responseCode = root.path("responseCode").asText();
										if (isApiResponseOk(responseCode)) {
											JsonNode result = root.path("result");
											credentials.put("username2Way", result.findValue("username2Way").asText());
											credentials.put("password2Way", result.findValue("password2Way").asText());
											credentials.put("usernameHSM", result.findValue("usernameHSM").asText());
											credentials.put("passwordHSM", result.findValue("passwordHSM").asText());
											System.out.println(credentials);
											return credentials;
										}
										return null;
									} catch (JsonProcessingException e) {
										return null;
									}
								}
								return null;
							}))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
					
	}

	public Application getButtonLinkedApp(String appName) {
		try {
			Application application = getCampaignFromName(appName);
			String buttonLinkedAppID = (String) ((ArrayList<Map>) application.data.get("parts")).get(0)
					.get("buttonLinkedApp");
			Application linkedApplication = this.getCampaignFromID(buttonLinkedAppID);
			return linkedApplication;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<Application> getApplications() {
		List<Application> applications = new ArrayList<>();
		ClientResponse<ApplicationResponse, Void> response = fusionAuthClient.retrieveApplications();
		if (response.wasSuccessful()) {
			applications = response.successResponse.applications;
		} else if (response.exception != null) {
			Exception exception = response.exception;
		}
		return applications;
	}

	/**
	 * Retrieve Campaign Params From its Identifier
	 *
	 * @param campaignID - Campaign Identifier
	 * @return Application
	 * @throws Exception Error Exception, in failure in Network request.
	 */
	public Application getCampaignFromID(String campaignID) throws Exception {
		ClientResponse<ApplicationResponse, Void> applicationResponse = fusionAuthClient
				.retrieveApplication(UUID.fromString(campaignID));
		if (applicationResponse.wasSuccessful()) {
			return applicationResponse.successResponse.application;
		} else if (applicationResponse.exception != null) {
			throw applicationResponse.exception;
		}
		return null;
	}

	/**
	 * Retrieve Campaign Params From its Name
	 *
	 * @param campaignName - Campaign Name
	 * @return Application
	 */
	private Application getCampaignFromName(String campaignName) {
		List<Application> applications = getApplications();

		Application currentApplication = null;
		if (applications.size() > 0) {
			for (Application application : applications) {
				if (application.name.equals(campaignName)) {
					currentApplication = application;
				}
			}
		}
		return currentApplication;
	}

	public Mono<Pair<Boolean, String>> updateUser(String userID, String botName) {
		log.info("U1 - Start");
		return getBotIDFromBotName(botName).doOnError(e -> log.error(e.getMessage()))
				.flatMap(new Function<String, Mono<Pair<Boolean, String>>>() {
					@Override
					public Mono<Pair<Boolean, String>> apply(String botID) {
						return webClient.get().uri(new Function<UriBuilder, URI>() {
							@Override
							public URI apply(UriBuilder builder) {
								String base = String.format("/admin/v1/userSegment/addUser/%s/%s", botID, userID);
								URI uri = builder.path(base).build();
								return uri;
							}
						}).retrieve().bodyToMono(String.class).map(response -> {
							log.info("U2 - AddUser response: "+response);
							if (response != null) {
								ObjectMapper mapper = new ObjectMapper();
								try {
									JsonNode root = mapper.readTree(response);
									String responseCode = root.path("responseCode").asText();
									if (isApiResponseOk(responseCode)) {
										log.info("U3 - Api response ok");
										Boolean status = root.path("result").path("status").asText()
												.equalsIgnoreCase("Success");
										String userID = root.path("result").path("userID").asText();
										log.info("U4 - Status: "+status+", userID: "+userID);
										return Pair.of(status, userID);
									}
									return Pair.of(false, "");
								} catch (JsonProcessingException jsonMappingException) {
									log.info("U4 - Exception in json mapping: "+jsonMappingException.getMessage());
									return Pair.of(false, "");
								}
							} else {
								log.info("U5 - AddUser response: null");
								return Pair.of(false, "");
							}
						}).doOnError(throwable -> log.info("U6 - Error in updating user: " + throwable.getMessage()))
								.onErrorReturn(Pair.of(false, ""));
					}
				});
	}

	/**
	 * Check if response code sent in api response is ok
	 * 
	 * @param responseCode
	 * @return Boolean
	 */
	private Boolean isApiResponseOk(String responseCode) {
		return responseCode.equals("OK");
	}
}
