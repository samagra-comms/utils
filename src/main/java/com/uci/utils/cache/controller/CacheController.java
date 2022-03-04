package com.uci.utils.cache.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(value = "/cache")
public class CacheController {
	@Autowired
	private CacheManager cacheManager;
	
	/**
	 * call this to invalidate all cache instances
	 */
	@GetMapping(path = "/all", produces = { "application/json", "text/json" })
	public ResponseEntity getAll() {
		ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree("{\"id\":\"api.content.cache\",\"ver\":\"3.0\",\"ts\":\"2021-06-26T22:47:05Z+05:30\",\"responseCode\":\"OK\",\"result\":{}}");
			JsonNode resultNode = mapper.createObjectNode();
	        Collection<String> cacheNames = cacheManager.getCacheNames();
	        for (String name : cacheNames) {
	           ((ObjectNode) resultNode).put(name, (cacheManager.getCache(name) != null 
	        		   	&& cacheManager.getCache(name).get(name) != null 
	        			? cacheManager.getCache(name).get(name).get().toString() : null));
	        }
	        ((ObjectNode) jsonNode).put("result", resultNode);
			return ResponseEntity.ok(jsonNode);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return ResponseEntity.ok(null);
	}

	/**
	 * call this to invalidate all cache instances
	 */
	@DeleteMapping(path = "/removeAll")
	public void removeAll() {
		Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String name : cacheNames) {
        	cacheManager.getCache(name).clear();
//			removeCache(key.toString());
		}
	}

//	private void removeCache(final String cacheName) {
//		if (cache.getIfPresent(cacheName) != null) {
//			cache.invalidate(cacheName);
//		}
//	}
}
