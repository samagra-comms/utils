package com.uci.utils.bot.util;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

public class FileUtil {

	/**
	 * Function to get Mime type of file from url
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String getMimeTypeFromUrl(String url) throws IOException {
		File file = new File(url);
		URLConnection connection = file.toURL().openConnection();
		String mimeType = connection.getContentType();
		return mimeType;
	}
	
	/**
	 * Check if file type is image
	 * @param mime_type
	 * @return
	 */
	public static boolean isFileTypeImage(String mime_type) {
		ArrayList<String> list = getImageFileTypes();
		for(int i=0; i < list.size(); i++) {
			if(list.get(i).equals(mime_type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if file type is image
	 * @param mime_type
	 * @return
	 */
	public static boolean isFileTypeAudio(String mime_type) {
		ArrayList<String> list = getAudioFileTypes();
		for(int i=0; i < list.size(); i++) {
			if(list.get(i).equals(mime_type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if file type is image
	 * @param mime_type
	 * @return
	 */
	public static boolean isFileTypeVideo(String mime_type) {
		ArrayList<String> list = getVideoFileTypes();
		for(int i=0; i < list.size(); i++) {
			if(list.get(i).equals(mime_type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if file type is image
	 * @param mime_type
	 * @return
	 */
	public static boolean isFileTypeDocument(String mime_type) {
		ArrayList<String> list = getDocumentFileTypes();
		for(int i=0; i < list.size(); i++) {
			if(list.get(i).equals(mime_type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if file mime type is valid
	 * @param mime_type
	 * @return
	 */
	public static boolean isValidFileType(String mime_type) {
		ArrayList<String> list = getImageFileTypes();
		list.addAll(getAudioFileTypes());
		list.addAll(getVideoFileTypes());
		list.addAll(getDocumentFileTypes());
		for(int i=0; i < list.size(); i++) {
			if(list.get(i).equals(mime_type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get Image file types list
	 * @param data
	 * @return
	 */
	public static ArrayList<String> getImageFileTypes() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("image/jpg");
		list.add("image/jpeg");
		list.add("image/gif");
		list.add("image/png");
		return list;
	}

	/**
	 * Get Audio file types list
	 * @param data
	 * @return
	 */
	public static ArrayList<String> getAudioFileTypes() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("audio/mp3");
		list.add("audio/aac");
		list.add("audio/wav");
		list.add("audio/flac");
		list.add("audio/ogg");
		list.add("audio/ogg; codecs=opus");
		list.add("audio/wma");
		list.add("audio/x-ms-wma"); //wma
		list.add("audio/mpeg");
		return list;
	}
	
	/**
	 * Get Video file types list
	 * @param data
	 * @return
	 */
	public static ArrayList<String> getVideoFileTypes() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("video/mp4");
		list.add("video/flv");
		list.add("video/mov");
		list.add("video/wmv");
		list.add("video/mkv");
		list.add("video/quicktime"); //mov
		list.add("video/x-matroska"); //mkv
		list.add("video/x-flv"); //flv
		return list;
	}
	
	/**
	 * Get Document file types list
	 * @param data
	 * @return
	 */
	public static ArrayList<String> getDocumentFileTypes() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("application/pdf");
		list.add("application/msword");
		list.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		return list;
	}
	
	/**
	 * Get File type by mime sub type 
	 * @param type
	 * @return
	 */
	public static String getFileTypeByMimeSubTypeString(String type) {
		String fileType = type;
		switch (type) {
			case "vnd.openxmlformats-officedocument.wordprocessingml.document":
				fileType = "docx";
				break;
			case "msword":
				fileType = "doc";
				break;
			case "x-matroska":
				fileType = "mkv";
				break;
			case "x-flv":
				fileType = "flv";
				break;
			case "x-ms-wma":
				fileType = "wma";
				break;
			default:
				break;
		}
		return fileType;
	}
	
}
