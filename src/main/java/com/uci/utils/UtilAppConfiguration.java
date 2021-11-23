package com.uci.utils;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;


@Configuration
@EnableAutoConfiguration
public class UtilAppConfiguration {


    @Value("${campaign.url}")
    public String CAMPAIGN_URL;
    
	@Value("${campaign.admin.token}")
	public String CAMPAIGN_ADMIN_TOKEN;

    @Bean
    public WebClient getWebClient() {
//    	HttpClient httpClient = HttpClient.create()
//    			  .responseTimeout(Duration.ofSeconds(1)); 
        return WebClient.builder()
//        		.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(CAMPAIGN_URL)
                .defaultHeader("admin-token", CAMPAIGN_ADMIN_TOKEN)
                .build();
    }


}
