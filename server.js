var express = require('express');
var bodyParser = require('body-parser');
var config = require('./settings.json');
var net = require('net');
var fs = require('fs');
var StringDecoder = require('string_decoder').StringDecoder;
var sendgrid = require('sendgrid')(config.sendgrid.key);

var lastErrorEmail = new Date();

function clean(str){
	return cleanNewLines(str).replace(/\s/g,'_');
};

function cleanNewLines(str){
	return (str+'').replace(/[\r\n]/g,'');
}

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
}

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
}

var app = express();

app.use(express.static('www'));
app.use('/decks', express.static(config.deckDir));
app.use('/misc', express.static('misc'));
app.use('/setAssets', express.static(config.setAssetDir));
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
		var client = net.connect({port: config.port});
		var deckId = req.body.set.replace(/[^a-zA-Z0-9]/g, '') + '_' + getDeckID();
		client.on('close', function(){
			res.end(JSON.stringify({name:deckId,status:0}));//TODO add fail status
		});

		client.write('draft\r\n');
		client.write(deckId + '\r\n');
		client.write(cleanNewLines(req.body.set) + '\r\n');
		client.write(clean(req.body.n) + '\r\n');
	}catch(err){
		res.end(JSON.stringify({status:1,errObj:{message:'Unknown error occured'}}));
	}
});

app.post('/newdeck', function(req, res){
	try{
		var decklist = req.body.decklist;
		var deckID = getDeckID();
		
		var backURL = clean(req.body.backURL);
		var hiddenURL = clean(req.body.hiddenURL);
		var compression = clean(req.body.compression);
		var deckName = cleanNewLines(req.body.name);
		var useImgur = !!req.body.imgur;
		var coolifyBasic = !!req.body.coolify;

		var client = net.connect({port: config.port});
		
		client.on('error', function(){
			console.log('Deck maker is down...');
			majorError({'message': 'Unable to connect to deck maker'});
			res.end(JSON.stringify({
				status:1,
				errObj: {
					message: 'The server is experiencing technical issues, please check back soon for details.'
				}
			}));
		})
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
			}
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
	}catch(err){
		res.end(JSON.stringify({status:1,errObj:{message:'Unknown error occured'}}));
	}
});

app.listen(80, function(){
	console.log('Listening on port 80!');
});