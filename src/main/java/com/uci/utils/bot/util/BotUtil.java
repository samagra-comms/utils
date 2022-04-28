package com.uci.utils.bot.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BotUtil {
	public static String botEnabledStatus = "enabled";
	public static String botLiveStatus = "live";

	public static String getBotValidFromJsonNode(JsonNode data) {
		String status = data.findValue("status").asText();
    	String startDate = data.findValue("startDate").asText();
    	String endDate = data.findValue("endDate").asText();
		
    	log.info("Bot Status: "+status+", Start Date: "+startDate+", End Date: "+endDate);
    	
    	return getBotValid(status, startDate, endDate);
	}
	
	public static String getBotValid(String status, String startDate, String endDate) {
		if(!checkBotLiveStatus(status)) {
			return String.format("This conversation is not active yet. Please contact your state admin to seek help with this.");
		} else if(startDate == null || startDate == "null" || startDate.isEmpty()) {
			log.info("Bot start date is empty.");
			return String.format("This conversation is not active yet. Please contact your state admin to seek help with this..");
		} else if(!checkBotStartDateValid(startDate)) {
			if(!startDate.isEmpty()) {
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
				try {
					Date date=fmt.parse(startDate);
					SimpleDateFormat fmt2 = new SimpleDateFormat("dd/MM/yy");
					startDate = fmt2.format(date).toString();
				} catch (ParseException e) {
					log.info("Date cannot be formatted");
				}
				return String.format("This conversation is not active yet. It will be enabled on %s. Please try again then.", startDate);
			}
			return String.format("This conversation is not active yet. Please try again then.");
		} else if(!checkBotEndDateValid(endDate)) {
			return String.format("This conversation has expired now. Please contact your state admin to seek help with this.");
		}
		return "true";
	}
	
	public static Boolean checkBotValidFromJsonNode(JsonNode data) {
		String status = data.findValue("status").asText();
    	String startDate = data.findValue("startDate").asText();
    	String endDate = data.findValue("endDate").asText();
		
    	log.info("Bot Status: "+status+", Start Date: "+startDate+", End Date: "+endDate);
    	
    	return checkBotValid(status, startDate, endDate);
	}
	
	public static Boolean checkBotValid(String status, String startDate, String endDate) {
		if(checkBotLiveStatus(status) && checkBotStartDateValid(startDate) 
				&& checkBotEndDateValid(endDate)
				&& !(startDate == null || startDate == "null" || startDate.isEmpty())) {
			return true;
		}
		return false;
	}
	
	public static Boolean checkBotLiveStatus(String status) {
		status = status.toLowerCase();
		if(status.equals(botLiveStatus) || status.equals(botEnabledStatus)) {
			return true;
		}
		log.info("Bot is invalid as its status is not live or enabled.");
		return false;
	}
	
	public static Boolean checkBotStartDateValid(String startDate) {
		try {
			/* Start Date  */
//			log.info("Start Date empty check: "+(startDate == null || startDate == "null" || startDate.isEmpty()));
//			if(startDate == null || startDate == "null" || startDate.isEmpty()) {
//				log.info("Bot start date is empty.");
//				return true;
//			}
			
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        	
        	/* local date time */
        	LocalDateTime localNow = LocalDateTime.now();
        	String dateString = fmt.format(localNow).toString();
        	LocalDateTime localDateTime = LocalDateTime.parse(dateString, fmt);
        	
        	/* bot start date in local date time format */
        	LocalDateTime localStartDate = LocalDateTime.parse(startDate, fmt);
            
//        	log.info("localDateTime: "+localDateTime+", localStartDate: "+localStartDate+", Comparison: "+localDateTime.compareTo(localStartDate));
        	if(localDateTime.compareTo(localStartDate) >= 0) {
        		return true;
        	} else {
        		log.info("Bot is invalid as its start date is a greator than the current date.");
        	}
		} catch (Exception e) {
			log.error("Error in checkBotStartDateValid: "+e.getMessage());
		}
		return false;
	}
	
	public static Boolean checkBotEndDateValid(String endDate) {
		try {
			/* End Date  */
//			log.info("End Date empty check: "+(endDate == null || endDate == "null" || endDate.isEmpty()));
			if(endDate == null || endDate == "null" || endDate.isEmpty()) {
				log.info("Bot end date is empty.");
				return true;
			}
			
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        	
        	/* local date time */
        	LocalDateTime localNow = LocalDateTime.now();
        	String dateString = fmt.format(localNow).toString();
        	LocalDateTime localDateTime = LocalDateTime.parse(dateString, fmt);
        	
        	/* bot end date in local date time format */
        	LocalDateTime localEndDate = LocalDateTime.parse(endDate, fmt);
        	localEndDate = localEndDate.plusHours(23).plusMinutes(59).plusSeconds(59);
        	
//        	log.info("localDateTime: "+localDateTime+", localEndDate: "+localEndDate+", Comparison: "+localDateTime.compareTo(localEndDate));
        	if(localDateTime.compareTo(localEndDate) < 0) {
        		return true;
        	} else {
        		log.info("Bot is invalid as its end date is less than the current date.");
        	}
		} catch (Exception e) {
			log.error("Error in checkBotEndDateValid: "+e.getMessage());
		}
		return false;
	}
}

