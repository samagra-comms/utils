package com.uci.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.bot.util.BotUtil;
import com.uci.utils.common.CommonUtil;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.api.ApplicationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
@Service
@Slf4j
@AllArgsConstructor
public class CampaignService {

    public WebClient webClient;
    public FusionAuthClient fusionAuthClient;

    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     */
    public Mono<JsonNode> getCampaignFromID(String campaignID) {
        return webClient.get()
                .uri(builder -> builder.path("admin/v1/bot/get/" + campaignID).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                            if (response != null) {
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                	JsonNode root = mapper.readTree(response);
                                    String responseCode = root.path("responseCode").asText();
                                    if(CommonUtil.isWebClientApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
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
                )
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .doOnError(throwable -> log.error("Error in getting campaign: " + throwable.getMessage()))
                .onErrorReturn(null);
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
        return webClient.get()
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
                                     if(CommonUtil.isWebClientApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
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
                )
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .doOnError(throwable -> {
                    log.error("Error in fetching Campaign Information from Name when invoked by transformer >>> " + throwable.getMessage());
                }).onErrorReturn(null);


    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param botID - Bot ID
     * @return FormID for the first transformer.
     */
    public Mono<String> getFirstFormByBotID(String botID) {
        return webClient.get()
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
                                     if(CommonUtil.isWebClientApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
                                    	 return root.path("result").findValue("formID").asText();
                                     }
                                     return null;
//                                     return mapper.readTree(response).findValue("formID").asText();
                                 } catch (JsonProcessingException e) {
                                     return null;
                                 }
                             }
                             return null;
                         }
                     }
                )
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .onErrorReturn(null).doOnError(throwable -> log.error("Error in getting first form by bot id >>> " + throwable.getMessage()));
    }

    public Mono<String> getBotNameByBotID(String botID) {
        return webClient.get()
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
                                     if(CommonUtil.isWebClientApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
                                    	 return root.path("result").get("data").get("name").asText();
                                     }
                                     return null;
//                                     return mapper.readTree(response).get("data").get("name").asText();
                                 } catch (JsonProcessingException e) {
                                     return null;
                                 }
                             }
                             return null;
                         }
                     }
                )
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .onErrorReturn(null).doOnError(throwable -> log.error("Error in getting bot name from bot id >>> " + throwable.getMessage()));
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
}

