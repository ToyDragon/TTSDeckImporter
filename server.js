var express = require('express');
var bodyParser = require('body-parser');
var config = require('./settings.json');
var net = require('net');
var fs = require('fs');
var StringDecoder = require('string_decoder').StringDecoder;
var sendgrid = require('sendgrid')(config.sendgrid.key);

var lastErrorEmail = new Date();
var lastDailySummaryEmail = new Date();

var numAttempts = 3;

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

var handleingRequest = false;
var requestQueue = [
	//{
	//	req: request,
	//	res: response,
	//	isDraft: bool
	//}
];

var app = express();

function prettifyJson(msg){
	msg = msg.replace(/,/g,',\r\n').replace(/([\[{])/g,'$1\r\n').replace(/([\]}])/g,'\r\n$1');
	var lines = msg.split('\r\n');
	var indent=0;
	for(var i = 0; i < lines.length; i++){
		if(/[}\]]/.exec(lines[i]))indent--;
		for(var j=0; j < indent; j++)lines[i] = '  '+lines[i];
		if(/[{\[]/.exec(lines[i]))indent++;
	}
	return lines.join("\r\n");
}

var test = 0;

function notifyDeck(){
	if(new Date() > lastDailySummaryEmail && test++ > 3){
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
		lastErrorEmail = new Date();
		lastErrorEmail.setMinutes(lastErrorEmail.getMinutes() + 150);

		lastDailySummaryEmail = new Date();
		lastDailySummaryEmail.setDate(lastDailySummaryEmail.getDate()+1);
		lastDailySummaryEmail.setHours(21);//9pm
		lastDailySummaryEmail.setMinutes(0);

		console.log("Sent daily summary");
	}
}

function logDeck(body, badCards){
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
	}catch(err){console.log(err);}
};

function logDraft(body){
	try{
		draftsToday[body.set] = draftsToday[body.set] || {};
		draftsToday[body.set].packAmounts = draftsToday[body.set].packAmounts || [];
		draftsToday[body.set].packAmounts.push(body.n);

		notifyDeck();
	}catch(err){}
};

function clean(str){
	return cleanNewLines(str).replace(/\s/g,'_');
};

function cleanNewLines(str){
	return (str+'').replace(/[\r\n]/g,'');
};

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
};

function majorError(details){
	if(new Date() > lastErrorEmail){
		var payload = {
			to      : config.sendgrid.alertEmail,
			from    : config.sendgrid.sourceEmail,
			subject : 'Frogtown Error',
			text    : JSON.stringify(details)
		};

		sendgrid.send(payload, function(err, json) {
			if (err) { console.error(err); }
			console.log(json);
		});
		lastErrorEmail = new Date();
		lastErrorEmail.setMinutes(lastErrorEmail.getMinutes() + 30);
	}
};

function HandleRequest(){
	if(!handleingRequest && requestQueue.length > 0){
		handleingRequest = true;

		var reqObj = requestQueue.shift();
		if(reqObj && reqObj.req && reqObj.res){
			if(reqObj.isDraft) HandleDraft(reqObj);
			else HandleDeck(reqObj);
		}
	}
};

function HandleDraft(reqObj){
	var req = reqObj.req;
	var res = reqObj.res;
	var client = net.connect({port: config.port});
	var deckId = req.body.set.replace(/[^a-zA-Z0-9]/g, '') + '_' + getDeckID();

	client.on('error', function(){
		if(reqObj.attempt >= numAttempts){
			console.log('Deck maker is down...');
			majorError({'message': 'Unable to connect to deck maker'});
			res.end(JSON.stringify({
				status:1,
				errObj: {
					message: 'The server is experiencing technical issues, please check back soon for details.'
				}
			}));
		}else{
			reqObj.attempt++;
			requestQueue.push(reqObj);
		}
		handleingRequest = false;
		HandleRequest();
	});

	client.on('close', function(){
		res.end(JSON.stringify({name:deckId,status:0}));
		logDraft(req.body);
		handleingRequest = false;
		HandleRequest();
	});

	client.write('draft\r\n');
	client.write(deckId + '\r\n');
	client.write(cleanNewLines(req.body.set) + '\r\n');
	client.write(clean(req.body.n) + '\r\n');
};

function HandleDeck(reqObj){
	var req = reqObj.req;
	var res = reqObj.res;
	var decklist = req.body.decklist + '\r\nENDDECK';
	var deckID = getDeckID();
	
	var backURL = clean(req.body.backURL);
	var hiddenURL = clean(req.body.hiddenURL);
	var compression = clean(req.body.compression);
	var deckName = cleanNewLines(req.body.name);
	var useImgur = !!req.body.imgur;
	var coolifyBasic = !!req.body.coolify;

	var client = net.connect({port: config.port});
	
	client.on('error', function(){
		if(reqObj.attempt >= numAttempts){
			console.log('Deck maker is down...');
			majorError({'message': 'Unable to connect to deck maker'});
			res.end(JSON.stringify({
				status:1,
				errObj: {
					message: 'The server is experiencing technical issues, please check back soon for details.'
				}
			}));
		}else{
			reqObj.attempt++;
			requestQueue.push(reqObj);
		}
		handleingRequest = false;
		HandleRequest();
	});

	client.on('close', function(){
		var errObj = null;
		try{
			errObj = JSON.parse(data)
		}catch(err){}
		if(errObj){
			console.log(errObj);
			res.end(JSON.stringify({
				status:1,
				errObj: errObj
			}));
		}else{
			res.end(JSON.stringify({
				status: 0,
				name: deckID
			}));
			logDeck(req.body, !!errObj);
		}
		handleingRequest = false;
		HandleRequest();
	});

	var data = '';
	var decoder = new StringDecoder('utf8');
	client.on('data', function(buffer){
		data += decoder.write(buffer);
	});

	client.write('deck\r\n');
	client.write(deckID + '\r\n');
	client.write(deckName + '\r\n');
	client.write(useImgur + '\r\n');
	client.write(backURL + '\r\n');
	client.write(hiddenURL + '\r\n');
	client.write(coolifyBasic + '\r\n');
	client.write(compression + '\r\n');
	client.write(decklist + '\r\n');
	client.write('ENDDECK\r\n');
}

app.use(express.static('www'));
app.use('/decks', express.static(config.deckDir));
app.use('/misc', express.static('misc'));
app.use('/setAssets/v1', express.static(config.setAssetDir));
app.use(bodyParser.urlencoded({ extended: false }));

app.get('/ping', function(req, res){
	res.end('true\r\n');
});

app.get('/sets', function(req, res){
	try{
		fs.readFile(config.setAssetDir + 'setlist',function(err, data){
			res.end(data);
		});
	}catch(err){
		res.end(JSON.stringify({status:1,errObj:{message:'Unknown error occured'}}));
	}
});

app.post('/newdraft', function(req, res){
	try{
		requestQueue.push({
			req: req,
			res: res,
			attempt: 0,
			isDraft: true
		});
		HandleRequest();
	}catch(err){
		console.log(err);
		res.end(JSON.stringify({status:1,errObj:{message:'Unknown error occured'}}));
	}
});

app.post('/newdeck', function(req, res){
	try{
		requestQueue.push({
			req: req,
			res: res,
			attempt: 0,
			isDraft: false
		});
		HandleRequest();
	}catch(err){
		console.log(err);
		res.end(JSON.stringify({status:1,errObj:{message:'Unknown error occured'}}));
	}
});

app.listen(80, function(){
	console.log('Listening on port 80!');
});