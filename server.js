var express = require('express');
var bodyParser = require('body-parser');
var fs = require('fs');
var frogtown = require('./frogtown.js');
var config = require('./settings.json');
var logger = require('./logger.js');

var requestQueue = [
	//{
	//	req: request,
	//	res: response,
	//	isDraft: bool
	//}
];

var app = express();

app.use(express.static('www'));
app.use('/decks', express.static(config.deckDir));
app.use('/misc', express.static('misc'));
for(var i = 1; i <= Number(config.setAssetVersion); i++){
	app.use('/setAssets/v' + i, express.static(config.setAssetDir));
}
app.use(bodyParser.urlencoded({ extended: false }));

var numAttempts = 3;
var handleingRequest = false;

function HandleRequest(){
	if(!handleingRequest && requestQueue.length > 0){
		handleingRequest = true;

		var reqObj = requestQueue.shift();
		if(reqObj && reqObj.req && reqObj.res){
			if(reqObj.isDraft) frogtown.HandleDraft(reqObj,HandleSuccess,HandleError);
			else frogtown.HandleDeck(reqObj,HandleSuccess,HandleError);
		}
	}
};

function HandleError(reqObj, message, object){
	if(reqObj.attempt >= numAttempts){
		object = object || {};
		object.message = object.message || message || 'A server error occurred.';
		logger.majorError({'message': message});
		reqObj.res.end(JSON.stringify({
			status:1,
			errObj: {
				message: message
			}
		}));
	}else{
		reqObj.attempt++;
		requestQueue.push(reqObj);
	}

	handleingRequest = false;
	HandleRequest();
};

function HandleSuccess(reqObj, deckId){
	reqObj.res.end(JSON.stringify({name:deckId,status:0}));
	if(reqObj.isDraft) logger.logDraft(reqObj.req.body);
	else logger.logDeck(reqObj.req.body);

	handleingRequest = false;
	HandleRequest();
}

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