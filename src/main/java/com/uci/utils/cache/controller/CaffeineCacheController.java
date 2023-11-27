package com.uci.utils.cache.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(value = "/cache/caffeine")
public class CaffeineCacheController {
    @Autowired
    private Cache<Object, Object> cache;
    @Value("${spring.caffeine.authorization.key:#{''}}")
    private String authorizationKey;

    /**
     * call this to invalidate all cache instances
     */
    @GetMapping(path = "/all", produces = {"application/json", "text/json"})
    public ResponseEntity getAll(@RequestHeader(name = "Authorization") String authorizationHeader) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            if (authorizationKey.equals(authorizationHeader)) {
                jsonNode = mapper.readTree("{\"id\":\"api.content.cache\",\"ver\":\"3.0\",\"ts\":\"2021-06-26T22:47:05Z+05:30\",\"responseCode\":\"OK\",\"result\":{}}");
                JsonNode resultNode = mapper.createObjectNode();
                cache.asMap().keySet().forEach(key -> {
                    String cacheName = key.toString();
                    ((ObjectNode) resultNode).put(cacheName, cache.getIfPresent(cacheName).toString());
                });
                ((ObjectNode) jsonNode).put("result", resultNode);
                return ResponseEntity.ok(resultNode);
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("message", "Unauthorized. Invalid secure key.");
                map.put("status", "failed");
                return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            log.error("CaffeineCacheController:getAll: Error while getting cache : " + ex.getMessage());
            Map<String, Object> map = new HashMap<>();
            map.put("message", ex.getMessage());
            map.put("status", "failed");
            return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * call this to invalidate all cache instances
     */
    @DeleteMapping(path = "/removeAll")
    public ResponseEntity<Object> removeAll(@RequestHeader(name = "Authorization") String authorizationHeader) {
        try {
            cache.asMap().keySet().forEach(key -> {
                removeCache(key.toString());
            });
            log.info("All cache removed success");
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Cache removed success");
            map.put("status", "success");
            return new ResponseEntity<>(map, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("CaffeineCacheController:removeAll: Error while removing cache : " + ex.getMessage());
            Map<String, Object> map = new HashMap<>();
            map.put("message", ex.getMessage());
            map.put("status", "failed");
            return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void removeCache(final String cacheName) {
        if (cache.getIfPresent(cacheName) != null) {
            cache.invalidate(cacheName);
        }
    }
}
