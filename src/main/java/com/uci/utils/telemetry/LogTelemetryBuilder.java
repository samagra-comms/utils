package com.uci.utils.telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uci.utils.telemetry.dto.Actor;
import com.uci.utils.telemetry.dto.Context;
import com.uci.utils.telemetry.dto.Producer;
import com.uci.utils.telemetry.dto.Telemetry;
import com.uci.utils.telemetry.util.TelemetryEventNames;
import com.uci.utils.telemetry.util.TelemetryEventTypes;
import com.uci.utils.telemetry.util.TelemetryEvents;

public class LogTelemetryBuilder {
	private static final String TELEMETRY_IMPL_VERSION = "3.0";
    private static final String LOG_TELEMETRY_IMPL_VERSION = "1.0";
    private static final String ACTOR_TYPE_SYSTEM = "System";
    private static final String ACTOR_TYPE_USER = "User";
    private static final String DIKSHA_ORG = "DIKSHA";
    private static final String pageId = "bot-screen";
    private static final String LOG_EVENT_MID = "fb3db9abceb578d8acbc812cdbd9c931";
    //represents the MD5 Hash of "Individual Question-Responses - Survey/Questionnaire"

    public String build(
            String message,
    		TelemetryEventNames eventName,
            String botOrg,
            String channel,
            String provider,
            String producerID, 
            String userID, 
            String conversationId,
            String conversationOwnerId) {
    	
    	/* Event related parameters */
    	Map oData =  getEData(eventName);
    	
    	/* Channel Name */
    	String channelName = (botOrg != null && botOrg.equalsIgnoreCase("Anonymous")) ? DIKSHA_ORG : botOrg;
        
    	/* Environment */
    	String env = channel != null && !channel.isEmpty()  
        		? (provider != null && !provider.isEmpty() ? channel + "." + provider : channel) 
        		: (provider != null && !provider.isEmpty() ? provider : null);
        
    	/* Conversation Details */
    	List<Map<String, Object>> cdata = new ArrayList();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("ConversationOwner", conversationOwnerId);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("Conversation", conversationId);
        cdata.add(map1);
        cdata.add(map2);
        
        Map<String, String> rollup = new HashMap<>();
        rollup.put("l1", "ConversationOwner");
        rollup.put("l2", "Conversation");
    	
    	/* Context */
        Context context = Context.builder()
                .channel(channelName)
                .env(env)
                .pdata(Producer.builder()
                        .id("prod.uci.diksha")
                        .pid(producerID).ver(LOG_TELEMETRY_IMPL_VERSION)
                        .build()
                )
                .did(userID)
                .cdata(cdata)
                .rollup(rollup)
                .build();
        
        /* Edata */
        Map<String, Object> edata = new HashMap<>();
        edata.put("type", oData.get("edataType").toString());
        edata.put("level", "INFO");
        edata.put("pageid", pageId);
        edata.put("message", message);
        
        /* If actor type not user, replace used id with empty string */
        if(!oData.get("actorType").toString().equals(ACTOR_TYPE_USER)) {
        	userID = "";
        }
       
        /* Telemetry Object */
        Telemetry telemetry = Telemetry.builder().
                eid(oData.get("eid").toString()).
                ets(System.currentTimeMillis()).
                ver(TELEMETRY_IMPL_VERSION).
                mid(LOG_EVENT_MID)
                .actor(Actor.builder()
                        .type(oData.get("actorType").toString())
                        .id(userID)
                        .build())
                .context(context)
                .edata(edata)
                .build();
        return Telemetry.getTelemetryRequestData(telemetry);
    }
    
    private Map getEData(TelemetryEventNames event) {
    	Map edata = new HashMap();
    	edata.put("eid", "");
		edata.put("edataType", "");
		edata.put("actorType", "");
    	if(event != null) {
    		switch (event) {
				case STARTCONVERSATION:
					edata.put("eid", TelemetryEvents.START.getName());
					edata.put("edataType", TelemetryEventTypes.CONVERSATION.getName());
					edata.put("actorType", ACTOR_TYPE_USER);
					break;
				case ENDCONVERSATION:
					edata.put("eid", TelemetryEvents.START.getName());
					edata.put("edataType", TelemetryEventTypes.CONVERSATION.getName());
					edata.put("actorType", ACTOR_TYPE_USER);
					break;
				case SWITCHCONVERSATIONLOGIC:
					edata.put("eid", TelemetryEvents.INTERACT.getName());
					edata.put("edataType", TelemetryEventTypes.SWITCHCONVERSATION.getName());
					edata.put("actorType", ACTOR_TYPE_USER);
					break;
				case PRETRANSFORMER:
					edata.put("eid", TelemetryEvents.LOG.getName());
					edata.put("edataType", TelemetryEventTypes.PRETRANSFORMER.getName());
					break;
				case POSTTRANSFORMER:
					edata.put("eid", TelemetryEvents.LOG.getName());
					edata.put("edataType", TelemetryEventTypes.POSTTRANSFORMER.getName());
					break;
				case SENT:
					edata.put("eid", TelemetryEvents.AUDIT.getName());
					edata.put("edataType", TelemetryEventTypes.SENT.getName());
					break;
				case DELIVERED:
					edata.put("eid", TelemetryEvents.AUDIT.getName());
					edata.put("edataType", TelemetryEventTypes.DELIVERED.getName());
					break;
				case READ:
					edata.put("eid", TelemetryEvents.AUDIT.getName());
					edata.put("edataType", TelemetryEventTypes.READ.getName());
					edata.put("actorType", ACTOR_TYPE_USER);
					break;
				case AUDITEXCEPTIONS:
					edata.put("eid", TelemetryEvents.ERROR.getName());
					edata.put("edataType", TelemetryEventTypes.EXCEPTION.getName());
					break;
				default:
					break;
			}
    	}
    	return edata;
    }
}

