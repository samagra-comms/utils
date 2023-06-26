package com.uci.utils.dto;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@Getter
public class BotServiceParams {
    @Value("${webclient.interval}")
    private long webclientInterval;
    @Value("${webclient.retryMaxAttempts}")
    private long webclientRetryMaxAttempts;
    @Value("${webclient.retryMinBackoff}")
    private long getWebclientMinBackoff;
}
