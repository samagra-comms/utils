package com.uci.utils;

import com.uci.utils.kafka.KafkaConfig;
import com.uci.utils.service.UserService;
import io.fusionauth.client.FusionAuthClient;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

public class UtilsTestConfig {

    @Bean
    public UserService getUserService(){
        return new UserService();
    }

    @Bean
    public UtilHealthService getUtilHealthService(){
        return new UtilHealthService();
    }

    @Bean
    public KafkaConfig getKafkaConfig(){
        return new KafkaConfig();
    }

    @Bean
    public BotService getBotService(){
        return new BotService(webClient, fusionAuthClient, null);
    }


    @Mock
    WebClient webClient;

    @MockBean
    FusionAuthClient fusionAuthClient;


    @Autowired
    UtilHealthService utilHealthService;



}
