package utils.cardretrieval;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.Card;
import core.Config;
import utils.FrogUtils;
import utils.ImageUtils;

public class MythicSpoilerRetriever extends CardRetriever{
	
	public static String mythicSpoilerPage;

	@Override
	public boolean LoadCard(Card card) {
		boolean success = true;
		success = LoadCard(card, false) && success;
		if(card.transformName != null)success = LoadCard(card, true) && success;
		return success;
	}

	public boolean LoadCard(Card card, boolean isBack){
		String cname = isBack ? card.transformName : card.name;
		String imgname = Config.imageDir + "MYTHICSPOILER" + cname + ".jpg";
		
		if(isBack)card.transformImageFileName = imgname;
		else card.imageFileName = imgname;
		
		if(new File(imgname).exists()){return true;}
		
		String mythicSet = card.set;
		if(mythicSet == null || mythicSet.length() == 0) mythicSet = "emn"; //TODO move latest set into config file or something, or read it from the page somehow		
		
		String urlname = cname.toLowerCase().replaceAll("[^a-z]", "");
		String url = "http://www.mythicspoiler.com/"+mythicSet+"/cards/"+urlname+".jpg";
		if(ImageUtils.SaveImage(url, imgname, 1.0)){
			return true;
		}else{
			System.out.println("Couldn't load from " + url);
		}
		
		if(mythicSpoilerPage == null){
			mythicSpoilerPage = FrogUtils.GetHTML("http://www.mythicspoiler.com/");
		}String processedName = cname.replaceAll("[^a-zA-Z]","").toLowerCase();
		
		for(String[] pair : Config.mythicErrors){
			if(processedName.equalsIgnoreCase(pair[0])){
				processedName = pair[1];
			}
		}
		
		String patternString = "\"(..?.?.?/(?:cards/)?"+processedName+"[0-9]?.jpg)";
		Pattern regex = Pattern.compile(patternString);
		
		Matcher matcher = regex.matcher(mythicSpoilerPage);
		if(matcher.find()){			
			url = "http://www.mythicspoiler.com/"+matcher.group(1);
			ImageUtils.SaveImage(url, imgname, 1.0);
			return true;
		}
		System.out.println("Failed at " + imgname);
		return false;
	}
}
