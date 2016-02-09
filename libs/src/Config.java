import java.net.InetAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Config {
	public static String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97 Safari/537.36";

	public static String deckDir;
	public static String imageDir;
	public static String setAssetDir;

	public static String publicDeckDir = "/decks/";
	public static String publicSetAssetDir = "/setAssets/"; 
	
	public static String tokenAssetDir;
	
	public static int port;
	
	public static String imageBackDir;
	public static String tokenListDir;
	public static String transformMapDir;
	
	public static String hostname;
	public static String expectedLocalHostName;
	public static String expectedHostName;
	public static String hostUrlPrefix;
	
	public static String defaultBackImage;

	public static String[][] hardUrls;
	public static String[][] hardNameCharacters;
	public static String[][] mythicErrors;
	
	public static boolean LoadConfig(){
		try{
			JsonObject configObject = FrogUtils.JsonObjectFromFile("settings.json");

			deckDir = configObject.getAsJsonPrimitive("deckDir").getAsString();
			imageDir = configObject.getAsJsonPrimitive("imageDir").getAsString();
			setAssetDir = configObject.getAsJsonPrimitive("setAssetDir").getAsString();
			
			tokenAssetDir = configObject.getAsJsonPrimitive("tokenImageDir").getAsString();

			port = configObject.getAsJsonPrimitive("port").getAsInt();

			tokenListDir = configObject.getAsJsonPrimitive("tokenListDir").getAsString();
			transformMapDir = configObject.getAsJsonPrimitive("transformMapDir").getAsString();

			expectedLocalHostName = configObject.getAsJsonPrimitive("expectedLocalHostName").getAsString();
			expectedHostName = configObject.getAsJsonPrimitive("expectedHostName").getAsString();
			hostname = InetAddress.getLocalHost().getHostName();
			
			if(hostname.equalsIgnoreCase(expectedLocalHostName)){
				hostUrlPrefix = "http://localhost/";
			}else{
				hostUrlPrefix = expectedHostName;
			}

			defaultBackImage = configObject.getAsJsonPrimitive("defaultBackImage").getAsString();
		
			JsonArray hardUrlArr = configObject.getAsJsonArray("hardUrls");
			hardUrls = new String[hardUrlArr.size()][2];
			for(int i = 0; i < hardUrlArr.size(); i++){
				JsonObject hardUrlObj = hardUrlArr.get(i).getAsJsonObject();
				hardUrls[i] = new String[]{
						hardUrlObj.get("name").getAsString(),
						hardUrlObj.get("url").getAsString()
				};
			}
			
			JsonArray mythicErrorsArr = configObject.getAsJsonArray("mythicErrors");
			mythicErrors = new String[mythicErrorsArr.size()][2];
			for(int i = 0; i < mythicErrorsArr.size(); i++){
				JsonObject mythicErrorObj = mythicErrorsArr.get(i).getAsJsonObject();
				mythicErrors[i] = new String[]{
						mythicErrorObj.get("name").getAsString(),
						mythicErrorObj.get("correction").getAsString()
				};
			}
			
			JsonArray hardNameArr = configObject.getAsJsonArray("cardNameReplacements");
			hardNameCharacters = new String[hardNameArr.size()][2];
			for(int i = 0; i < hardNameArr.size(); i++){
				JsonObject harNameObj = hardNameArr.get(i).getAsJsonObject();
				hardNameCharacters[i] = new String[]{
						harNameObj.get("hard").getAsString(),
						harNameObj.get("easy").getAsString()
				};
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
