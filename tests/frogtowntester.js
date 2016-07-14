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

var numAttempts = 3;

exports.Test = function(input,expectedOutput,cb){
	var endTest = function(result, output, errObj){
		if(result){
			cb(result);
		}else{
			if((!errObj || !errObj.badCards) && input.attempt < numAttempts){
				input.attempt++;
				exports.Test(input,expectedOutput,cb);
			}else{
				console.log(output);
				cb(result);
			}
		}
	}
	var error = function(reqObj, message, errObj){
		var output = '';
		if(expectedOutput.success){
			output += 'Error message: ' + message + '\n';
			output += 'Error: ' + JSON.stringify(errObj || {}) + '\n';
		}
		endTest(!expectedOutput.success, output, errObj);
	};
	var success = function(reqObj, deckId){
		var output = '';
		var result = expectedOutput.success;
		if(!result)output += 'Succeed when expected to fail\n';
		if(result){
			var dataObj = null;
			try{
				dataObj = JSON.parse(fs.readFileSync(config.deckDir + deckId + '.json'));
			}catch(err){
				try{
					dataObj = JSON.parse(fs.readFileSync('../' + config.deckDir + deckId + '.json'));
				}catch(err2){}
			}
			if(dataObj == null){
				output += 'Unable to load deck json\n';
				endTest(false, output);
				return;
			}
			var amtDecks = dataObj.ObjectStates.length;
			result = amtDecks == expectedOutput.successData.amtDecks; //Validate the correct number of decks
			if(!result)output += 'Wrong amount of decks\n';
			for(var decki = 0;decki < amtDecks && result; decki++){
				var deckObj = dataObj.ObjectStates[decki];
				deckObj.DeckIDs = deckObj.DeckIDs || [deckObj.CardID];
				var deckMatchFound = false;
				if(deckObj.ContainedObjects){
					result = result && deckObj.ContainedObjects.length == deckObj.DeckIDs.length;//Validate that the number of nicknames = number of cards
					if(!result)output += 'Wrong amount of card names\n';
				}
				for(var i = 0; i < expectedOutput.successData.deckDescriptions.length && result && !deckMatchFound; i++){
					deckDescription = expectedOutput.successData.deckDescriptions[i];
					deckMatchFound = deckObj.DeckIDs.length == deckDescription.amt;//Validate the card count is right
					if(deckObj.ContainedObjects){
						deckMatchFound = deckMatchFound && deckDescription.names != null;
						if(deckMatchFound && !deckDescription.names.allowAllNames){
							for(var cardi = 0; cardi < deckObj.ContainedObjects.length && deckMatchFound; cardi++){
								deckMatchFound = !!deckDescription.names[deckObj.ContainedObjects[cardi].Nickname];//Validate the card names
								if(!deckMatchFound) output += 'Couldn\'t find: ' + deckObj.ContainedObjects[cardi].Nickname + '\n';
							}
						}
					}else{
						deckMatchFound = deckMatchFound && deckDescription.names == null;
						if(!deckMatchFound) output += 'Wrong names?\n';
					}
				}
				if(!deckMatchFound) output += 'Wrong cards\n';
				result = result && deckMatchFound;
				if(!result){
					output += JSON.stringify(deckObj) + '\n';
				}
			}
		}
		endTest(result, output);
	};
	if(input.isDraft){
		frogtown.HandleDraft(input,success,error);
	}else{
		frogtown.HandleDeck(input,success,error);
	}
}