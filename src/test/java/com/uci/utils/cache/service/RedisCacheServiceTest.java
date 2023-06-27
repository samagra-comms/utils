package com.uci.utils.cache.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

class RedisCacheServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;
    RedisCacheService redisCacheService;
    final Object obj = "object";
    final String key = "someKay";
    final Integer redisKeyTimeout = 3600;

    @BeforeEach
    void init(){
        RedisTemplate<String, Object> obj = new RedisTemplate<>();
        redisCacheService = Mockito.spy(new RedisCacheService(obj,redisKeyTimeout));
        Mockito.doReturn(obj).when(redisCacheService).getCache(anyString());
        Mockito.doNothing().when(redisCacheService).setCache(anyString(), Mockito.any());
        Mockito.doNothing().when(redisCacheService).deleteCache(anyString());
    }

    @Test
    void getMinioCDNCache() {
        Object result = redisCacheService.getMinioCDNCache(key);
        assertEquals(RedisTemplate.class, result.getClass());
    }

    @Test
    void setMinioCDNCache() {
        redisCacheService.setMinioCDNCache(key, obj);
    }

    @Test
    void getFAUserIDForAppCache() {
        Object result = redisCacheService.getFAUserIDForAppCache(key);
        assertEquals(RedisTemplate.class, result.getClass());
    }

    @Test
    void setFAUserIDForAppCache() {
        redisCacheService.setFAUserIDForAppCache(key, obj);
    }

    @Test
    void deleteFAUserIDForAppCache() {
        redisCacheService.deleteCache(key);
    }

    @Test
    void getXMessageDaoCache() {
        Object result = redisCacheService.getXMessageDaoCache(key);
        System.out.println(result);
        assertEquals(RedisTemplate.class, result.getClass());
    }

    @Test
    void setXMessageDaoCache() {
        redisCacheService.setXMessageDaoCache(key, obj);
    }

//    @Test
//    void deleteCache() {
//
//    }
//
//    @Test
//    void setCache(){
//    }
//
//    @Test
//    void getCache(){
//    }
}