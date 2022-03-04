package com.uci.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.bot.util.BotUtil;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.api.ApplicationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
@Service
@Slf4j
@AllArgsConstructor
public class CampaignService {

    public WebClient webClient;
    public FusionAuthClient fusionAuthClient;
	private CacheManager cacheManager;
    
//    private static final Cache<Object, Object> cache = Caffeine.newBuilder().maximumSize(1000)
//			.expireAfterWrite(Duration.ofSeconds(300))
//			.removalListener((key, value, cause) -> log.info("Remove cache({}) ... {}", key, cause)).build();

    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     */
    public Mono<JsonNode> getCampaignFromID(String campaignID) {
    	String cacheKey = "campaign-node-by-id:" + campaignID;
    	Cache cache = cacheManager.getCache(cacheKey);
		
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache != null && cache.get(cacheKey) != null ? (JsonNode)cache.get(cacheKey).get() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
		                .uri(builder -> builder.path("admin/v1/bot/get/" + campaignID).build())
		                .retrieve()
		                .bodyToMono(String.class)
		                .map(response -> {
		                            if (response != null) {
		                                ObjectMapper mapper = new ObjectMapper();
		                                try {
		                                	JsonNode root = mapper.readTree(response);
		                                    String responseCode = root.path("responseCode").asText();
		                                    if(isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
		                                   	 return root.path("result");
		                                    }
		                                    return null;
		//                                    return mapper.readTree(response);
		                                } catch (JsonProcessingException e) {
		                                    return null;
		                                }
		                            }
		                            return null;
		                        }
		                ))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public Application getCampaignFromName(String campaignName) throws Exception {
        List<Application> applications = new ArrayList<>();
        ClientResponse<ApplicationResponse, Void> response = fusionAuthClient.retrieveApplications();
        if (response.wasSuccessful()) {
            applications = response.successResponse.applications;
        } else if (response.exception != null) {
            Exception exception = response.exception;
        }


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


    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     */
    public Mono<JsonNode> getCampaignFromNameTransformer(String campaignName) {
    	String cacheKey = "campaign-node-by-name:" + campaignName;
    	Cache cache = cacheManager.getCache(cacheKey);
    	
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache != null && cache.get(cacheKey) != null ? (JsonNode)cache.get(cacheKey).get() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
		                .uri(builder -> builder.path("admin/v1/bot/search/").queryParam("name", campaignName).queryParam("match", true).build())
		                .retrieve()
		                .bodyToMono(String.class)
		                .map(new Function<String, JsonNode>() {
		                         @Override
		                         public JsonNode apply(String response) {
		                             if (response != null) {
		                                 ObjectMapper mapper = new ObjectMapper();
		                                 try {
		                                     JsonNode root = mapper.readTree(response);
		                                     String responseCode = root.path("responseCode").asText();
		                                     if(isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
		                                         return root.path("result").path("data").get(0);
		                                     }else{
		                                         log.error("API response not okay");
		                                         return null;
		                                     }
		                                 } catch (JsonProcessingException e) {
		                                     log.error("JSON Parsing error" + e.getMessage());
		                                     return null;
		                                 }
		                             }
		                             log.error("API response was null");
		                             return null;
		                         }
		                     }
		                ).doOnError(throwable -> {
		                    log.error("Error in fetching Campaign Information from Name when invoked by transformer >>> " + throwable.getMessage());
		                }).onErrorReturn(null))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param botID - Bot ID
     * @return FormID for the first transformer.
     */
    public Mono<String> getFirstFormByBotID(String botID) {
    	String cacheKey = "form-by-bot-name:" + botID;
    	Cache cache = cacheManager.getCache(cacheKey);
    	
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache != null && cache.get(cacheKey) != null ? cache.get(cacheKey).get().toString() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
		                .uri(builder -> builder.path("admin/v1/bot/get/" + botID).build())
		                .retrieve()
		                .bodyToMono(String.class)
		                .map(new Function<String, String>() {
		                         @Override
		                         public String apply(String response) {
		                             if (response != null) {
		                                 ObjectMapper mapper = new ObjectMapper();
		                                 try {
		                                	 JsonNode root = mapper.readTree(response);
		                                     String responseCode = root.path("responseCode").asText();
		                                     if(isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
		                                    	 return root.path("result").findValue("formID").asText();
		                                     }
		                                     return null;
		                                 } catch (JsonProcessingException e) {
		                                     return null;
		                                 }
		                             }
		                             return null;
		                         }
		                     }
		                )
		                .onErrorReturn(null)
		                .doOnError(throwable -> log.error("Error in getFirstFormByBotID >>> " + throwable.getMessage())))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
		                
    }

    public Mono<String> getBotNameByBotID(String botID) {
    	String cacheKey = "bot-name-by-id:" + botID;
    	Cache cache = cacheManager.getCache(cacheKey);
    	
		return CacheMono.lookup(key -> Mono.justOrEmpty(cache != null && cache.get(cacheKey) != null ? cache.get(cacheKey).get().toString() : null)
					.map(Signal::next), cacheKey)
				.onCacheMissResume(() -> webClient.get()
		                .uri(builder -> builder.path("admin/v1/bot/get/" + botID).build())
		                .retrieve()
		                .bodyToMono(String.class)
		                .map(new Function<String, String>() {
		                         @Override
		                         public String apply(String response) {
		                             if (response != null) {
		                                 ObjectMapper mapper = new ObjectMapper();
		                                 try {
		                                	 JsonNode root = mapper.readTree(response);
		                                     String responseCode = root.path("responseCode").asText();
		                                     if(isApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
		                                    	 return root.path("result").get("data").get("name").asText();
		                                     }
		                                     return null;
		                                 } catch (JsonProcessingException e) {
		                                     return null;
		                                 }
		                             }
		                             return null;
		                         }
		                     }
		                )
		                .onErrorReturn(null)
		                .doOnError(throwable -> log.error("Error in getFirstFormByBotID >>> " + throwable.getMessage())))
				.andWriteWith((key, signal) -> Mono.fromRunnable(
						() -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
				.log("cache");
    }


    /**
     * Retrieve Campaign Params From its Name
     *
     * @param botID - Bot ID
     * @return FormID for the first transformer.
     * @throws Exception Error Exception, in failure in Network request.
     */
    public Mono<ArrayNode> getFirstFormHiddenFields(String botID) {
//        return webClient.get()
//                .uri(builder -> builder.path("admin/v1/bot/get/"+botID).build())
//                .retrieve()
//                .bodyToMono(String.class)
//                .map(response -> {
//                            if (response != null) {
//                                ObjectMapper mapper = new ObjectMapper();
//                                try {
//                                    return Mono.just(mapper.readTree(response).findValue("hiddenFields"));
//                                } catch (JsonProcessingException e) {
//                                    return null;
//                                }
//                            }
//                            return null;
//                        }
//                );
        return null;

    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public Application getCampaignFromNameESamwad(String campaignName) {
        List<Application> applications = new ArrayList<>();
        ClientResponse<ApplicationResponse, Void> response = fusionAuthClient.retrieveApplications();
        if (response.wasSuccessful()) {
            applications = response.successResponse.applications;
        } else if (response.exception != null) {
            Exception exception = response.exception;
        }

        Application currentApplication = null;
        if (applications.size() > 0) {
            for (Application application : applications) {
                try {
                    if (application.data.get("appName").equals(campaignName)) {
                        currentApplication = application;
                    }
                } catch (Exception e) {

                }
            }
        }
        return currentApplication;
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

