package com.uci.utils.cdn.samagra;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.cache.service.RedisCacheService;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.credentials.StaticProvider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SuppressWarnings("ALL")
@Service
@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class MinioClientService {
	private MinioClientProp minioClientProp;
	private RedisCacheService redisCacheService;
	
	/**
	 * Get Signed URL of CDN for given media file name
	 * @param mediaName
	 * @return
	 */
	public String getCdnSignedUrl(String mediaName) {
		String url = "";
		try {
			if(minioClientProp != null) {
				MinioClient minioClient = getMinioClient();
				log.info("minioClient: "+minioClient);
		        if(minioClient != null) {
					try {
						url = minioClient.getPresignedObjectUrl(
			                GetPresignedObjectUrlArgs.builder()
			                    .method(Method.GET)
			                    .bucket(minioClientProp.bucketId)
			                    .object(mediaName)
			                    .expiry(1, TimeUnit.DAYS)
			                    .build());
					} catch (InvalidKeyException | InsufficientDataException | InternalException
							| InvalidResponseException | NoSuchAlgorithmException | XmlParserException | ServerException
							| IllegalArgumentException | IOException e) {
						// TODO Auto-generated catch block
						log.error("Exception in getCdnSignedUrl: "+e.getMessage());
					} catch (ErrorResponseException e1) {
						log.error("Exception in getCdnSignedUrl: "+e1.getMessage()+", name: "+e1.getClass());
					}
		        }
		        log.info("minioClient url: "+url);
			}
		} catch (Exception ex) {
			log.error("Exception in getCdnSignedUrl: "+ex.getMessage());
		}
		
        return url;
    }
	
	/**
	 * Get Minio Client
	 * @return
	 */
	private MinioClient getMinioClient() {
		try {
			StaticProvider provider = getMinioCredentialsProvider();
			log.info("provider: "+provider+", url: "+minioClientProp.cdnBaseUrl);
			if(provider != null) {
				return MinioClient.builder()
						.endpoint(minioClientProp.cdnBaseUrl)
						.credentialsProvider(provider)
						.build();
			}
		} catch(Exception e) {
			log.error("Exception in getMinioClient: "+e.getMessage());
		}
		return null;
	}
	
	/**
	 * Get Credentials Provider for Minio Client
	 * @return
	 */
	private StaticProvider getMinioCredentialsProvider() {
		try {
			String token = getFusionAuthToken();
			log.info("token: "+token);
			if(!token.isEmpty()) {
				WebClient client = WebClient.builder()
						.baseUrl(minioClientProp.cdnBaseUrl)
		                .build();
//				Integer duration = (minioClientProp.credentialsExpiry)*60*60;
				Integer duration = 36000;
				String response = client.post().uri(builder -> builder.path(minioClientProp.bucketId)
											.queryParam("Action", "AssumeRoleWithWebIdentity")
											.queryParam("DurationSeconds", 36000) //duration: 10 Hours
											.queryParam("WebIdentityToken", token)
											.queryParam("Version", "2011-06-15")
											.build())
					.exchange()
                    .block()
                    .bodyToMono(String.class)
                    .block();
				
				JSONObject xmlJSONObj = XML.toJSONObject(response);
		        String jsonPrettyPrintString = xmlJSONObj.toString(4);
		        
		        ObjectMapper mapper = new ObjectMapper();
		        JsonNode node = mapper.readTree(jsonPrettyPrintString);
		        JsonNode credentials = node.path("AssumeRoleWithWebIdentityResponse").path("AssumeRoleWithWebIdentityResult").path("Credentials");
		        if(credentials != null && credentials.get("SessionToken") != null 
		        		&& credentials.get("AccessKeyId") != null && credentials.get("SecretAccessKey") != null) {
		        	String sessionToken = credentials.get("SessionToken").asText();
					String accessKey = credentials.get("AccessKeyId").asText();
					String secretAccessKey = credentials.get("SecretAccessKey").asText();
					
					log.info("sessionToken: "+sessionToken+", accessKey: "+accessKey+",secretAccessKey: "+secretAccessKey);
					
					if(!accessKey.isEmpty() && !secretAccessKey.isEmpty() && !sessionToken.isEmpty()) {
						return new StaticProvider(accessKey, secretAccessKey, sessionToken);
//						return new StaticProvider("test", secretAccessKey, sessionToken);
					}
		        } else {
		        	if(node.path("ErrorResponse") != null 
		        			&& node.path("ErrorResponse").path("Error") != null 
		        			&& node.path("ErrorResponse").path("Error").path("Message") != null) {
		        		log.error("Error when getting credentials for minio client: "+node.path("ErrorResponse").path("Error").path("Message").asText());
		        	}
		        }
			}  
		} catch (Exception e) {
			log.error("Exception in getMinioCredentialsProvider: "+e.getMessage());
		}
		return null;
	}
	
	/**
	 * Set Minio Credentials Cache
	 * @param sessionToken
	 * @param accessKey
	 * @param secretAccessKey
	 */
	private void setMinioCredentialsCache(String sessionToken, String accessKey, String secretAccessKey) {
//		Integer duration = (minioClientProp.credentialsExpiry-1)*60*60;
		Integer duration = 23*60*60;
		
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    	
		/* local date time */
    	LocalDateTime localNow = LocalDateTime.now();
    	LocalDateTime expiry = localNow.plusSeconds(duration);
    	String expiryDateString = fmt.format(expiry).toString();
    	
		redisCacheService.setMinioCDNCache("sessionToken", sessionToken);
		redisCacheService.setMinioCDNCache("accessKey", accessKey);
		redisCacheService.setMinioCDNCache("secretAccessKey", secretAccessKey);
		redisCacheService.setMinioCDNCache("expiresAt", expiryDateString);
	}
	
	/**
	 * Get Minio Credentials from Redis Cache
	 * @param sessionToken
	 * @param accessKey
	 * @param secretAccessKey
	 * @return
	 */
	private Map<String, String> getMinioCredentialsCache(String sessionToken, String accessKey, String secretAccessKey) {
		Map<String, String> credentials = new HashMap();
		
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    	
		/* local date time */
    	LocalDateTime localNow = LocalDateTime.now();
    	String dateString = fmt.format(localNow).toString();
    	LocalDateTime localDateTime = LocalDateTime.parse(dateString, fmt);
    	
		String expiry = (String) redisCacheService.getMinioCDNCache("expiresAt");
		
		LocalDateTime expiryDateTime = LocalDateTime.parse(dateString, fmt);
		
		if(localDateTime.compareTo(expiryDateTime) < 0) {
			credentials.put("sessionToken", (String) redisCacheService.getMinioCDNCache("sessionToken"));
			credentials.put("accessKey", (String) redisCacheService.getMinioCDNCache("accessKey"));
			credentials.put("secretAccessKey", (String) redisCacheService.getMinioCDNCache("secretAccessKey"));
		}
		return credentials;
	}
	
//	private StaticProvider getMinioClientProvider1() {
//		
//	}
	
	/**
	 * Get Fustion Auth Token
	 * @return
	 */
	private String getFusionAuthToken() {
		String token = "";		
		try {
			ClientResponse<LoginResponse, Errors> clientResponse = minioClientProp.fusionAuth.login(minioClientProp.loginRequest);
			if(clientResponse.wasSuccessful()) {
				token = clientResponse.successResponse.token;
			}
		} catch (Exception e) {
			log.error("Exception in getFusionAuthToken: "+e.getMessage());
		}
		return token;
	}
}
