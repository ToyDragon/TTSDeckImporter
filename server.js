var express = require('express');
var bodyParser = require('body-parser');
var config = require('./settings.json');
var logger = require('./logger.js');
var net = require('net');
var fs = require('fs');
var StringDecoder = require('string_decoder').StringDecoder;

var numAttempts = 3;

var handleingRequest = false;
var requestQueue = [
	//{
	//	req: request,
	//	res: response,
	//	isDraft: bool
	//}
];

var app = express();

function clean(str, onlyNewLines){
	var clean = (str+'').replace(/[\r\n]/g,'');
	if(!onlyNewLines) clean = clean.replace(/\s/g,'_');
	return clean;
};

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
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

	var errorOccured = false;
	
	client.on('error', function(){
		errorOccured = true;
		if(reqObj.attempt >= numAttempts){
			console.log('Deck maker is down...');
			logger.majorError({'message': 'Unable to connect to deck maker'});
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
		if(!errorOccured){
			res.end(JSON.stringify({name:deckId,status:0}));
			logger.logDraft(req.body);
			handleingRequest = false;
			HandleRequest();
		}
		errorOccured = false;
	});

	client.write('draft\r\n');
	client.write(deckId + '\r\n');
	client.write(clean(req.body.set, true) + '\r\n');
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
	var deckName = clean(req.body.name, true);
	var coolifyBasic = !!req.body.coolify;

	var client = net.connect({port: config.port});

	var errorOccured = false;
	
	client.on('error', function(){
		errorOccured = true;
		if(reqObj.attempt >= numAttempts){
			console.log('Deck maker is down...');
			logger.majorError({'message': 'Unable to connect to deck maker'});
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
		if(!errorOccured){
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
				logger.logDeck(req.body, !!errObj);
			}
			handleingRequest = false;
			HandleRequest();
		}
		errorOccured = false;
	});

	var data = '';
	var decoder = new StringDecoder('utf8');
	client.on('data', function(buffer){
		data += decoder.write(buffer);
	});

	client.write('deck\r\n');
	client.write(deckID + '\r\n');
	client.write(deckName + '\r\n');
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
app.use('/setAssets/v2', express.static(config.setAssetDir));
app.use('/setAssets/v3', express.static(config.setAssetDir));
app.use(bodyParser.urlencoded({ extended: false }));

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