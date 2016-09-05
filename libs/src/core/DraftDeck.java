package core;
import java.util.ArrayList;

public class DraftDeck extends Deck{
	public static final String[] RARITIES={"Mythic Rare","Rare","Uncommon","Common","Basic Land","Special"};
	
	public ArrayList<ArrayList<Card>> cardsByRarity = new ArrayList<ArrayList<Card>>();
	public ArrayList<Card> basics = new ArrayList<Card>();
	
	public DraftDeck(){
		compressionLevel = 1.0;
		for(int i = 0; i < RARITIES.length; i++){
			cardsByRarity.add(new ArrayList<Card>());
		}
	}
	
	public String setName;
	public String code;
	public int[] boosterAmts = new int[RARITIES.length];

	public int amountPacks;
	public int curRarity;
	
	public void add(Card card){
		if(!cardsByRarity.get(curRarity).contains(card)){
			cardsByRarity.get(curRarity).add(card);
		}
		super.add(card);
	}
}
