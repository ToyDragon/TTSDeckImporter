var express = require('express');
var bodyParser = require('body-parser');
var fs = require('fs');
var exec = require('child_process').exec;
var MongoClient = require('mongodb').MongoClient;
var os = require('os');

var app = express();
var db;
var collection;

var debug = false;
process.argv.forEach(function (val, index, array) {
	if(val == 'debug'){
		debug = true;
		console.log("DEBUG MODE ENABLED");
	}
});

MongoClient.connect('mongodb://localhost/mtg', function(err, dbt){
	if(err){
		console.log(err);
	}else{
		console.log('Connected!');
		db = dbt;
		
		collection = db.collection('decks');
	}
});

app.use(express.static('www'));
app.use('/decks', express.static('decks'));
app.use('/misc', express.static('misc'));
app.use('/setAssets', express.static('setAssets'));
app.use(bodyParser.urlencoded({ extended: false }));

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
}

app.get('/sets', function(req, res){
	fs.readdir('sets',function(err, files){
		for(var i in files){
			files[i] = files[i].substring(0,files[i].indexOf('.'));
		}
		res.end(JSON.stringify(files));
	});
});

app.get('/decks', function(req, res){
	var decks = [];
	collection.find().sort({_id:-1}).limit(20).toArray(function(err, arr){
		console.log('err: ' + err);
		arr.forEach(function(item){
			decks.push(item);
			console.log(JSON.stringify(item));
		});
		console.log('decks: ' + JSON.stringify(decks));
		res.end(JSON.stringify(decks));
	});
});

app.post('/newdeck', function(req, res){
	var decklist = req.body.decklist+'\n';
	var deckID = getDeckID();
	
	var deckName = req.body.deckName;
	if(deckName == 'default')deckName = deckID;
	
	var backURL = req.body.backURL;
	
	var hiddenURL = req.body.hiddenURL;
	
	var compression = req.body.compression;
	
	//save decklist to file
	fs.writeFile(__dirname+'/lists/'+deckID, decklist, function(err) {
		if(err) {
			return console.log(err);
		}
		//invoke the deck maker
		var clean = function(str){
			return str.replace(/([\(\)])/g,'\\$1').replace(/\s/g,'_');
		};
		
		var nameClean = clean(deckName);
		var hideClean = clean(hiddenURL);
		var backClean = clean(backURL);
		
		var cmdStr = 'java -cp gson-2.3.1.jar:. MakeDeck ' + deckID;
		if(os.platform().indexOf('win')!=-1)cmdStr = 'java -cp gson-2.3.1.jar;. MakeDeck ' + deckID;
		cmdStr += ' -name ' + nameClean;
		cmdStr += ' -backURL ' + backClean;
		cmdStr += ' -hiddenURL ' + hideClean;
		cmdStr += ' -compression ' + compression;
		if(req.body.coolify){
			cmdStr += ' -coolifyBasics';
		}
		if(debug){
			console.log('running: ' + cmdStr);
		}
		var imgurI = '';
		if(req.body.imgur && req.body.imgur == 'true'){
			//console.log('Using imgur!');
			cmdStr += ' imgur';
			imgurI='[i]';
		}
		var dateString = new Date().toLocaleDateString();
		
		console.log('('+dateString+')Saved decklist'+imgurI+' ' + deckID+' '+deckName);
		if(debug){
			console.log("cmd: "+cmdStr);
		}
		var proc = exec(cmdStr, function(error, stdout, stderr){
			if(debug){
				console.log('err:'+error);
				console.log('out:'+stdout);
				console.log('ser:'+stderr);
			}
		});

		proc.on('exit', function(code){
			if(code == 0 && deckName != deckID){
				//success!
				if(collection)
					collection.insert({name:deckName,uid:deckID});
			}
			if(debug){
				console.log('ended with code ' + code);
			}
			res.end(JSON.stringify({name:deckID,status:code}));
		});
		//respond with deck name
	}); 
});

app.listen(80, function(){
	console.log('Listening on port 80!');
});

function cleanUp(){
	console.log('Cleaning up...');
	db.close();
}

process.on('exit', function () {
	cleanUp();
});