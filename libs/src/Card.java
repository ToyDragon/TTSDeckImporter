
public class Card {
	
	public String name;
	public String transformName;
	public String set;
	public String printing;
	public String language;
	public String multiverseId;
	
	public String line;
	
	public String cardKey;
	
	public int[] amounts;
	
	public boolean doubleSided;
	
	public String imageFileName;
	public String transformImageFileName;
	
	public int jsonId;
	
	public Card(){
		amounts = new int[3];
	}

	public static String getCardKey(String cardName, String set, String printing, String language) {
		String cardKey = cardName;
		if(language != null)cardKey+="{"+language+"}";
		if(set != null)cardKey+="["+set+"]";
		if(printing != null)cardKey+="<"+printing+">";
		return cardKey;
	}
	
	public String toString(){
		return "{ name: " + name + ", amount: " + amounts[1] + ", multiverseId: " + multiverseId + "}";
	}
}
