import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

public class ImageUtils {
	public static ArrayList<BufferedImage> freeBuffers = new ArrayList<BufferedImage>();
	public static ArrayList<BufferedImage> occupiedBuffers = new ArrayList<BufferedImage>();
	public static String mythicSpoilerPage;
	
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
	
	public static void DownloadImages(Deck deck){
		
		for(int i = deck.cardList.size()-1; i >= 0; i--){
			Card card = deck.cardList.get(i);
			if(Transform.nameToTransformMap.containsKey(card.name)) continue;
			if(LoadFromMagicCards(card) != null) continue;
			if(LoadFromMythicSpoiler(card)) continue;
			if(LoadFromGatherer(card)) continue;
			
			System.out.println("Failed to load "+card);
			deck.unknownCards.add(card);
			deck.cardList.remove(i);
		}

		for(int i = deck.transformList.size()-1; i >= 0; i--){
			Card card = deck.transformList.get(i);
			card.transformCardKey = Card.getCardKey(card.transformName, card.set, card.printing, card.language);
			card.imageFileName = LoadFromMagicCards(card);
			card.transformImageFileName = LoadFromMagicCards(card, true);
			
			if(card.imageFileName != null && card.transformImageFileName != null) continue;
			
			System.out.println("Failed to load " + card.imageFileName + " or " + card.transformImageFileName);
			deck.cardList.remove(i);
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
	
	public static void SaveImage(String rawurl, String destFile, double compressionLevel){
		SaveImage(ImageFromUrl(rawurl), destFile, compressionLevel);
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
	
	public static String LoadFromMagicCards(Card card){
		String imageFileName = LoadFromMagicCards(card, false);
		card.imageFileName = imageFileName;
		return imageFileName;
	}
	
	public static String LoadFromMagicCards(Card card, boolean isBack){
		String cardKey = isBack ? card.transformCardKey : card.cardKey;
		String cardName = isBack ? card.transformName : card.name;
		String imageFileName = Config.imageDir+cardKey.toLowerCase().replaceAll("[<>/\"]+", "_")+".jpg";
		if(new File(imageFileName).exists()) return imageFileName;
		
		for(String[] hardPair : Config.hardUrls){
			if(hardPair[0].equalsIgnoreCase(cardName)){
				SaveImage(hardPair[1], imageFileName, 1.0);
				return imageFileName;
			}
		}
		String processedName = "\""+cardName.trim().toLowerCase()+"\"";
		if(card.set != null && card.set.length() > 0) processedName +=" e:"+card.set;
		if(card.language != null && card.language.length() > 0)	processedName +=" l:"+card.language;

		URI uri = null;
		String qstr = null;
		try{
			uri = new java.net.URI("http", "magiccards.info", "/query", "q="+processedName, null);
			qstr = uri.toURL().toString().replaceAll("&", "%26").replaceAll(" ", "%20")+"&v=card&s=cname";
		}catch(Exception e){}
		
		String result = FrogUtils.GetHTML(qstr).toLowerCase();

		for(String[] hardPair : Config.hardNameCharacters){
			result = result.replaceAll("\\Q"+hardPair[0]+"\\E", hardPair[1]);
		}
		
		String regexStr = "";
		if(card.printing != null && !card.printing.trim().equals("")){
			String lang = card.language;
			String set = card.set;
			if(lang == null || lang.equals("")){
				lang = "en";
			}
			if(set == null){
				regexStr = "(?i)/(..?.?.?.?)/"+lang+"/"+card.printing+".html";//find set and lang
				Pattern regex = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);

				try{
					Matcher matcher = regex.matcher(result);
					matcher.find();
					if(matcher.groupCount() > 0){
						set = matcher.group(1);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			String url = "http://magiccards.info/scans/"+lang+"/"+set+"/"+card.printing+".jpg";
			SaveImage(url, imageFileName, 1.0);
			return imageFileName;
		}else{
			String cleanName = Pattern.quote(cardName);
			regexStr = "<img src=\"(http://magiccards.info/[a-z0-9/]+\\.jpg)\"\\s+alt=\"" + cleanName +"\"";
			Pattern regex = Pattern.compile(regexStr);
			
			Matcher matcher = regex.matcher(result);
			if(matcher.find()){
				SaveImage(matcher.group(1), imageFileName, 1.0);
				return imageFileName;
			}
		}
		return null;
	}
	
	public static boolean LoadFromGatherer(Card card){
		if(card.multiverseId != null){
			card.imageFileName = Config.imageDir + card.multiverseId+".jpg";
			File f = new File(card.imageFileName);
			if(!f.exists()){
				String imageURL = "http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid="+card.multiverseId+"&type=card";
				SaveImage(imageURL, card.imageFileName, 1.0);
			}
			return true;
		}
		return false;
	}
	
	public static boolean LoadFromMythicSpoiler(Card card){
		card.imageFileName = Config.imageDir + "MYTHICSPOILER" + card.name + ".jpg";
		if(new File(card.imageFileName).exists()){return true;}
		if(mythicSpoilerPage == null){
			mythicSpoilerPage = FrogUtils.GetHTML("http://www.mythicspoiler.com/");
		}
		
		String processedName = card.name.replaceAll("[^a-zA-Z]","").toLowerCase();
		
		for(String[] pair : Config.mythicErrors){
			if(processedName.equalsIgnoreCase(pair[0])){
				processedName = pair[1];
			}
		}
		
		String patternString = "\"(..?.?.?/(?:cards/)?"+processedName+"[0-9]?.jpg)";
		Pattern regex = Pattern.compile(patternString);
		
		Matcher matcher = regex.matcher(mythicSpoilerPage);
		if(matcher.find()){			
			String url = "http://www.mythicspoiler.com/"+matcher.group(1);
			SaveImage(url, card.imageFileName, 1.0);
			return true;
		}
		
		return false;
	}
	
	public static void StitchDeck(Deck deck){
		int cardsPerDeck = 69;
		
		int regularDecks = (int) Math.ceil((deck.cardList.size() + deck.tokens.size())/(double)cardsPerDeck);
		int transformDecks = 2 * (int) Math.ceil(deck.transformList.size()/(double)cardsPerDeck);
		int deckAmt = regularDecks + transformDecks;
		
		boolean draftAssetsExist = deck instanceof Draft;
		
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
			
			if(deck instanceof Draft){
				Draft draft = (Draft)deck;
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
