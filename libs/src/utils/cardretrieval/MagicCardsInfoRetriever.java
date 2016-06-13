package utils.cardretrieval;

import java.io.File;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.Card;
import core.Config;
import utils.FrogUtils;
import utils.ImageUtils;

public class MagicCardsInfoRetriever extends CardRetriever{

	@Override
	public boolean LoadCard(Card card) {
		boolean success = true;
		success = LoadCard(card, false) && success;
		if(card.transformName != null) success = LoadCard(card, true) && success;
		return success;
	}
	
	public boolean LoadCard(Card card, boolean isBack){
		String cardKey = isBack ? card.transformCardKey : card.cardKey;
		String cardName = isBack ? card.transformName : card.name;
		String imageFileName = Config.imageDir+cardKey.toLowerCase().replaceAll("[<>/\"]+", "_")+".jpg";
		
		if(isBack) card.transformImageFileName = imageFileName;
		else card.imageFileName = imageFileName;

		if(new File(imageFileName).exists()) return true;
		if(HandleHardCard(cardName, imageFileName)) return true;
		
		String result = RequestCard(card, cardName);
		
		if(card.printing != null && !card.printing.trim().equals("")){
			String lang = card.language;
			String set = card.set;
			if(lang == null || lang.equals("")){
				lang = "en";
			}
			if(set == null){
				set = FindSet(card.language, card.printing, result);
			}
			String url = "http://magiccards.info/scans/"+lang+"/"+set+"/"+card.printing+".jpg";
			ImageUtils.SaveImage(url, imageFileName, 1.0);
			return true;
		}else{
			String regexStr = "<img src=\"(http://magiccards.info/[a-z0-9/]+\\.jpg)\"\\s+alt=\"" + Pattern.quote(cardName) +"\"";
			Pattern regex = Pattern.compile(regexStr);
			
			Matcher matcher = regex.matcher(result);
			if(matcher.find()){
				ImageUtils.SaveImage(matcher.group(1), imageFileName, 1.0);
				return true;
			}
			
			regexStr = "<a href=\"/([a-z0-9]+)/([a-z0-9]+)/([a-z0-9]+)\\.html\">" + Pattern.quote(cardName);
			regex = Pattern.compile(regexStr);
			
			matcher = regex.matcher(result);
			if(matcher.find()){
				ImageUtils.SaveImage("http://magiccards.info/scans/" + matcher.group(2) + "/" + matcher.group(1) + "/" + matcher.group(3) + ".jpg", imageFileName, 1.0);
				return true;
			}
			
			System.out.println(result);
		}
		return false;
	}
	
	public static String FindSet(String language, String printing, String html){
		String set = "";
		String regexStr = "(?i)/(..?.?.?.?)/"+language+"/"+printing+".html";
		Pattern regex = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);

		try{
			Matcher matcher = regex.matcher(html);
			matcher.find();
			if(matcher.groupCount() > 0){
				set = matcher.group(1);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return set;
	}
	
	public static String RequestCard(Card card, String cardName){
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
		
		return FrogUtils.ReplaceHardChars(result);
	}
	
	public static boolean HandleHardCard(String cardName, String imageFileName){
		for(String[] hardPair : Config.hardUrls){
			if(hardPair[0].equalsIgnoreCase(cardName)){
				if(!new File(hardPair[1]).exists()) ImageUtils.SaveImage(hardPair[1], imageFileName, 1.0);
				return true;
			}
		}
		return false;
	}
}
