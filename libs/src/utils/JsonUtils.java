package utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cardbuddies.Token;
import core.Card;
import core.Config;
import core.Deck;
import core.DraftDeck;

public class JsonUtils {
	public static String GetGUID(){
		String chars = "0123456789abcdef";
		String uid = "";
		for(int i = 0; i < 6; i++){
			uid += chars.charAt((int)Math.floor(Math.random()*chars.length()));
		}
		return uid;
	}
	
	public static JsonObject NewDeckBaseObject(JsonArray stateIDs, String name){
		JsonObject colorObj = new JsonObject();
		colorObj.add("r", new JsonPrimitive(0.713235259));
		colorObj.add("g", new JsonPrimitive(0.713235259));
		colorObj.add("b", new JsonPrimitive(0.713235259));
		
		JsonObject baseState = new JsonObject();
		baseState.add("Name", new JsonPrimitive(stateIDs.size() == 1 ? "Card" : "DeckCustom"));
		baseState.add("Nickname", new JsonPrimitive(name));
		baseState.add("Description", new JsonPrimitive(""));
		baseState.add("Grid", new JsonPrimitive(true));
		baseState.add("Locked", new JsonPrimitive(false));
		baseState.add("SidewaysCard", new JsonPrimitive(false));
		baseState.add("GUID", new JsonPrimitive(GetGUID()));
		baseState.add("ColorDiffuse", colorObj);
		
		if(stateIDs.size() == 1){
			baseState.add("CardID", stateIDs.get(0));
		}else{
			baseState.add("DeckIDs", stateIDs);
		}
		return baseState;
	}
	
	public static JsonObject NewDeckPosObject(int x, int y, int z, boolean faceup, double scale){
		JsonObject statePos = new JsonObject();
		statePos.add("posX", new JsonPrimitive(2.5 * x));
		statePos.add("posY", new JsonPrimitive(2.5 * y));
		statePos.add("posZ", new JsonPrimitive(3.5 * z));
		statePos.add("rotX", new JsonPrimitive(0));
		statePos.add("rotY", new JsonPrimitive(180));
		statePos.add("rotZ", new JsonPrimitive(faceup ? 0 : 180));
		statePos.add("scaleX", new JsonPrimitive(scale));
		statePos.add("scaleY", new JsonPrimitive(scale));
		statePos.add("scaleZ", new JsonPrimitive(scale));
		
		return statePos;
	}
	
	public static JsonObject NewDeckStateObject(ArrayList<Integer> deckIds, Deck deck){
		int cardsPerDeck = 69;
		int regularDecks = (int) Math.ceil((deck.cardList.size() + deck.tokens.size())/(double)cardsPerDeck);
		JsonObject deckStateObject = new JsonObject();
		for(int i = 0; i < deckIds.size(); i++){
			int deckID = deckIds.get(i);
			int transId = deckID - regularDecks - 1;
			if(transId >= 0){
				if(transId % 2 == 0){
					JsonObject deckObj = new JsonObject();
					deckObj.add("FaceURL", new JsonPrimitive(deck.deckLinks[deckID-1]));
					deckObj.add("BackURL", new JsonPrimitive(deck.deckLinks[deckID] + "{Unique}"));
					deckStateObject.add(""+(deckID), deckObj);
				}
			}else{
				JsonObject deckObj = new JsonObject();
				deckObj.add("FaceURL", new JsonPrimitive(deck.deckLinks[deckID-1]));
				deckObj.add("BackURL", new JsonPrimitive(deck.backUrl));
				deckStateObject.add(""+deckID, deckObj);
			}
		}

		return deckStateObject;
	}
	
	public static JsonObject NewCardObject(int id, String name){
		JsonObject cardObject = new JsonObject();
		cardObject.add("Name", new JsonPrimitive("Card"));
		cardObject.add("Nickname", new JsonPrimitive(""+name));
		cardObject.add("CardID", new JsonPrimitive(id));
		cardObject.add("Transform", NewDeckPosObject(1, 1, 1, false, 1.0));
		
		return cardObject;
	}
	
	public static void BuildJSONFile(Deck deck){
			
		JsonObject deckJSON = new JsonObject();
		
		String[] emptyProps = {"SaveName", "GameMode", "Date", "Table", "Sky", "Note", "Rules", "PlayerTurn"};
		for(String prop : emptyProps){
			deckJSON.add(prop, new JsonPrimitive(""));
		}

		JsonArray objectStates = new JsonArray();
		
		//commander object state----------------------------------------
		int curStateIndex = 0;
		JsonArray commanderStateIDs = new JsonArray();
		JsonArray commanderContents = new JsonArray();
		ArrayList<Integer> commanderStateDeckIDs = new ArrayList<Integer>();
		for(Card card : deck.cardList){
			if(card.amounts[curStateIndex] == 0)continue;
			int deckID = card.jsonId/100;
			if(!commanderStateDeckIDs.contains(deckID)) commanderStateDeckIDs.add(deckID);
			commanderStateIDs.add(new JsonPrimitive(card.jsonId));
			commanderContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
		}

		JsonObject commanderState = NewDeckBaseObject(commanderStateIDs, "Commander");
		commanderState.add("ContainedObjects", commanderContents);
		commanderState.add("Transform", NewDeckPosObject(2, 1, 2, true, 1.25));
		commanderState.add("CustomDeck", NewDeckStateObject(commanderStateDeckIDs, deck));
		if(commanderStateIDs.size()>0)objectStates.add(commanderState);
		
		//token and transform object state----------------------------------------
		JsonArray tokenStateIds = new JsonArray();
		ArrayList<Integer> tokenStateDeckIDs = new ArrayList<Integer>();
		for(Token token : deck.tokens){
			int deckID = token.jsonId/100;
			if(!tokenStateDeckIDs.contains(deckID)) tokenStateDeckIDs.add(deckID);
			tokenStateIds.add(new JsonPrimitive(token.jsonId));
		}
		for(Card card : deck.transformList){
			int deckID = card.transformJsonId/100;
			if(!tokenStateDeckIDs.contains(deckID)) tokenStateDeckIDs.add(deckID);
			tokenStateIds.add(new JsonPrimitive(card.transformJsonId));
		}

		JsonObject tokenState = NewDeckBaseObject(tokenStateIds, "Tokens");
		tokenState.add("Transform", NewDeckPosObject(0, 1, 0, true, 1.0));
		tokenState.add("CustomDeck", NewDeckStateObject(tokenStateDeckIDs, deck));
		if(tokenStateIds.size()>0)objectStates.add(tokenState);
		
		//main deck object state---------------------------------------------
		curStateIndex = 1;
		JsonArray mainStateIDs = new JsonArray();
		JsonArray mainContents = new JsonArray();
		ArrayList<Integer> mainStateDeckIDs = new ArrayList<Integer>();
		for(Card card : deck.cardList){
			if(card.amounts[curStateIndex] == 0)continue;
			int deckID = card.jsonId/100;
			if(!mainStateDeckIDs.contains(deckID)) mainStateDeckIDs.add(deckID);
			for(int i = 0; i < card.amounts[1]; i++){
				mainStateIDs.add(new JsonPrimitive(card.jsonId));
				mainContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
			}
		}
		JsonObject mainState = NewDeckBaseObject(mainStateIDs, deck.name);
		mainState.add("ContainedObjects", mainContents);
		mainState.add("Transform", NewDeckPosObject(1, 1, 0, false, 1.0));
		mainState.add("CustomDeck", NewDeckStateObject(mainStateDeckIDs, deck));
		if(mainStateIDs.size()>0)objectStates.add(mainState);
		
		//side board state ---------------------------------------------------
		curStateIndex = 2;
		JsonArray sideStateIDs = new JsonArray();
		JsonArray sideContents = new JsonArray();
		ArrayList<Integer> sideStateDeckIDs = new ArrayList<Integer>();
		for(Card card : deck.cardList){
			if(card.amounts[curStateIndex] == 0)continue;
			int deckID = card.jsonId/100;
			if(!sideStateDeckIDs.contains(deckID)){
				sideStateDeckIDs.add(deckID);
			}
			for(int i = 0; i < card.amounts[2]; i++){
				sideStateIDs.add(new JsonPrimitive(card.jsonId));
				sideContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
			}
		}
		for(Card card : deck.transformList){
			if(card.amounts[curStateIndex] == 0)continue;
			int deckID = card.jsonId/100;
			if(!sideStateDeckIDs.contains(deckID)){
				sideStateDeckIDs.add(deckID);
			}
			for(int i = 0; i < card.amounts[1]; i++){
				sideStateIDs.add(new JsonPrimitive(card.jsonId));
				sideContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
			}
		}
		JsonObject sideState = NewDeckBaseObject(sideStateIDs, "Sideboard");
		sideState.add("ContainedObjects", sideContents);
		sideState.add("Transform", NewDeckPosObject(2, 1, 0, false, 1.0));
		sideState.add("CustomDeck", NewDeckStateObject(sideStateDeckIDs, deck));
		if(sideStateIDs.size()>0)objectStates.add(sideState);
		
		//main obj ------------------------------------------------------------
		deckJSON.add("ObjectStates", objectStates);
		
		String deckStr = FrogUtils.gson.toJson(deckJSON);
		
		try{
			String deckName = Config.deckDir + deck.deckId + ".json";
			System.out.println("Saving deck to " + deckName);
			PrintWriter fileWriter = new PrintWriter(new File(deckName));
			fileWriter.write(deckStr);
			fileWriter.close();
		}catch(Exception e){
			System.out.println("Error saving deck json");
			e.printStackTrace();
		}
	}
	
	public static void BuildDraftJSONFile(DraftDeck draft){			
		JsonObject deckJSON = new JsonObject();
		
		String[] emptyProps = {"SaveName", "GameMode", "Date", "Table", "Sky", "Note", "Rules", "PlayerTurn"};
		for(String prop : emptyProps){
			deckJSON.add(prop, new JsonPrimitive(""));
		}

		JsonArray objectStates = new JsonArray();
		
		//token and transforms object state----------------------------------------
		JsonArray tokenStateIds = new JsonArray();
		ArrayList<Integer> tokenStateDeckIDs = new ArrayList<Integer>();
		for(Token token : draft.tokens){
			int deckID = token.jsonId/100;
			if(!tokenStateDeckIDs.contains(deckID)) tokenStateDeckIDs.add(deckID);
			tokenStateIds.add(new JsonPrimitive(token.jsonId));
		}
		for(Card card : draft.transformList){
			int deckID = card.transformJsonId/100;
			if(!tokenStateDeckIDs.contains(deckID)) tokenStateDeckIDs.add(deckID);
			tokenStateIds.add(new JsonPrimitive(card.transformJsonId));
		}

		JsonObject tokenState = NewDeckBaseObject(tokenStateIds, "Tokens");
		tokenState.add("Transform", NewDeckPosObject(-3, 1, -2, true, 1.0));
		tokenState.add("CustomDeck", NewDeckStateObject(tokenStateDeckIDs, draft));
		if(tokenStateIds.size()>0)objectStates.add(tokenState);
			
		//basics object state----------------------------------------
		JsonArray basicStateIds = new JsonArray();
		JsonArray basicContents = new JsonArray();
		ArrayList<Integer> basicStateDeckIDs = new ArrayList<Integer>();
		for(Card card : draft.basics){
			int deckID = card.jsonId/100;
			if(!basicStateDeckIDs.contains(deckID)){
				basicStateDeckIDs.add(deckID);
			}
			basicStateIds.add(new JsonPrimitive(card.jsonId));
			basicContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
		}

		JsonObject basicState = NewDeckBaseObject(basicStateIds, "Basic Lands");
		basicState.add("Transform", NewDeckPosObject(-3, 1, -3, true, 1.0));
		basicState.add("CustomDeck", NewDeckStateObject(basicStateDeckIDs, draft));
		basicState.add("ContainedObjects", basicContents);
		if(basicStateIds.size()>0)objectStates.add(basicState);
		
		//pack object states---------------------------------------------
		boolean hasMythic = draft.cardsByRarity.get(0).size() > 0;
		if(hasMythic){
			for(Card card :	draft.cardsByRarity.get(1)){
				draft.cardsByRarity.get(0).add(card);
				draft.cardsByRarity.get(0).add(card);
			}
		}
		for(int packi = 0; packi < draft.amountPacks; packi++){
			JsonArray packStateIds = new JsonArray();
			JsonArray packContents = new JsonArray();
			ArrayList<Integer> packStateDeckIds = new ArrayList<Integer>();
			HashSet<Integer> existingPackIds = new HashSet<Integer>();
			for(int rarityi = 0; rarityi < DraftDeck.RARITIES.length; rarityi++){
				if(hasMythic && rarityi == 1) continue;
				ArrayList<Card> byRarity = draft.cardsByRarity.get(rarityi);
				if(byRarity.size() == 0)continue;
				for(int cardi = 0; cardi < draft.boosterAmts[rarityi]; cardi++){
					Card card = null;
					do{
						card = byRarity.get((int)(byRarity.size() * Math.random()));
					}while(existingPackIds.contains(card.jsonId) && Math.random() > 0.25);
					existingPackIds.add(card.jsonId);
					int deckID = card.jsonId/100;
					if(!packStateDeckIds.contains(deckID)){
						packStateDeckIds.add(deckID);
					}
					packStateIds.add(new JsonPrimitive(card.jsonId));
					packContents.add(NewCardObject(card.jsonId, card.getDisplayName()));
				}
			}
			JsonObject packState = NewDeckBaseObject(packStateIds, "");
			packState.add("Transform", NewDeckPosObject(-2 + packi%6, 0, -3 + packi/6, false, 1.0));
			packState.add("CustomDeck", NewDeckStateObject(packStateDeckIds, draft));
			packState.add("ContainedObjects", packContents);
			objectStates.add(packState);
		}
		//main obj ------------------------------------------------------------
		deckJSON.add("ObjectStates", objectStates);
		
		String deckStr = FrogUtils.gson.toJson(deckJSON);
		
		try{
			String deckName = Config.deckDir + draft.deckId + ".json";
			System.out.println("Saving deck to " + deckName);
			PrintWriter fileWriter = new PrintWriter(new File(deckName));
			fileWriter.write(deckStr);
			fileWriter.close();
		}catch(Exception e){
			System.out.println("Error saving deck json");
			e.printStackTrace();
		}
	}
}
