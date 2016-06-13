var frogtown = require('../frogtown.js');
var config = require('../settings.json');
var fs = require('fs');

/*
expectedOutput = {
	success: true,
	successData: {
		amtDecks: 3,
		deckDescriptions: [ //decks may fit any description to be considered valid
			{
				amt: 20,
				names: {
					"Lightning Bolt": 1, //1 is not the amount, just a truthy value
					"Mountain": 1,
					...
					"allowAllNames": true //if you require that names exist, but don't care what they are
				}
			}, ...
		]
	}	
}
*/

exports.Test = function(input,expectedOutput,cb){
	var error = function(reqObj, message, errObj){
		if(expectedOutput.success){
			console.log('Error message: ' + message);
			console.log('Error: ' + JSON.stringify(errObj || {}));
		}
		cb(!expectedOutput.success);
	};
	var success = function(reqObj, deckId){
		var result = expectedOutput.success;
		if(!result)console.log('Succeed when expected to fail');
		if(result){
			var dataObj = JSON.parse(fs.readFileSync('../' + config.deckDir + deckId + '.json'));
			var amtDecks = dataObj.ObjectStates.length;
			result = amtDecks == expectedOutput.successData.amtDecks; //Validate the correct number of decks
			if(!result)console.log('Wrong amount of decks');
			for(var decki = 0;decki < amtDecks && result; decki++){
				var deckObj = dataObj.ObjectStates[decki];
				deckObj.DeckIDs = deckObj.DeckIDs || [deckObj.CardID];
				var deckMatchFound = false;
				if(deckObj.ContainedObjects){
					result = result && deckObj.ContainedObjects.length == deckObj.DeckIDs.length;//Validate that the number of nicknames = number of cards
					if(!result)console.log('Wrong amount of card names');
				}
				for(var i = 0; i < expectedOutput.successData.deckDescriptions.length && result && !deckMatchFound; i++){
					deckDescription = expectedOutput.successData.deckDescriptions[i];
					deckMatchFound = deckObj.DeckIDs.length == deckDescription.amt;//Validate the card count is right
					if(deckObj.ContainedObjects){
						deckMatchFound = deckMatchFound && deckDescription.names != null;
						if(deckMatchFound && !deckDescription.names.allowAllNames){
							for(var cardi = 0; cardi < deckObj.ContainedObjects.length && deckMatchFound; cardi++){
								deckMatchFound = !!deckDescription.names[deckObj.ContainedObjects[cardi].Nickname];//Validate the card names
								if(!deckMatchFound) console.log('Couldn\'t find: ' + deckObj.ContainedObjects[cardi].Nickname);
							}
						}
					}else{
						deckMatchFound = deckMatchFound && deckDescription.names == null;
						if(!deckMatchFound) console.log('Wrong names?');
					}
				}
				if(!deckMatchFound) console.log('Wrong cards');
				result = result && deckMatchFound;
				if(!result){
					console.log(JSON.stringify(deckObj));
				}
			}
		}
		cb(result);
	};
	if(input.isDraft){
		frogtown.HandleDraft(input,success,error);
	}else{
		frogtown.HandleDeck(input,success,error);
	}
}