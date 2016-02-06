import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FrogUtils {
	public static Gson gson;
	
	public static JsonObject JsonObjectFromFile(String fileUrl){
		if(gson == null) gson = new Gson();
		try{
			File jsonFile = new File(fileUrl);
			JsonObject jsonObject = (new Gson()).fromJson(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8), JsonObject.class);
			
			return jsonObject;
		}catch(Exception e){}
		return null;
	}
	
	public static String GetHTML(String urlToRead) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", Config.userAgent);
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
