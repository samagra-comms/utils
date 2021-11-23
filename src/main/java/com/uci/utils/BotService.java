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
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
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

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param startingMessage - Starting Message
     * @return Application
     */
    public Mono<String> getCampaignFromStartingMessage(String startingMessage) {
        return webClient.get()
                .uri(builder -> builder.path("admin/v1/bot/getByParam/").queryParam("startingMessage", startingMessage).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    if (response != null) {
                    	log.info(response);
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            JsonNode root = mapper.readTree(response);
                            String responseCode = root.path("responseCode").asText();
                            if(CommonUtil.isWebClientApiResponseOk(responseCode) && BotUtil.checkBotValidFromJsonNode(root)) {
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
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .doOnError(throwable -> log.error("Error in getting campaign: " + throwable.getMessage()))
                .onErrorReturn("");
    }

    public Mono<String> getCurrentAdapter(String botName) {
        return webClient.get()
        		.uri(builder -> builder.path("admin/v1/bot/getByParam/").queryParam("name", botName).build())
        		.retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                	if (response != null) {                    	
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            JsonNode root = mapper.readTree(response);
                            String responseCode = root.path("responseCode").asText();
                            if(CommonUtil.isWebClientApiResponseOk(responseCode)) {
                            	JsonNode name = root.path("result").path("data");
                                if (name.has("name") && name.get("name").asText().equals(botName)) {
                                    return (((JsonNode) ((ArrayNode) name.path("logic"))).get(0).path("adapter")).asText();
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
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .doOnError(throwable -> log.error("Error in getting adpater: " + throwable.getMessage()))
                .onErrorReturn("");
    }

    public Mono<String> getBotIDFromBotName(String botName) {
        return webClient.get()
                .uri(new Function<UriBuilder, URI>() {
                    @Override
                    public URI apply(UriBuilder builder) {
                        URI uri = builder.path("admin/v1/bot/getByParam/").queryParam("name", botName).build();
                        return uri;
                    }
                })
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
                                	JsonNode name = root.path("result").path("data");
	                                if (name.has("name") && name.get("name").asText().equals(botName)) {
	                                    return ((JsonNode) ((JsonNode) name.path("id"))).asText();
	
	                                }
                                }
                                return null;
                            } catch (JsonProcessingException jsonMappingException) {
                                return null;
                            }

                        } else {
                        }
                        return null;
                    }
                })
                .timeout(CommonUtil.getWebClientTimeoutDuration())
                .doOnError(throwable -> log.error("Error in getting bot id from bot name: " + throwable.getMessage()))
                .onErrorReturn("");
    }
    

    
    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     */
    public Mono<Map<String, String>> getGupshupAdpaterCredentials(String adapterID) {
        return webClient.get()
                .uri(builder -> builder.path("admin/v1/adapter/getCredentials/" + adapterID).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                            if (response != null) {
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                	Map<String, String> credentials = new HashMap<String, String>();
                                	JsonNode root = mapper.readTree(response);
                                    String responseCode = root.path("responseCode").asText();
                                    if(isApiResponseOk(responseCode)) {
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
                        }
                );
    }
    

    public Application getButtonLinkedApp(String appName) {
        try {
            Application application = getCampaignFromName(appName);
            String buttonLinkedAppID = (String) ((ArrayList<Map>) application.data.get("parts")).get(0).get("buttonLinkedApp");
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
        ClientResponse<ApplicationResponse, Void> applicationResponse = fusionAuthClient.retrieveApplication(UUID.fromString(campaignID));
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
        return getBotIDFromBotName(botName)
                .doOnError(e -> log.error(e.getMessage()))
                .flatMap(new Function<String, Mono<Pair<Boolean, String>>>() {
                    @Override
                    public Mono<Pair<Boolean, String>> apply(String botID) {
                        return webClient
                                .get()
                                .uri(new Function<UriBuilder, URI>() {
                                    @Override
                                    public URI apply(UriBuilder builder) {
                                        String base = String.format("/admin/v1/userSegment/addUser/%s/%s", botID, userID);
                                        URI uri = builder.path(base).build();
                                        return uri;
                                    }
                                })
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(response -> {
                                	System.out.println(response);
                                    if (response != null) {
                                        ObjectMapper mapper = new ObjectMapper();
                                        try {
                                            JsonNode root = mapper.readTree(response);
                                            String responseCode = root.path("responseCode").asText();
                                            if(CommonUtil.isWebClientApiResponseOk(responseCode)) {
	                                            Boolean status = root.path("result").path("status").asText().equalsIgnoreCase("Success");
	                                            String userID = root.path("result").path("userID").asText();
	                                            return Pair.of(status, userID);
                                            }
                                            return Pair.of(false, "");
                                        } catch (JsonProcessingException jsonMappingException) {
                                            return Pair.of(false, "");
                                        }
                                    } else {
                                        return Pair.of(false, "");
                                    }
                                })
                                .timeout(CommonUtil.getWebClientTimeoutDuration())
                                .doOnError(throwable -> log.error("Error in updating user: " + throwable.getMessage()))
                                .onErrorReturn(Pair.of(false, ""));
                    }
                });
    }
}
