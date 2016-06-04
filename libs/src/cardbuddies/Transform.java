package cardbuddies;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import core.Config;
import utils.FrogUtils;

public class Transform{
	public static HashMap<String, String> nameToTransformMap = new HashMap<String, String>();
	
	public static void LoadTransformMap(){
		JsonObject transformObj = FrogUtils.JsonObjectFromFile(Config.transformMapDir);
		Set<Entry<String, JsonElement>> transformSet = transformObj.entrySet();
		for(Entry<String, JsonElement> entry : transformSet){
			String cardName = entry.getKey();
			String transformName = entry.getValue().getAsString();
			nameToTransformMap.put(cardName, transformName);
		}
	}
}
