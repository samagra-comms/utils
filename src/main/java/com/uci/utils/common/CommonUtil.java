package com.uci.utils.common;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtil {


    /**
     * Check if response code sent in api response is ok
     *
     * @param responseCode
     * @return Boolean
     */
    public static Boolean isWebClientApiResponseOk(String responseCode) {
        return responseCode.equals("OK");
    } 
	
    /**
     * Get Timeout Duration for Web Client
     * @return Duration
     */
    public static Duration getWebClientTimeoutDuration() {
    	return Duration.ofSeconds(getWebClientTimeoutSeconds());
    }
    
    /**
     * Get Timeout in seconds from env variables, default value 5
     * @return Long
     */
    public static Long getWebClientTimeoutSeconds() {
    	Long timeout = null;
    	try {
    		timeout = Long.parseLong(System.getenv("WEBCLIENT_HTTP_REQUEST_TIMEOUT"));
    	} catch (Exception e) {
    		log.error("Exception in conversion of webclient http request timeout.");
    	}
    	
    	return timeout != null ? timeout : 5;
    }  
}
