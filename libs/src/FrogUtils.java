import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FrogUtils {
	public static Gson gson;
	
	public static JsonObject JsonObjectFromFile(String fileUrl){
		try{
			File jsonFile = new File(fileUrl);
			JsonObject jsonObject = FrogUtils.gson.fromJson(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8), JsonObject.class);
			
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
		long start = System.currentTimeMillis();
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", Config.userAgent);
			conn.setConnectTimeout(1000);
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			long time = (System.currentTimeMillis() - start)/1000;
			System.out.println("Failed to download html in " + time + " seconds, from " + urlToRead);
		}
		return result;
	}
}
