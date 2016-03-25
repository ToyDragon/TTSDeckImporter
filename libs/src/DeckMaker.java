import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DeckMaker {

	
	public static boolean running;
	public static ArrayList<Card> TokenList = new ArrayList<Card>();
	public static ServerSocket serverSocket;
	public static boolean DEBUG = false;
	
	public static Pattern cardNameRegex = Pattern.compile("([0-9]*)x?\\s*([^<\\[{]*)");
	
	public static double DeckThread;
	
	public static void main(String[] args){
		FrogUtils.gson = new Gson();
		
		if(args.length > 0 && args[0].equals("debug")) DEBUG = true;
		if(!Config.LoadConfig()){
			ExitFailure("Error loading from config file");
		}
		
		Token.LoadTokenMap();
		Transform.LoadTransformMap();
		
		try {
			serverSocket = new ServerSocket(Config.port);
		} catch (Exception e) {
			ExitFailure("Unable to listen on port " + Config.port);
		}
		
		System.out.println("Initialization successful, waiting for client on port " + Config.port);
		
		Socket clientSocket = null;
		BufferedReader clientScanner = null;
		BufferedWriter clientWriter = null;
		running = true;
		while(running){
			try {
				clientSocket = serverSocket.accept();
				clientScanner = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
				clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
				try{
					HandleClient(clientScanner, clientWriter);
				}catch(Exception e){}
			} catch (Exception e) {
				System.out.println("Error in main thread loop :(");
				e.printStackTrace();
			} finally{
				if(clientScanner != null){try{clientScanner.close();}catch(Exception e) {}}
				if(clientWriter != null){try{clientWriter.close();}catch(Exception e) {}}
				if(clientSocket != null){try{clientSocket.close();}catch(Exception e) {}}
			}
		}
	}
	
	public static void Debug(String msg){
		if(DEBUG){
			System.out.println(msg);
		}
	}
	
	public static String ReadLine(BufferedReader reader){
		try{
			return reader.readLine();
		}catch(Exception e){
			Debug("Error reading from reader :(");
		}
		return null;
	}
	
	public static void ExitFailure(String message){
		System.out.println(message);
		System.exit(0);
	}
	
	public static void HandleClient(BufferedReader clientScanner, BufferedWriter clientWriter){
		String response;
		
		response = ReadLine(clientScanner);
		if(response.equals("deck")){
			HandleDeck(clientScanner, clientWriter);
		}else if(response.equals("draft")){
			HandleDraft(clientScanner);
		}else if(response.equals("ping")){
			try{
				clientWriter.write("true\r\n");
				clientWriter.flush();
			}catch(Exception e){}
		}
	}
	
	public static void HandleDeck(BufferedReader clientScanner, BufferedWriter clientWriter){
		Deck newDeck = new Deck();
		
		//load params
		newDeck.deckId = ReadLine(clientScanner);
		newDeck.name = ReadLine(clientScanner);
		newDeck.useImgur = ReadLine(clientScanner).equals("true");
		newDeck.backUrl = ReadLine(clientScanner);
		newDeck.hiddenUrl = ReadLine(clientScanner);
		boolean coolifyBasics = ReadLine(clientScanner).equals("true");
		try{
			newDeck.compressionLevel = Double.parseDouble(clientScanner.readLine());
		}catch(Exception e){}

		ReadDeckList(newDeck, clientScanner);
		if(newDeck.cardList.size() + newDeck.transformList.size() == 0){
			JsonObject errorObj = new JsonObject();
			errorObj.add("message", new JsonPrimitive("Too few cards!"));
			
			try {
				String badJson = FrogUtils.gson.toJson(errorObj);
				System.out.println("Bad: " + badJson);
				clientWriter.write(badJson);
				clientWriter.flush();
				return;
			} catch (Exception e) {e.printStackTrace();}
		}
		if(newDeck.cardList.size() + newDeck.transformList.size() >= 150){
			JsonObject errorObj = new JsonObject();
			errorObj.add("message", new JsonPrimitive("Too many different cards!"));
			
			try {
				String badJson = FrogUtils.gson.toJson(errorObj);
				System.out.println("Bad: " + badJson);
				clientWriter.write(badJson);
				clientWriter.flush();
				return;
			} catch (Exception e) {e.printStackTrace();}
		}
		
		if(coolifyBasics)newDeck.Coolify();

		ImageUtils.DownloadImages(newDeck);
		if(newDeck.unknownCards.size() == 0){
			ImageUtils.StitchDeck(newDeck);
			BuildJSONFile(newDeck);
		}else{
			JsonObject errorObj = new JsonObject();
			JsonArray badCardArray = new JsonArray();
			for(Card card : newDeck.unknownCards){
				badCardArray.add(new JsonPrimitive(card.line));
			}
			errorObj.add("badCards", badCardArray);
			errorObj.add("message", new JsonPrimitive(badCardArray.size() + " unrecognized cards!"));
			
			try {
				String badJson = FrogUtils.gson.toJson(errorObj);
				System.out.println("Bad: " + badJson);
				clientWriter.write(badJson);
				clientWriter.flush();
			} catch (Exception e) {e.printStackTrace();}
		}
		ImageUtils.FreeAllBuffers();
		System.out.println("Done with deck :O " + newDeck.name);
	}
	
	public static void ReadDeckList(Deck newDeck, BufferedReader clientScanner){
		String line;
		int cardSection = 1;
		String[] sectionLabels = {"COMMANDER","MAINBOARD","SIDEBOARD"};
		String[] prefixsToIgnore = {"CREATURE (", "INSTANT (", "LAND (", "PLANESWALKER (", "TCG $", "SIDEBOARD (", "ENCHANTMENT (", "SORCERY (", "MAYBEBOARD ("};
		String[] suffixsToIgnore = {" CREATURES", " INSTANTS and SORC.", " LANDS", " OTHER SPELLS"};
		
		cardInputLoop:
		while(true){
			line = ReadLine(clientScanner).trim();
			if(line.length() == 0)continue;
			if(line.equals("ENDDECK"))break;
			
			//check if line is a label for a new section
			for(int i = 0; i < sectionLabels.length; i++){
				if(line.equals(sectionLabels[i])){
					cardSection = i;
					continue cardInputLoop;
				}
			}
			
			for(int i = 0; i < prefixsToIgnore.length; i++){
				if(line.startsWith(prefixsToIgnore[i])) continue cardInputLoop;
			}
			for(int i = 0; i < suffixsToIgnore.length; i++){
				if(line.endsWith(suffixsToIgnore[i])) continue cardInputLoop;
			}
			
			ReadCard(newDeck, cardSection, line);			
		}
	}
	
	/**
	 * 
	 * @param newDeck
	 * @param board 0 - Commander, 1 - Maindeck, 2 - Sideboard
	 * @param line
	 */
	public static void ReadCard(Deck newDeck, int board, String line){
		String set = null, printing = null, language = null;
		String cardName = null;
		String multiverseId = null;
		int amt = 1;
		
		if(newDeck instanceof Draft){
			Draft draft = (Draft)newDeck;
			set = draft.code;

			multiverseId = line.split(":")[0];
			cardName = line.split(":")[1].trim().toLowerCase();
		}else{
			Matcher cardNameMatcher = cardNameRegex.matcher(line);
			cardNameMatcher.find();
			
			String rawAmt = cardNameMatcher.group(1);
			try{
				amt = Integer.parseInt(rawAmt);
			}catch(Exception e){}
			
			cardName = cardNameMatcher.group(2).trim().toLowerCase();
			
			int leftIndex,rightIndex;
			leftIndex = line.indexOf("<"); rightIndex = line.indexOf(">");
			if(leftIndex > 0 && rightIndex > leftIndex + 1) printing = line.substring(leftIndex + 1,rightIndex);
			
			leftIndex = line.indexOf("["); rightIndex = line.indexOf("]");
			if(leftIndex > 0 && rightIndex > leftIndex + 1) set = line.substring(leftIndex + 1,rightIndex);
			
			leftIndex = line.indexOf("{"); rightIndex = line.indexOf("}");
			if(leftIndex > 0 && rightIndex > leftIndex + 1) language = line.substring(leftIndex + 1,rightIndex);
		}
		

		for(String[] hardPair : Config.hardNameCharacters){
			cardName = cardName.replaceAll("\\Q"+hardPair[0]+"\\E", hardPair[1]);
		}
		if(cardName.contains("/")){//make double sided cards consistent with magiccards.info
			//Wear // Tear
			String leftHalf = cardName.substring(0, cardName.indexOf("/")).trim();
			String rightHalf = cardName.substring(cardName.lastIndexOf("/")+1).trim();
			
			cardName = leftHalf+=" ("+leftHalf+"/"+rightHalf+")";
		}
		
		String cardKey = Card.getCardKey(cardName, set, printing, language);
		Card card = newDeck.getCard(cardKey);
		if(card == null){
			card = new Card();
			card.name = cardName;
			card.cardKey = cardKey;
			card.language = language;
			card.printing = printing;
			card.multiverseId = multiverseId;
			card.set = set;
			card.line = line;

			newDeck.add(card);
		}
		
		card.amounts[board] += amt;
	}
	
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
	
	public static void HandleDraft(BufferedReader clientScanner){
		try{
			Debug("Draft...");
			Draft draft = new Draft();
			
			draft.deckId = ReadLine(clientScanner);
			draft.setName = ReadLine(clientScanner);
			try{
				draft.amountPacks = Integer.parseInt(ReadLine(clientScanner));
			}catch(Exception e){draft.amountPacks = 18;}
			Debug("Reading list...");
			ReadDraftList(clientScanner, draft);
			if(!ImageUtils.CheckDraftAssets(draft)){
				Debug("Downloading images...");
				ImageUtils.DownloadImages(draft);
			}
			Debug("Stitching deck...");
			ImageUtils.StitchDeck(draft);
			Debug("Building JSON...");
			BuildDraftJSONFile(draft);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			ImageUtils.FreeAllBuffers();
			Debug("Done with draft :O");
		}
	}
	

	public static void BuildDraftJSONFile(Draft draft){			
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
			for(int rarityi = 0; rarityi < Draft.RARITIES.length; rarityi++){
				if(hasMythic && rarityi == 1) continue;
				ArrayList<Card> byRarity = draft.cardsByRarity.get(rarityi);
				if(byRarity.size() == 0)continue;
				for(int cardi = 0; cardi < draft.boosterAmts[rarityi]; cardi++){
					Card card = byRarity.get((int)(byRarity.size() * Math.random()));
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
	
	public static void ReadDraftList(BufferedReader clientScanner, Draft draft){
		BufferedReader fileReader = null;
		try{
			fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.setAssetDir + draft.setName)), StandardCharsets.UTF_8));
		}catch(Exception e){ e.printStackTrace();return; }

		final int CARD=1,CODE=2,BOOSTER=3,CRAP=4;

		int mode = 0;
		for(String line = ReadLine(fileReader); line != null; line = ReadLine(fileReader)){
			boolean isControl = false;
			for(int i = 0; i < Draft.RARITIES.length; i++){
				if(line.toLowerCase().equals(Draft.RARITIES[i].toLowerCase())){
					mode = CARD;
					draft.curRarity = i;
					isControl = true;
				}
			}
			if(line.equalsIgnoreCase("Code")){
				mode = CODE;
				isControl = true;
			}
			if(line.equalsIgnoreCase("Booster")){
				mode = BOOSTER;
				isControl = true;
			}
			if(line.equalsIgnoreCase("Gatherer Code")){
				mode = CRAP;
				isControl = true;
			}
			if(isControl) continue;
			
			if(mode == CODE){
				draft.code = line;
				System.out.println("Loaded code of " + draft.code);
			}
			if(mode == BOOSTER){
				String[] rarities = line.split(":")[0].split(",");
				int amt = Integer.parseInt(line.split(":")[1]);
				for(int j = 0; j < rarities.length; j++){
					for(int i = 0; i < Draft.RARITIES.length; i++){
						if(rarities[j].toLowerCase().startsWith(Draft.RARITIES[i].toLowerCase())){
							draft.boosterAmts[i] = amt;
						}
					}
				}
			}
			if(mode == CARD && !Draft.RARITIES[draft.curRarity].equals("Basic Land")){
				ReadCard(draft, 1, line);
			}
		}
		String[] basics = {"island","mountain","forest","plains","swamp"};
		for(String name : basics){
			Card card = new Card();
			card.name = name;
			card.set = "uh";
			card.cardKey = Card.getCardKey(name, "uh", null, null);
			
			draft.add(card);
			draft.basics.add(card);
		}
		Card wastes = new Card();
		wastes.name = "wastes";
		wastes.set = "ogw";
		wastes.cardKey = Card.getCardKey(wastes.name, wastes.set, null, null);
		draft.add(wastes);
		draft.basics.add(wastes);
	}
}
