package com.uci.utils.azure;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AzureBlobProperties {
//	public String url;
//	public String token;
	
	public String container;
	public String accountKey;
	public String accountName;
}