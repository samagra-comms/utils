package com.uci.utils;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableCaching
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
	
	@Bean
	public Caffeine<Object, Object> caffeineCacheBuilder() {
		return Caffeine.newBuilder()
				.maximumSize(cacheMaxSize)
				.expireAfterWrite(Duration.ofSeconds(cacheExpireDuration))
				.recordStats();
	}
	
	@Bean
    @Profile("!dev")
    public CacheManager getRealCacheManager(Caffeine caffeine) {
		log.info("Real cache enabled");
    	CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

    @Bean
    @Profile("dev")
    public CacheManager getNoOpCacheManager() {
    	log.info("No-op cache enabled");
    	NoOpCacheManager caffeineCacheManager = new NoOpCacheManager();
        return caffeineCacheManager;
    }

	@Bean
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl(CAMPAIGN_URL).defaultHeader("admin-token", CAMPAIGN_ADMIN_TOKEN).build();
	}

}
