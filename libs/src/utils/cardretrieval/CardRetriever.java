package utils.cardretrieval;


import java.util.HashSet;

import core.Card;

public abstract class CardRetriever {
	public HashSet<String> failedCards;

	public CardRetriever(){
		failedCards = new HashSet<String>();
	}
	
	public void ClearFailedCards(){
		failedCards.clear();
	}
	
	public void LoadFailed(Card card){
		failedCards.add(card.cardKey);
	}
	
	public boolean HasCardFailed(Card card){
		return failedCards.contains(card.cardKey);
	}
	
	public abstract boolean LoadCard(Card card); 
}
