package core;

import utils.FrogUtils;

public class Card {
	
	public String name;
	public String transformName;
	public String set;
	public String printing;
	public String language;
	public String multiverseId;
	
	public String line;
	
	public String cardKey;
	public String transformCardKey;
	
	public int[] amounts;
	
	public boolean doubleSided;
	
	public String imageFileName;
	public String transformImageFileName;
	
	public int jsonId;
	public int transformJsonId;
	
	public Card(){
		amounts = new int[3];
	}
	
	public String getDisplayName(){
		try{
			String displayName = "";
			String processedName = name;
			int start = name.indexOf("(")+1;
			int end = name.indexOf(")");
			if(start >= 0 && end > start){
				processedName = name.substring(start,end);
			}
			String[] words = (processedName + "").split(" ");
			for(int i = 0; i < words.length; i++){
				displayName += (words[i].charAt(0)+"").toUpperCase();
				if(words[i].length() > 1){
					displayName += words[i].substring(1);
				}
				if(i < words.length-1)displayName += " ";
			}
			return displayName;
		}catch(Exception e){return name;}
	}

	public static String getCardKey(String cardName, String set, String printing, String language) {
		String cardKey = FrogUtils.ReplaceHardChars(cardName);
		if(language != null)cardKey+="{"+language+"}";
		if(set != null)cardKey+="["+set+"]";
		if(printing != null)cardKey+="<"+printing+">";
		return cardKey;
	}
	
	public String toString(){
		return "{ name: " + name + ", amount: " + amounts[1] + ", multiverseId: " + multiverseId + "}";
	}
}
