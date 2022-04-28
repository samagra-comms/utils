package com.uci.utils.cache.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.uci.utils.cache.service.RedisCacheService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(value = "/cache/redis")
public class RedisCacheController {
	@Autowired
	private RedisCacheService service;

	/**
	 * call this remove cache by key
	 * @param cacheName
	 */
	@DeleteMapping(path = "/remove")
	private void removeCacheByKey(@RequestParam String prefix, @RequestParam String key) {
		service.deleteCache(prefix, key);
	}
}
