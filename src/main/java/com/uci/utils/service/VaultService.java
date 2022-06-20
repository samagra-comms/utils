package com.uci.utils.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.cache.service.RedisCacheService;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class VaultService {
    public String getLoginToken() {
        FusionAuthClient fusionAuthClient = new FusionAuthClient(System.getenv("VAULT_FUSION_AUTH_TOKEN"), System.getenv("VAULT_FUSION_AUTH_URL"));
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.loginId = "uci-user";
        loginRequest.password = "abcd1234";
        loginRequest.applicationId = UUID.fromString("a1313380-069d-4f4f-8dcb-0d0e717f6a6b");
        ClientResponse<LoginResponse, Errors> loginResponse = fusionAuthClient.login(loginRequest);
        if(loginResponse.wasSuccessful()) {
            return loginResponse.successResponse.token;
        } else {
            return null;
        }
    }

    /**
     * Retrieve Adapter Credentials From its Identifier
     *
     * @param adapterID - Adapter Identifier
     * @return Application
     */
    public Mono<JsonNode> getAdpaterCredentials(String adapterID) {
        String userToken = getLoginToken();
        if(userToken == null || userToken.isEmpty()) {
            return Mono.just(null);
        }
        WebClient webClient = WebClient.builder().baseUrl(System.getenv("VAULT_SERVICE_URL")).build();
        return webClient.get()
                .uri(builder -> builder.path("admin/secret/" + adapterID).build())
                .headers(httpHeaders ->{
                    httpHeaders.set("ownerId", "8f7ee860-0163-4229-9d2a-01cef53145ba");
                    httpHeaders.set("ownerOrgId", "org1");
                    httpHeaders.set("Authorization", "Bearer "+userToken);
                })
                .retrieve().bodyToMono(String.class).map(response -> {
                    if (response != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            Map<String, String> credentials = new HashMap<String, String>();
                            JsonNode root = mapper.readTree(response);
                            if(root.path("result") != null && root.path("result").path(adapterID) != null) {
                                return root.path("result").path(adapterID);
                            }
                            return null;
                        } catch (JsonProcessingException e) {
                            return null;
                        }
                    }
                    return null;
                });

    }
}
