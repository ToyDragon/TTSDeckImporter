var config = require('./settings.json');
var util = require('util');
var sendgrid = require('sendgrid')(config.sendgrid.key);

var nextErrorSummary = new Date();
var nextDailySummary = new Date();

var decksToday = [
	//{
	//	name: deckName,
	//	mainboard: [card line, card line],
	//	sideboard: [card line],
	//	commander: [card line]
	//}
];
var draftsToday = {
	//"setName": {
	//	count: 10
	//	packAmounts: [18,18,24,18]
	//}
};

function prettifyJson(msg){
	msg = msg.replace(/,/g,',\r\n').replace(/([\[{])/g,'$1\r\n').replace(/([\]}])/g,'\r\n$1');
	var lines = msg.split('\r\n');
	var indent=0;
	for(var i = 0; i < lines.length; i++){
		if(/[}\]]/.exec(lines[i]))indent--;
		for(var j=0; j < indent; j++)lines[i] = '  '+lines[i];
		if(/[{\[]/.exec(lines[i]))indent++;
	}
	return lines.join('\r\n');
}

function notifyDeck(){
	if(new Date() > nextDailySummary){
		var deckStats = {};
		deckStats.drafts = draftsToday;

		var uniqueNames = {};

		var amtCommander = 0;
		var amtWithSideboard = 0;
		var averageSize = 0;
		var averageUnique = 0;
		var totalUnique = 0;
		var totalSize = 0;
		var totalCount = 0;
		var withNoName = 0;
		var totalWithBadCards = 0;
		var withDuplicateNames = 0;

		for(var i = 0; i < decksToday.length; i++){
			var deck = decksToday[i];

			if(!deck.name || deck.name.trim().length == 0)withNoName++;
			else if(uniqueNames[deck.name.trim()])withDuplicateNames++;
			else uniqueNames[deck.name.trim()]=true;

			if(deck.badCards)totalWithBadCards++;

			if(deck.sideboard.length > 0)amtWithSideboard++;
			if(deck.commander.length > 0)amtCommander++;
			else{
				totalUnique += deck.commander.length + deck.mainboard.length + deck.sideboard.length;
				var cardRegex = /^([0-9]*)x?\s/;
				var lists = [deck.commander, deck.mainboard, deck.sideboard];
				for(var listi = 0; listi < lists.length; listi++){
					var list = lists[listi];
					for(var j = 0; j < list.length; j++){
						try{
							var count = 1;
							var result = cardRegex.exec(list[j]);
							if(result) count = Number(result[1]);
							totalSize += count;
						}catch(unused){console.log(unused);}
					}
				}
				totalUnique++;
			}
		}

		if(decksToday.length > 0){
			averageUnique = totalUnique / decksToday.length;
			averageSize = totalSize / decksToday.length;
		}

		deckStats.decks = {
			amtCommander: amtCommander,
			amtWithSideboard: amtWithSideboard,
			averageSize: averageSize,
			totalSize: totalSize,
			averageUnique: averageUnique,
			totalUnique: totalUnique,
			totalCount: decksToday.length,
			totalWithBadCards: totalWithBadCards,
			withNoName: withNoName,
			withDuplicateNames: withDuplicateNames
		};

		var payload = {
			to      : config.sendgrid.alertEmail,
			from    : config.sendgrid.summaryEmail,
			subject : 'Daily Summary',
			text    : prettifyJson(JSON.stringify(deckStats))
		};

		sendgrid.send(payload, function(err, json) {
			if (err) { console.error(err); }
			console.log(json);
		});

		nextDailySummary = new Date();
		nextDailySummary.setDate(nextDailySummary.getDate()+1);
		nextDailySummary.setHours(21);//9pm?
		nextDailySummary.setMinutes(0);

		decksToday = [];
		draftsToday = {};
		console.log("Sent daily summary");
	}
}

exports.logDeck = function(body, badCards){
	try{
		var deckObj = {
			name: body.name,
			badCards: badCards,
			mainboard: [],
			sideboard: [],
			commander: []
		};
		var rawLines = body.decklist.split(/[\r\n]/);
		var prefixsToIgnore = [new RegExp('^CREATURE \\(')
			                 , new RegExp('^INSTANT \\(')
			                 , new RegExp('^LAND \\(')
			                 , new RegExp('^PLANESWALKER \\(')
			                 , new RegExp('^TCG \\$')
			                 , new RegExp('^SIDEBOARD \\(')
			                 , new RegExp('^ENCHANTMENT \\(')
			                 , new RegExp('^SORCERY \\(')
			                 , new RegExp('^MAYBEBOARD \\(')];
		var suffixsToIgnore = [new RegExp(' CREATURES$')
		                     , new RegExp(' INSTANTS and SORC.$')
		                     , new RegExp(' LANDS$')
		                     , new RegExp(' OTHER SPELLS$')];

		var curDeck = deckObj.mainboard;
		for(var i = 0; i < rawLines.length; i++){
			var line = rawLines[i];
			if(line == 'COMMANDER'){curDeck = deckObj.commander; continue;}
			if(line == 'MAINBOARD'){curDeck = deckObj.mainboard; continue;}
			if(line == 'SIDEBOARD'){curDeck = deckObj.sideboard; continue;}
			var badLine = false;
			for(var j = 0; j < prefixsToIgnore.length && !badLine; j++){
				if(prefixsToIgnore[j].exec(line)){badLine = true; break;}
			}
			for(var j = 0; j < suffixsToIgnore.length && !badLine; j++){
				if(suffixsToIgnore[j].exec(line)){badLine = true; break;}
			}
			if(badLine) continue;

			curDeck.push(line);
		}
		decksToday.push(deckObj);
		notifyDeck();
	}catch(err){}
};

exports.logDraft = function(body){
	try{
		draftsToday[body.set] = draftsToday[body.set] || {};
		draftsToday[body.set].packAmounts = draftsToday[body.set].packAmounts || [];
		draftsToday[body.set].packAmounts.push(body.n);

		notifyDeck();
	}catch(err){}
};

exports.majorError = function(details){
	if(new Date() > nextErrorSummary){
		var payload = {
			to      : config.sendgrid.alertEmail,
			from    : config.sendgrid.sourceEmail,
			subject : 'Frogtown Error',
			text    : util.inspect(details)
		};

		sendgrid.send(payload, function(err, json) {
			if (err) { console.error(err); }
			console.log(json);
		});
		nextErrorSummary = new Date();
		nextErrorSummary.setMinutes(nextErrorSummary.getMinutes() + 30);
	}
};
