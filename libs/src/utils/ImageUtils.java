package utils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import cardbuddies.Token;
import core.Card;
import core.Config;
import core.Deck;
import core.DraftDeck;
import utils.cardretrieval.CardRetriever;
import utils.cardretrieval.GathererRetriever;
import utils.cardretrieval.MagicCardsInfoRetriever;
import utils.cardretrieval.MythicSpoilerRetriever;

public class ImageUtils {
	public static ArrayList<BufferedImage> freeBuffers = new ArrayList<BufferedImage>();
	public static ArrayList<BufferedImage> occupiedBuffers = new ArrayList<BufferedImage>();
	public static ArrayList<CardRetriever> cardRetrievers = new ArrayList<CardRetriever>();
	
	public static long resetTime = 0;
	
	static{
		cardRetrievers.add(new MagicCardsInfoRetriever());
		cardRetrievers.add(new MythicSpoilerRetriever());
		cardRetrievers.add(new GathererRetriever());
	}
	
	public static BufferedImage GetBuffer(int width, int height){
		for(int i = freeBuffers.size()-1; i >= 0; i--){
			BufferedImage img = freeBuffers.get(i);
			if(img.getWidth() == width && img.getHeight() == height){
				freeBuffers.remove(i);
				occupiedBuffers.add(img);
				return img;
			}
		}
		
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		occupiedBuffers.add(img);
		return img;
	}
	
	public static void FreeAllBuffers(){
		for(int i = occupiedBuffers.size()-1; i >= 0; i--){
			freeBuffers.add(occupiedBuffers.remove(i));
		}
	}
	
	public static void AttemptClearFailedCards(){
		long curTime = System.currentTimeMillis();
		if(curTime >= resetTime){
			for(CardRetriever retriever : cardRetrievers){
				retriever.ClearFailedCards();
			}
			
			Calendar tomorrow = Calendar.getInstance();
			tomorrow.set(Calendar.HOUR_OF_DAY, 0);
			tomorrow.set(Calendar.MINUTE, 0);
			tomorrow.set(Calendar.SECOND, 0);
			tomorrow.add(Calendar.DAY_OF_MONTH, 1);
			
			resetTime = tomorrow.getTimeInMillis();
		}
	}
	
	public static void DownloadImages(Deck deck){
		AttemptClearFailedCards();

		for(int i = deck.cardList.size()-1; i >= 0; i--){
			Card card = deck.cardList.get(i);
			boolean success = false;
			for(CardRetriever retriever : cardRetrievers){
				if(!retriever.HasCardFailed(card)){
					success = retriever.LoadCard(card);
					if(success) break;
				}
			}
			
			if(!success){
				deck.unknownCards.add(card);
				deck.cardList.remove(i);
			}
		}

		for(int i = deck.transformList.size()-1; i >= 0; i--){
			Card card = deck.transformList.get(i);
			boolean success = false;
			for(CardRetriever retriever : cardRetrievers){
				if(!retriever.HasCardFailed(card)){
					success = retriever.LoadCard(card);
					if(success) break;
				}
			}
			
			if(!success){
				deck.cardList.remove(card);
			}
		}
	
		if(deck.hiddenUrl != null && !deck.hiddenUrl.equals("default")){
			deck.hiddenImage = ImageFromUrl(deck.hiddenUrl);
		}
	}
	
	public static BufferedImage ImageFromUrl(String rawurl){
		try{
			URL url = new URL(rawurl);
			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection(); 
			httpcon.addRequestProperty("User-Agent", Config.userAgent); 
			return ImageIO.read(httpcon.getInputStream());
		}catch(Exception e){}
		return null;
	}
	
	public static boolean SaveImage(String rawurl, String destFile, double compressionLevel){
		BufferedImage source = null;
		try{
			source = ImageFromUrl(rawurl);
			if(source != null){
				SaveImage(source, destFile, compressionLevel);
				return true;
			}
		}catch(Exception e){}
		return false;
	}
	
	public static void SaveImage(BufferedImage source, String dest, double compressionLevel){
		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality((float)compressionLevel);

		try {
			jpgWriter.setOutput(new FileImageOutputStream(new File(dest)));
			IIOImage outputImage = new IIOImage(source, null, null);
			jpgWriter.write(null, outputImage, jpgWriteParam);
		} catch (Exception e) {
			e.printStackTrace();
		}
		jpgWriter.dispose();
	}
	
	public static boolean CheckDraftAssets(DraftDeck draft){
		boolean draftAssetsExist = true;
		
		int cardsPerDeck = 69;
		
		int regularDecks = (int) Math.ceil((draft.cardList.size() + draft.tokens.size())/(double)cardsPerDeck);
		int transformDecks = 2 * (int) Math.ceil(draft.transformList.size()/(double)cardsPerDeck);
		int deckAmt = regularDecks + transformDecks;
		
		draft.deckFileNames = new String[deckAmt];
		
		for(int i = 0; i < deckAmt; i++){
			String cleanSetName = draft.setName.replaceAll("\\s", "_");
			draft.deckFileNames[i] = Config.setAssetDir + cleanSetName + i + ".jpg";
			draftAssetsExist = draftAssetsExist && new File(draft.deckFileNames[i]).exists();
			if(!draftAssetsExist){
				System.out.println(draft.deckFileNames[i] + " does not exist");
			}
		}
		
		return draftAssetsExist;
	}
	
	public static void StitchDeck(Deck deck){
		int cardsPerDeck = 69;
		
		int regularDecks = (int) Math.ceil((deck.cardList.size() + deck.tokens.size())/(double)cardsPerDeck);
		int transformDecks = 2 * (int) Math.ceil(deck.transformList.size()/(double)cardsPerDeck);
		int deckAmt = regularDecks + transformDecks;
		
		boolean draftAssetsExist = deck instanceof DraftDeck;
		
		int cardOffsetX = 10;
		int cardOffsetY = 10;
		
		int cardWidth = 312 + 2*cardOffsetX;
		int cardHeight = 445 + 2*cardOffsetY;
		
		deck.buffers = new BufferedImage[deckAmt];
		deck.deckFileNames = new String[deckAmt];
		deck.deckLinks = new String[deckAmt];
		
		Graphics[] gs = new Graphics[deckAmt];
		
		for(int i = 0; i < deckAmt; i++){
			deck.deckFileNames[i] = Config.deckDir + deck.deckId + i + ".jpg";
			deck.deckLinks[i] = Config.hostUrlPrefix + Config.publicDeckDir + deck.deckId + i + ".jpg";
			
			if(deck instanceof DraftDeck){
				DraftDeck draft = (DraftDeck)deck;
				String cleanSetName = draft.setName.replaceAll("\\s", "_");
				deck.deckFileNames[i] = Config.setAssetDir + cleanSetName + i + ".jpg";
				deck.deckLinks[i] = Config.hostUrlPrefix + Config.publicSetAssetDir + cleanSetName + i + ".jpg";
				draftAssetsExist = draftAssetsExist && new File(deck.deckFileNames[i]).exists();
			}
			
			deck.buffers[i] = GetBuffer(cardWidth * 10, cardHeight * 7);
			gs[i] = deck.buffers[i].getGraphics();
			gs[i].setColor(Color.BLACK);
			gs[i].fillRect(0, 0, cardWidth * 10, cardHeight * 7);
			if(deck.hiddenImage != null){
				gs[i].drawImage(deck.hiddenImage, cardWidth * 9, cardHeight * 6, cardWidth, cardHeight, null);
			}
		}
		
		int cardCount = 0;
		for(Card card : deck.cardList){			
			int deckNum = cardCount / cardsPerDeck;
			int deckID = cardCount % cardsPerDeck;
			card.jsonId = 100*(1+deckNum) + deckID;
			if(!draftAssetsExist){
				int gridX = deckID%10;
				int gridY = deckID/10;
	
				int realX = gridX * cardWidth + cardOffsetX;
				int realY = gridY * cardHeight + cardOffsetY;
				try{
					BufferedImage cardImage = ImageIO.read(new File(card.imageFileName));
					gs[deckNum].drawImage(cardImage, realX, realY, cardWidth - cardOffsetX*2, cardHeight - cardOffsetY*2, null);
				}catch(Exception e){
					System.out.println("Error loading from file " + card.imageFileName);
					e.printStackTrace();
				}
			}
			cardCount++;
		}

		for(Token token : deck.tokens){			
			int deckNum = cardCount / cardsPerDeck;
			int deckID = cardCount % cardsPerDeck;
			token.jsonId = 100*(1+deckNum) + deckID;
			if(!draftAssetsExist){
				int gridX = deckID%10;
				int gridY = deckID/10;
	
				int realX = gridX * cardWidth + cardOffsetX;
				int realY = gridY * cardHeight + cardOffsetY;
				try{
					gs[deckNum].drawImage(token.image, realX, realY, cardWidth - cardOffsetX*2, cardHeight - cardOffsetY*2, null);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			cardCount++;
		}

		if(cardCount%cardsPerDeck!=0){
			cardCount += cardsPerDeck - (cardCount%cardsPerDeck);
		}
		for(Card card : deck.transformList){			
			int deckNum = cardCount / cardsPerDeck;
			int deckID = cardCount % cardsPerDeck;
			card.transformJsonId = 100*(1+deckNum) + deckID;
			
			if(!draftAssetsExist){
				int gridX = deckID%10;
				int gridY = deckID/10;
	
				int realX = gridX * cardWidth + cardOffsetX;
				int realY = gridY * cardHeight + cardOffsetY;
				try{
					BufferedImage cardImage = ImageIO.read(new File(card.imageFileName));
					gs[deckNum].drawImage(cardImage, realX, realY, cardWidth - cardOffsetX*2, cardHeight - cardOffsetY*2, null);
					cardImage = ImageIO.read(new File(card.transformImageFileName));
					gs[deckNum+1].drawImage(cardImage, realX, realY, cardWidth - cardOffsetX*2, cardHeight - cardOffsetY*2, null);
				}catch(Exception e){
					System.out.println("Err with " + card.imageFileName);
					System.out.println("and also " + card.transformImageFileName);
					e.printStackTrace();
				}
			}
			cardCount++;
			if(cardCount%cardsPerDeck==0)cardCount+=cardsPerDeck;
		}
		
		for(int i = 0; i < deckAmt; i++){
			gs[i].dispose();
			if(!draftAssetsExist){
				SaveImage(deck.buffers[i], deck.deckFileNames[i], deck.compressionLevel);
			}
		}
	}
}
