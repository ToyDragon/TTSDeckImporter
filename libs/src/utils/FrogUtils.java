package utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import core.Config;

/**
 * Miscellaneous helper methods
 * @author Matt
 */
public class FrogUtils {
	public static Gson gson;
	public static boolean DEBUG = false;
	
	public static JsonObject JsonObjectFromFile(String fileUrl){
		try{
			File jsonFile = new File(fileUrl);
			JsonObject jsonObject = FrogUtils.gson.fromJson(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8), JsonObject.class);
			
			return jsonObject;
		}catch(Exception e){}
		return null;
	}
	
	public static void Debug(String msg){
		if(DEBUG){
			System.out.println(msg);
		}
	}
	
	public static String GetHTML(String urlToRead) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		long start = System.currentTimeMillis();
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", Config.userAgent);
			conn.setConnectTimeout(1500);
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
			System.out.println("Loaded from " + urlToRead);
		} catch (Exception e) {
			long time = (System.currentTimeMillis() - start);
			System.out.println("Failed to download html in " + time + " miliseconds, from " + urlToRead);
			System.out.println(e);
		}
		return result;
	}
	
	public static String StringBetween(String line, String left, String right){
		int leftIndex,rightIndex;
		leftIndex = line.indexOf(left); rightIndex = line.indexOf(right);
		if(leftIndex > 0 && rightIndex > leftIndex + 1) return line.substring(leftIndex + 1,rightIndex);
		return "";
	}
	
	/**
	 * Replaces hard name characters, and formats multi-sided cards.
	 * @param cardName
	 * @return
	 */
	public static String CleanCardName(String cardName){
		for(String[] hardPair : Config.hardNameCharacters){
			cardName = cardName.replaceAll("\\Q"+hardPair[0]+"\\E", hardPair[1]);
		}
		
		cardName = cardName.replaceAll("/+", "/");
		if(cardName.contains("/")){
			//make double sided cards consistent with magiccards.info
			//Wear // Tear
			int start = cardName.lastIndexOf("(")+1;
			int end = cardName.indexOf(")");
			if(end <= 0) end = cardName.length();
			String leftHalf = cardName.substring(start, cardName.indexOf("/")).trim();
			String rightHalf = cardName.substring(cardName.indexOf("/")+1, end).trim();
			
			cardName = leftHalf+=" ("+leftHalf+"/"+rightHalf+")";
		}
		return cardName;
	}
}
