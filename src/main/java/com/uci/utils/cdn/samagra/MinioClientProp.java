package com.uci.utils.cdn.samagra;

import org.springframework.lang.Nullable;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MinioClientProp {
	LoginRequest loginRequest;
	String cdnBaseUrl;
	String bucketId;
	FusionAuthClient fusionAuth;
}
