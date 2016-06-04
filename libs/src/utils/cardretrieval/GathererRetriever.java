package utils.cardretrieval;

import java.io.File;

import core.Card;
import core.Config;
import utils.ImageUtils;

public class GathererRetriever extends CardRetriever {

	@Override
	public boolean LoadCard(Card card) {
		if(card.multiverseId != null){
			card.imageFileName = Config.imageDir + card.multiverseId+".jpg";
			File f = new File(card.imageFileName);
			if(!f.exists()){
				String imageURL = "http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid="+card.multiverseId+"&type=card";
				ImageUtils.SaveImage(imageURL, card.imageFileName, 1.0);
			}
			return true;
		}
		return false;
	}

}
