package core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cardbuddies.Token;
import cardbuddies.Transform;
import utils.FrogUtils;
import utils.ImageUtils;
import utils.JsonUtils;

public class DeckMaker {

	
	public static boolean running;
	public static ArrayList<Card> TokenList = new ArrayList<Card>();
	public static ServerSocket serverSocket;
	
	public static Pattern cardNameRegex = Pattern.compile("([0-9]*)x?\\s*([^<\\[{]*)");
	
	public static double DeckThread;
	
	public static void main(String[] args){
		FrogUtils.gson = new Gson();
		
		if(args.length > 0 && args[0].equals("debug")) FrogUtils.DEBUG = true;
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
				}catch(Exception e){e.printStackTrace();}
			} catch (Exception e) {
				System.out.println("Exception in main thread loop :(");
				e.printStackTrace();
			} finally{
				//if(clientScanner != null){try{clientScanner.close();}catch(Exception e) {e.printStackTrace();}}
				//if(clientWriter != null){try{clientWriter.close();}catch(Exception e) {e.printStackTrace();}}
				if(clientSocket != null){try{clientSocket.close();}catch(Exception e) {e.printStackTrace();}}
			}
		}
	}
	
	public static String ReadLine(BufferedReader reader){
		try{
			return reader.readLine();
		}catch(Exception e){}
		return "";
	}
	
	public static void ExitFailure(String message){
		System.out.println("Exit failure: " + message);
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
		
		newDeck.deckId = ReadLine(clientScanner);
		newDeck.name = ReadLine(clientScanner);
		newDeck.backUrl = ReadLine(clientScanner);
		newDeck.hiddenUrl = ReadLine(clientScanner);
		boolean coolifyBasics = ReadLine(clientScanner).equals("true");
		try{
			newDeck.compressionLevel = Double.parseDouble(clientScanner.readLine());
		}catch(Exception e){}

		ReadDeckList(newDeck, clientScanner);
		FrogUtils.Debug("Deck with " + newDeck.cardList.size() + " cards and " + newDeck.transformList.size() + " transforms");
		
		String errorMessage = null;
		JsonArray badCardArray = null;
		if(newDeck.cardList.size() + newDeck.transformList.size() == 0){ errorMessage = "Too few cards!"; }
		if(newDeck.cardList.size() + newDeck.transformList.size() >= 150){ errorMessage = "Too many different cards!"; }
		if(errorMessage == null){
			if(coolifyBasics)newDeck.Coolify();
	
			ImageUtils.DownloadImages(newDeck);
			FrogUtils.Debug("Downloaded images, with " + newDeck.unknownCards.size() + " unknown");
			if(newDeck.unknownCards.size() == 0){
				ImageUtils.StitchDeck(newDeck);
				JsonUtils.BuildJSONFile(newDeck);
			}else{
				badCardArray = new JsonArray();
				for(Card card : newDeck.unknownCards){
					badCardArray.add(new JsonPrimitive(card.line));
				}
				errorMessage = badCardArray.size() + " unrecognized cards!";
			}
		}
		
		if(errorMessage != null){
			JsonObject errorObj = new JsonObject();
			errorObj.add("message", new JsonPrimitive(errorMessage));
			if(badCardArray != null)errorObj.add("badCards", badCardArray);
			try {
				String badJson = FrogUtils.gson.toJson(errorObj);
				FrogUtils.Debug("Bad: " + badJson);
				clientWriter.write(badJson);
				clientWriter.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try{
			clientScanner.readLine(); //DECKEND
		}catch(Exception e){}
		
		ImageUtils.FreeAllBuffers();
		System.out.println("Done with deck " + newDeck.name);
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
	
	public static void ReadCard(Deck newDeck, int board, String line){
		try{
			String set = null, printing = null, language = null;
			String cardName = null;
			String multiverseId = null;
			int amt = 1;
			
			if(newDeck instanceof DraftDeck){
				DraftDeck draft = (DraftDeck)newDeck;
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

				printing = FrogUtils.StringBetween(line, "<", ">");
				set = FrogUtils.StringBetween(line, "[", "]");
				language = FrogUtils.StringBetween(line, "{", "}");
			}
			
			cardName = FrogUtils.CleanCardName(cardName);
			
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
		}catch(Exception e){
			FrogUtils.Debug("Unable to read line:" + line);
			e.printStackTrace();
		}
	}
	
	public static void HandleDraft(BufferedReader clientScanner){
		try{
			DraftDeck draft = new DraftDeck();
			
			draft.deckId = ReadLine(clientScanner);
			draft.setName = ReadLine(clientScanner);
			try{
				draft.amountPacks = Integer.parseInt(ReadLine(clientScanner));
			}catch(Exception e){draft.amountPacks = 18;}
			ReadDraftList(clientScanner, draft);

			System.out.println("There are " + draft.cardsByRarity.get(0).size() + " mythics");
			for(Card c : draft.cardsByRarity.get(0)){
				System.out.println(c.getDisplayName());
			}
			
			if(!ImageUtils.CheckDraftAssets(draft)){
				FrogUtils.Debug("Downloading draft images...");
				ImageUtils.DownloadImages(draft);
			}
			FrogUtils.Debug("Stitching draft deck...");
			ImageUtils.StitchDeck(draft);
			FrogUtils.Debug("Building draft JSON...");
			JsonUtils.BuildDraftJSONFile(draft);
		}catch(Exception e){
			e.printStackTrace();
		}
		ImageUtils.FreeAllBuffers();
		System.out.println("Done with draft");
	}
	
	public static void ReadDraftList(BufferedReader clientScanner, DraftDeck draft){
		BufferedReader fileReader = null;
		try{
			fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.setAssetDir + draft.setName)), StandardCharsets.UTF_8));
		}catch(Exception e){ e.printStackTrace();return; }

		final int CARD=1,CODE=2,BOOSTER=3,CRAP=4;

		int mode = 0;
		for(String line = ReadLine(fileReader); line != null; line = ReadLine(fileReader)){
			boolean isControl = false;
			for(int i = 0; i < DraftDeck.RARITIES.length; i++){
				if(line.toLowerCase().equals(DraftDeck.RARITIES[i].toLowerCase())){
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
					for(int i = 0; i < DraftDeck.RARITIES.length; i++){
						if(rarities[j].toLowerCase().startsWith(DraftDeck.RARITIES[i].toLowerCase())){
							draft.boosterAmts[i] = amt;
						}
					}
				}
			}
			if(mode == CARD && !DraftDeck.RARITIES[draft.curRarity].equals("Basic Land")){
				ReadCard(draft, 1, line);
			}
		}
		draft.curRarity = 4; //basic land
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
