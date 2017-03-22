package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import helpers.Pair;
import utils.FrogUtils;

public class Config {
	public static String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97 Safari/537.36";

	public static String deckDir;
	public static String imageDir;
	public static String setAssetDir;

	public static String publicDeckDir = "/decks/";
	public static String publicSetAssetDir; 
	
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
	public static Map<String, ArrayList<Pair<String, String>>> fullArtMap;
	
	public static boolean LoadConfig(){
		try{
			JsonObject configObject = FrogUtils.JsonObjectFromFile("settings.json");

			deckDir = configObject.getAsJsonPrimitive("deckDir").getAsString();
			imageDir = configObject.getAsJsonPrimitive("imageDir").getAsString();
			setAssetDir = configObject.getAsJsonPrimitive("setAssetDir").getAsString();
			
			tokenAssetDir = configObject.getAsJsonPrimitive("tokenImageDir").getAsString();

			port = configObject.getAsJsonPrimitive("port").getAsInt();

			tokenListDir = configObject.getAsJsonPrimitive("tokenListDir").getAsString();
			//System.out.println("TEST: " + configObject.getAsJsonPrimitive("tokenListDir"));
			transformMapDir = configObject.getAsJsonPrimitive("transformMapDir").getAsString();

			hostUrlPrefix = configObject.getAsJsonPrimitive("hostUrlPrefix").getAsString();

			defaultBackImage = configObject.getAsJsonPrimitive("defaultBackImage").getAsString();
			
			publicSetAssetDir = "/setAssets/v" + configObject.getAsJsonPrimitive("setAssetVersion").getAsString() + "/";
		
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
			
			JsonArray fullArtLandConfigArr = configObject.getAsJsonArray("fullArtLands");
			fullArtMap = new HashMap<String, ArrayList<Pair<String, String>>>();
			for(int i = 0; i < fullArtLandConfigArr.size(); i++) {
				ArrayList<Pair<String, String>> printingList = new ArrayList<Pair<String, String>>();
				
				String landName = fullArtLandConfigArr.get(i).getAsJsonObject().get("landType").getAsString();
				JsonArray printingInfoArr = fullArtLandConfigArr.get(i).getAsJsonObject().get("printInfo").getAsJsonArray(); //array of printInfos.  PrintInfos have sets and associated print numbers 

				for(int j = 0; j < printingInfoArr.size(); j++) {
					JsonObject setObj = printingInfoArr.get(j).getAsJsonObject(); //printInfo's set
					String setName = setObj.get("set").getAsString();
					JsonArray printNumberArr = setObj.getAsJsonArray("printNumbers"); //printInfo's arr of print numbers
					for(int k = 0; k < printNumberArr.size(); k++) {
						String printNumber = printNumberArr.get(k).getAsString();
						Pair<String, String> printing = new Pair<String, String>(setName, printNumber); //each printing is defined by a set and printNumber pair
						printingList.add(printing);
					}
				}
				fullArtMap.put(landName, printingList);
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
