package com.uci.utils;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;

@Configuration
@EnableAutoConfiguration
public class UtilAppConfiguration {

	@Value("${campaign.url}")
	public String CAMPAIGN_URL;

	@Value("${campaign.admin.token}")
	public String CAMPAIGN_ADMIN_TOKEN;

	@Value("${caffeine.cache.max.size}")
	public Integer cacheMaxSize;
	
	@Value("${caffeine.cache.exprie.duration.seconds}")
	public Integer cacheExpireDuration;
	
	@Value("${spring.redis.db}")
	private String redisDb;
	
	@Value("${spring.data.redis.host}")
	private String redisHost;
	
	@Value("${spring.redis.number.port}")
	private String redisPort;
	
	public Caffeine<Object, Object> caffeineCacheBuilder() {
		return Caffeine.newBuilder()
				.maximumSize(cacheMaxSize)
				.expireAfterWrite(Duration.ofSeconds(cacheExpireDuration))
				.recordStats();
	}

	@Bean
	public Cache<Object, Object> cache() {
		return caffeineCacheBuilder().build();
	}

	@Bean
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl(CAMPAIGN_URL).defaultHeader("admin-token", CAMPAIGN_ADMIN_TOKEN).build();
	}
	
	@SuppressWarnings("ALL")
	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		JedisConnectionFactory jedisConFactory
	      = new JedisConnectionFactory();
	    System.out.println("redisPort:"+redisPort+",redisDb: "+redisDb+", redisHost: "+redisHost);
	    jedisConFactory.setHostName(redisHost);
	    Integer port = Integer.parseInt(redisPort);
	    jedisConFactory.setPort(port);
	    Integer dbIndex = Integer.parseInt(redisDb);
	    jedisConFactory.setDatabase(dbIndex);
	    return jedisConFactory;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
	    RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(jedisConnectionFactory());
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
	    return template;
	}
}
