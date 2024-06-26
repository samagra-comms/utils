package com.uci.utils.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.BotService;
import com.uci.utils.model.FAUser;
import com.uci.utils.model.FAUserSegment;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import io.fusionauth.domain.api.*;
import io.fusionauth.domain.api.user.RegistrationRequest;
import io.fusionauth.domain.api.user.RegistrationResponse;
import io.fusionauth.domain.api.user.SearchRequest;
import io.fusionauth.domain.api.user.SearchResponse;
import io.fusionauth.domain.search.UserSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Service
public class UserService {

	@Autowired
	private BotService botService;

	@Value("${campaign.url}")
    public String CAMPAIGN_URL;

    @Value("${campaign.admin.token}")
	public String CAMPAIGN_ADMIN_TOKEN;

	@Value("${template.service.base.url:#{''}}")
	private String baseUrlTemplate;

	@Value("${fusionauth.url}")
	private String fusionAuthUrl;

	@Autowired
	private FusionAuthClient fusionAuthClient;

	@Autowired
	Cache<Object, Object> cache;

	private static String shortnrBaseURL = "http://localhost:9999";

	/**
	 * Create application if not exists
	 * @param applicationID
	 * @param applicationName
	 * @return
	 */
	public Boolean createApplicationIfNotExists(UUID applicationID, String applicationName) {
		if(findApplicationByID(applicationID)) {
			return true;
		} else {
			return createApplication(applicationID, applicationName);
		}
	}

	/**
	 * Find application by id
	 * @param applicationID
	 * @return
	 */
	public Boolean findApplicationByID(UUID applicationID) {
		ClientResponse<ApplicationResponse, Void> response = fusionAuthClient.retrieveApplication(applicationID);
		if(response.wasSuccessful()) {
			return true;
		}
		return false;
	}

	/**
	 * Create application with id & name
	 * @param applicationID
	 * @param applicationName
	 * @return
	 */
	public Boolean createApplication(UUID applicationID, String applicationName) {
		Application application = new Application()
				.with(app -> app.id = applicationID)
				.with(app -> app.name = applicationName);

		ClientResponse<ApplicationResponse, Errors> response2 = fusionAuthClient.createApplication(applicationID, new ApplicationRequest(application, null));
		if(response2.wasSuccessful()) {
			log.info("succes:" +response2.successResponse);
			return true;
		} else {
			log.info("Error response: "+response2.errorResponse);
		}
		return false;
	}

	/**
	 * Find Fusion Auth User by Username
	 * @param username
	 * @return
	 */
	public User findFAUserByUsername(String username) {
		ClientResponse<UserResponse, Errors> response = fusionAuthClient.retrieveUserByUsername(username);
		if (response.wasSuccessful()) {
			return response.successResponse.user;
		} else if (response.errorResponse != null) {
			Errors errors = response.errorResponse;
			log.error("Errors in findFAUserByUsername: " + errors.toString());
		} else if (response.exception != null) {
			// Exception Handling
			Exception exception = response.exception;
			log.error("Exception in findFAUserByUsername: " + exception.toString());
		}
		return null;
	}

	public User findByEmail(String email) {
		ClientResponse<UserResponse, Errors> response = fusionAuthClient.retrieveUserByEmail(email);
		if (response.wasSuccessful()) {
			return response.successResponse.user;
		} else if (response.errorResponse != null) {
			Errors errors = response.errorResponse;
		} else if (response.exception != null) {
			// Exception Handling
			Exception exception = response.exception;
		}

		return null;
	}

	public List<User> findUsersForCampaign(String campaignName) throws Exception {

		// Fixme: Important
		/*
		 * Application currentApplication =
		 * botService.getCampaignFromName(campaignName); FusionAuthClient
		 * staticClient = getFusionAuthClient(); if(currentApplication != null){
		 * UserSearchCriteria usc = new UserSearchCriteria(); usc.numberOfResults =
		 * 10000; usc.queryString = "(memberships.groupId: " +
		 * currentApplication.data.get("group") + ")"; SearchRequest sr = new
		 * SearchRequest(usc); ClientResponse<SearchResponse, Errors> cr =
		 * staticClient.searchUsersByQueryString(sr);
		 *
		 * if (cr.wasSuccessful()) { return cr.successResponse.users; } else if
		 * (cr.exception != null) { // Exception Handling Exception exception =
		 * cr.exception; log.error("Exception in getting users for campaign: " +
		 * exception.toString()); } }
		 */
		return new ArrayList<>();
	}

	public List<String> findUsersForESamwad(String campaignName) throws Exception {

		List<String> userPhoneNumbers = new ArrayList<>();

		Set<String> userSet = new HashSet<String>();
		Application currentApplication = botService.getCampaignFromNameESamwad(campaignName);
		FusionAuthClient clientLogin = fusionAuthClient;
		if (currentApplication != null) {
			// TODO: Step 1 => Get groups for application
			ArrayList<String> groups = (ArrayList<String>) currentApplication.data.get("group");

			// TODO: Step 3 => eSamwad Login and get token
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.loginId = "samarth-admin";
			loginRequest.password = "abcd1234";
			loginRequest.applicationId = UUID.fromString("f0ddb3f6-091b-45e4-8c0f-889f89d4f5da");
			ClientResponse<LoginResponse, Errors> loginResponse = clientLogin.login(loginRequest);

			if (loginResponse.wasSuccessful()) {

				String token = loginResponse.successResponse.token;

				// TODO: Step 4 => Iterate over all filters to get phone number data
				for (String group : groups) {
					ClientResponse<GroupResponse, Errors> groupResponse = fusionAuthClient
							.retrieveGroup(UUID.fromString(group));
					if (groupResponse.wasSuccessful()) {
						String filter = new ObjectMapper()
								.writeValueAsString(groupResponse.successResponse.group.data.get("filterValues"));
						log.info("Group: " + group + "::" + filter);

						OkHttpClient client = new OkHttpClient().newBuilder().build();
						MediaType mediaType = MediaType.parse("application/json");
						RequestBody body = RequestBody.create(mediaType, filter);
						Request request = new Request.Builder()
								.url("http://esamwad.samagra.io/api/v1/segments/students/").method("POST", body)
								.addHeader("Authorization", "Bearer " + token)
								.addHeader("Content-Type", "application/json").build();
						Response response = client.newCall(request).execute();
						String jsonData = response.body().string();
						JSONObject responseJSON = new JSONObject(jsonData);
						ArrayList<String> userPhonesResponse = JSONArrayToList((JSONArray) responseJSON.get("data"));

						// TODO: Step 5 => Create a SET of data to remove duplicates.
						userSet.addAll(userPhonesResponse);
					}
				}

				userPhoneNumbers.addAll(userSet);

			} else if (loginResponse.exception != null) {
				// Exception Handling
				Exception exception = loginResponse.exception;
				log.error("Exception in getting users for eSamwad: " + exception.toString());
			}
		}
		return userPhoneNumbers;
	}

	private ArrayList<String> JSONArrayToList(JSONArray userPhonesResponse) {
		ArrayList<String> usersList = new ArrayList<String>();
		if (userPhonesResponse != null) {
			for (int i = 0; i < userPhonesResponse.length(); i++) {
				usersList.add((String.valueOf(userPhonesResponse.get(i))));
			}
		}
		return usersList;
	}

    public JSONArray getUsersFromFederatedServers(String campaignID, Map<String, String> meta) {

        String baseURL = null;
        String header = null;
        Boolean isHeader = Boolean.FALSE;
        if (meta != null && meta.containsKey("page") && !meta.get("page").isEmpty()) {
            baseURL = CAMPAIGN_URL + "/admin/bot/getAllUsers/" + campaignID + "/" + meta.get("segment") + "/" + meta.get("page");
        } else {
            baseURL = CAMPAIGN_URL + "/admin/bot/getAllUsers/" + campaignID;
        }
        if (meta != null && meta.containsKey("conversation-authorization") && !meta.get("conversation-authorization").isEmpty()) {
            isHeader = Boolean.TRUE;
            header = meta.get("conversation-authorization");
        }
        log.info("UserService:getUsersFromFederatedServers::Calling botId: " + campaignID + " ::: Base URL : " + baseURL + " ::: isHeader Found : " + isHeader);
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        Request request = null;
        if (isHeader) {
            request = new Request.Builder().url(baseURL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("admin-token", CAMPAIGN_ADMIN_TOKEN)
                    .addHeader("Conversation-Authorization", header)
                    .build();
        } else {
            request = new Request.Builder().url(baseURL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("admin-token", CAMPAIGN_ADMIN_TOKEN)
                    .build();
        }

        try {
            Response response = client.newCall(request).execute();
            return (new JSONObject(response.body().string())).getJSONArray("result");
        } catch (Exception e) {
            log.error("Error:getUsersFromFederatedServers::Exception: " + e.getMessage());
        }
        return null;
    }

	public ArrayList<JSONObject> getUsersMessageByTemplate(ObjectNode jsonData) {
		log.info("UserService:getUsersMessageByTemplate::CallingTemplaterService");
		String baseURL = baseUrlTemplate + "/process/testMany";
		OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(90, TimeUnit.SECONDS)
				.writeTimeout(90, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build();
		MediaType mediaType = MediaType.parse("application/json");

  		RequestBody body = RequestBody.create(mediaType, jsonData.toString());
  		Request request = new Request.Builder()
  			  .url(baseURL)
  			  .method("POST", body)
  			  .addHeader("Content-Type", "application/json")
  			  .build();

  		try {
  			Response response = client.newCall(request).execute();
  			log.info("response body: "+response.body());
  			ArrayList<JSONObject> usersMessage= new ArrayList();
  			JSONArray t = (new JSONObject(response.body().string())).getJSONArray("processed");
			for (int i = 0; i < t.length(); i++) {
				JSONObject o = (JSONObject) t.get(i);
				usersMessage.add(o);
			}
  			return usersMessage;
  		} catch (IOException e) {
  			log.error("Error:getUsersMessageByTemplate::Exception: "+e.getMessage());
  		}
  		return null;
    }

	/*
	 * Get the manager for a specific user
	 */
	public User getManager(User applicant) {
		try {
			String managerName = (String) applicant.data.get("reportingManager");
			User u = getUserByFullName(managerName, "SamagraBot");
			if (u != null)
				return u;
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * Get the programCoordinator for a specific user
	 */
	public User getProgramCoordinator(User applicant) {
		try {
			String managerName = (String) applicant.data.get("programCoordinator");
			User u = getUserByFullName(managerName, "SamagraBot");
			if (u != null)
				return u;
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * Get the programConstruct for a specific user
	 */
	public String getProgramConstruct(User applicant) {
		try {
			String programConstruct = String.valueOf(applicant.data.get("programConstruct"));
			if (programConstruct != null)
				return programConstruct;
			else
				return "2";
		} catch (Exception e) {
			return "2";
		}
	}

	/*
	 * Get the manager for a specific user
	 */
	public User getEngagementOwner(User applicant) {
		try {
			String engagementOwnerName = (String) applicant.data.get("programOwner");
			User u = getUserByFullName(engagementOwnerName, "SamagraBot");
			if (u != null)
				return u;
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	public User getUserByFullName(String fullName, String campaignName) throws Exception {
		List<User> allUsers = findUsersForCampaign(campaignName);
		for (User u : allUsers) {
			if (u.fullName.equals(fullName))
				return u;
		}
		return null;
	}

	public User update(User user) {
		ClientResponse<UserResponse, Errors> userResponse = fusionAuthClient.updateUser(user.id,
				new UserRequest(false, false, user));
		if (userResponse.wasSuccessful()) {
			return userResponse.successResponse.user;
		}
		return null;
	}

	public Boolean isAssociate(User applicant) {
		try {
			String role = (String) applicant.data.get("role");
			if (role.equals("Program Associate"))
				return true;
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	public JSONObject getUserByPhoneFromFederatedServers(String campaignID, String phone){
        String baseURL = CAMPAIGN_URL + "/admin/v1/bot/getFederatedUsersByPhone/" + campaignID + "/" + phone;
		String cacheKey = String.format("FEDERATED USERS: USER SERVICE: %s", baseURL);
		if (cache.getIfPresent(cacheKey) != null) {
			return (JSONObject) cache.getIfPresent(cacheKey);
		}
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(baseURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("admin-token", CAMPAIGN_ADMIN_TOKEN)
                .build();
        try {
            Response response = client.newCall(request).execute();
            JSONObject users = new JSONObject(response.body().string());
            try{
            	log.info("campaignID: "+campaignID+", phone: "+phone+", users data: "+users.getJSONObject("result"));
                JSONObject user = users.getJSONObject("result").getJSONObject("data").getJSONObject("user");
                user.put("is_registered", "yes");
				cache.put(cacheKey, user);
                return user;
            }catch (Exception e){
                JSONObject user = new JSONObject();
                user.put("is_registered", "no");
                return user;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

	/**
	 * Register Fusion Auth User, if already exists update
	 * @param username
	 * @param applicationID
	 * @param segment
	 * @return
	 */
	public ObjectNode registerUpdateFAUser(String username, UUID applicationID, FAUserSegment segment) {
		Map<String, Object> data = new HashMap();
		Map<String, String> device = new HashMap();
		device.put("type", segment.getDevice().getType());
		device.put("id", segment.getDevice().getId());

		data.put("device", device);
		if(segment.getUsers() != null && segment.getUsers().get(0) != null){
			FAUser fauser = segment.getUsers().get(0);
			ArrayList<Map<String, String>> users = new ArrayList();
			Map<String, String> user = new HashMap();
			user.put("registrationChannel", fauser.getRegistrationChannel());
			user.put("mobilePhone", fauser.getMobilePhone());
			user.put("username", fauser.getUsername());
			users.add(user);
			data.put("users", users);
		}

		User user = new User()
						.with(usr -> usr.username = username)
						.with(usr -> usr.password = "dummyPassword")
						.with(usr -> usr.active = true)
						.with(usr -> usr.data = data);
		UserRegistration registration = new UserRegistration()
				.with(rg -> rg.applicationId = applicationID)
				.with(rg -> rg.username = username);

		ClientResponse<RegistrationResponse, Errors> response = null;
		User existingUser = findFAUserByUsername(username);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode responseNode = mapper.createObjectNode();

		if(existingUser != null) {
			ClientResponse<UserResponse, Errors> userResponse = fusionAuthClient.updateUser(existingUser.id, new UserRequest(user));
			if(userResponse.wasSuccessful()) {
				UserRegistration existingRegistration = existingUser.getRegistrationForApplication(applicationID);
				if(existingRegistration == null) {
					response = fusionAuthClient.register(existingUser.id, new RegistrationRequest(null, registration));
				} else {
					responseNode.put("success", "true");
					responseNode.put("message", "User registered.");
					return responseNode;
				}
			} else if(userResponse.errorResponse != null) {
				responseNode.put("success", "false");
				responseNode.put("errors", userResponse.errorResponse.fieldErrors.toString());
				return responseNode;
			} else {
				responseNode.put("success", "false");
				responseNode.put("errors", "No response from Fustion Auth.");
				return responseNode;
			}
		} else {
			response = fusionAuthClient.register(null, new RegistrationRequest(user, registration));

		}

		if(response.wasSuccessful()) {
			responseNode.put("success", "true");
			responseNode.put("message", "User registered.");
		} else if(response.errorResponse != null) {
			responseNode.put("success", "false");
			responseNode.put("errors", response.errorResponse.fieldErrors.toString());
		} else {
			responseNode.put("success", "false");
			responseNode.put("errors", "No response from Fustion Auth.");
		}
		return responseNode;
	}

	/**
	 * Create/Register Fusion Auth user
	 * @param node
	 * @return
	 */
	public ObjectNode createFAUser(ObjectNode node) {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		ObjectMapper mapper = new ObjectMapper();

		okhttp3.RequestBody requestBody = null;
		try {
			String url = fusionAuthUrl+"api/user/";
			log.info("url: "+url+", node: "+node);
			requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"),  mapper.writeValueAsString(node));
			Request request = new Request.Builder()
					.url(url)
					.method("POST", requestBody)
					.addHeader("Content-Type", "application/json")
					.addHeader("Authorization", System.getenv("FUSIONAUTH_KEY"))
					.build();

			Response response = client.newCall(request).execute();
			String json = response.body().string();

			ObjectNode responseNode = mapper.createObjectNode();
			ObjectNode resultNode = (ObjectNode) mapper.readTree(json);
			if(resultNode != null) {
				if(resultNode.findValue("fieldErrors") != null) {
					responseNode.put("success", "false");
					ArrayNode errorsNode = mapper.createArrayNode();
					resultNode.findValue("fieldErrors").forEach(error -> {
						if(error.findValue("message") != null) {
							errorsNode.add(error.findValue("message"));
						}
					});
					responseNode.put("errors", errorsNode);
				} else if(resultNode.findValue("generalErrors") != null) {
					responseNode.put("success", "false");
					ArrayNode errorsNode = mapper.createArrayNode();
					resultNode.findValue("generalErrors").forEach(error -> {
						if(error.findValue("message") != null) {
							errorsNode.add(error.findValue("message"));
						}
					});
					responseNode.put("errors", errorsNode);
				} else {
					responseNode.put("success", "true");
					responseNode.put("data", resultNode);
				}
			} else {
				responseNode.put("success", "false");
				responseNode.put("errors", "No response from Fusion Auth");
			}
			return responseNode;
		} catch (JsonProcessingException e) {
			log.error("JsonProcessingException in updateFAUser: "+e.getMessage());
		} catch (IOException e) {
			log.error("IOException in updateFAUser: "+e.getMessage());
		} catch (Exception e){
			log.error("Exception in updateFAUser: "+e.getMessage());
		}
		return null;
	}

	/**
	 * Update Fusion Auth user by user uuid
	 * @param userId
	 * @param node
	 * @return
	 */
	public ObjectNode updateFAUser(String userId, ObjectNode node) {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		ObjectMapper mapper = new ObjectMapper();

		okhttp3.RequestBody requestBody = null;
		try {
			String url = fusionAuthUrl+"api/user/"+userId;
			log.info("url: "+url+", node: "+node);
			requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"),  mapper.writeValueAsString(node));
			Request request = new Request.Builder()
					.url(url)
					.method("PUT", requestBody)
					.addHeader("Content-Type", "application/json")
					.addHeader("Authorization", System.getenv("FUSIONAUTH_KEY"))
					.build();

			Response response = client.newCall(request).execute();
			String json = response.body().string();

			ObjectNode responseNode = mapper.createObjectNode();
			ObjectNode resultNode = (ObjectNode) mapper.readTree(json);
			if(resultNode != null) {
				if(resultNode.findValue("fieldErrors") != null) {
					responseNode.put("success", "false");
					ArrayNode errorsNode = mapper.createArrayNode();
					resultNode.findValue("fieldErrors").forEach(error -> {
						if(error.findValue("message") != null) {
							errorsNode.add(error.findValue("message"));
						}
					});
					responseNode.put("errors", errorsNode);
				} else if(resultNode.findValue("generalErrors") != null) {
					responseNode.put("success", "false");
					ArrayNode errorsNode = mapper.createArrayNode();
					resultNode.findValue("generalErrors").forEach(error -> {
						if(error.findValue("message") != null) {
							errorsNode.add(error.findValue("message"));
						}
					});
					responseNode.put("errors", errorsNode);
				} else {
					responseNode.put("success", "true");
					responseNode.put("data", resultNode);
				}
			} else {
				responseNode.put("success", "false");
				responseNode.put("errors", "No response from Fusion Auth");
			}
			return responseNode;
		} catch (JsonProcessingException e) {
			log.error("JsonProcessingException in updateFAUser: "+e.getMessage());
		} catch (IOException e) {
			log.error("IOException in updateFAUser: "+e.getMessage());
		} catch (Exception e){
			log.error("Exception in updateFAUser: "+e.getMessage());
		}
		return null;
	}
}