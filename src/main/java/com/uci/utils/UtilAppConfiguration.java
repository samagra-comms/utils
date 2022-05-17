package com.uci.utils;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

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

	@Value("${opentelemetry.lightstep.service}")
	private String lightstepService;
	
	@Value("${opentelemetry.lightstep.access.token}")
	private String lightstepAccessToken;
	
	@Value("${opentelemetry.lightstep.end.point}")
	private String lightstepEndPoint;
	
	@Value("${opentelemetry.lightstep.tracer}")
	private String lightstepTracer;
	
	@Value("${opentelemetry.lightstep.tracer.version}")
	private String lightstepTracerVersion;
	
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
	
	@Bean
    public Tracer OpenTelemetryTracer() {
        OpenTelemetryConfiguration.newBuilder()
                .setServiceName(lightstepService)
    	        .setAccessToken(lightstepAccessToken)
    	        .setTracesEndpoint(lightstepEndPoint)
                .install();

        Tracer tracer = GlobalOpenTelemetry
                .getTracer(lightstepTracer, lightstepTracerVersion);
        return tracer;
    }
}
