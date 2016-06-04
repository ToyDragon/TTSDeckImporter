package cardbuddies;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import core.Config;
import utils.FrogUtils;

public class Token {
	public static HashMap<String, Token> nameToTokenMap = new HashMap<String, Token>();
	public static HashMap<String, ArrayList<Token>> cardToTokenMap = new HashMap<String, ArrayList<Token>>();
	
	public String name;
	public BufferedImage image;
	public int jsonId;
	
	public static void LoadTokenMap(){
		JsonObject tokenObj = FrogUtils.JsonObjectFromFile(Config.tokenListDir);
		Set<Entry<String, JsonElement>> tokenSet = tokenObj.entrySet();
		for(Entry<String, JsonElement> entry : tokenSet){
			String cardName = entry.getKey();
			JsonArray rawTokenArray = entry.getValue().getAsJsonArray();
			for(int i = 0; i < rawTokenArray.size(); i++){
				String tokenName = rawTokenArray.get(i).getAsString();
				Token token = nameToTokenMap.get(tokenName);
				if(token == null){
					try{
						Token temp = new Token();
						temp.name = tokenName;
						temp.image = ImageIO.read(new File(Config.tokenAssetDir+"/"+tokenName+".jpg"));
						
						token = temp;
						nameToTokenMap.put(token.name, token);
					}catch(Exception e){}
				}
				if(token == null)continue;
				
				ArrayList<Token> curList = cardToTokenMap.get(cardName);
				if(curList == null){
					curList = new ArrayList<Token>(10);
					cardToTokenMap.put(cardName, curList);
				}
				
				curList.add(token);
			}
		}
	}
}
