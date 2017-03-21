var net = require('net');
var config = require('./settings.json');
var StringDecoder = require('string_decoder').StringDecoder;

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
};

function clean(str, onlyNewLines){
	var clean = (str+'').replace(/[\r\n]/g,'');
	if(!onlyNewLines) clean = clean.replace(/\s/g,'_');
	return clean;
};

exports.HandleDraft = function(reqObj, success, error){
	var req = reqObj.req;
	var res = reqObj.res;
	var client = net.connect({port: config.port});
	var deckId = req.body.set.replace(/[^a-zA-Z0-9]/g, '') + '_' + getDeckID();

	var errorOccured = false;

	client.on('error', function(e){
		errorOccured = true;
		console.log('Error: ' + e.code);
		if(error){
			error(reqObj,'The server is experiencing technical issues, please check back soon for details.');
		}
	});

	client.on('close', function(){
		if(!errorOccured && success)success(reqObj, deckId);
		errorOccured = false;
	});

	client.write('draft\r\n');
	client.write(deckId + '\r\n');
  client.write(clean(req.body.set, true) + '\r\n');
  client.write(clean(req.body.n) + '\r\n');
}

exports.HandleDeck = function(reqObj, success, error){
	var req = reqObj.req;
  var res = reqObj.res;
	var decklist = req.body.decklist + '\r\nENDDECK';
	var deckId = getDeckID();

	var backURL = clean(req.body.backURL);
	var hiddenURL = clean(req.body.hiddenURL);
	var compression = clean(req.body.compression);
	var deckName = clean(req.body.name, true);
	var coolifyBasic = clean(req.body.coolify);
	var artifyBasic = clean(req.body.artify);

	console.log('Handling deck...');

	var client = net.connect({port: config.port});
  var deckId = getDeckID();
	var errorOccured = false;

	client.on('error', function(e){
		errorOccured = true;
		console.log('Error: ' + e.code);
		if(error){
			error(reqObj,'The server is experiencing technical issues, please check back soon for details.');
		}
	});

	client.on('close', function(){
		if(!errorOccured){
			var errObj = null;
			try{errObj = JSON.parse(data);}catch(err){}
			if(errObj){
				console.log('Error data: ' + JSON.stringify(data));
				if(error)error(reqObj, null, errObj);
			}else if(success){
				console.log('Done with deck');
				success(reqObj, deckId);
			}
		}
		errorOccured = false;
	});

	var data = '';
	var decoder = new StringDecoder('utf8');
	client.on('data', function(buffer){
		data += decoder.write(buffer);
	});

	console.log('About to write "deck"');
	client.write('deck\r\n');
  client.write(deckId + '\r\n');
  client.write(deckName + '\r\n');
  client.write(backURL + '\r\n');
  client.write(hiddenURL + '\r\n');
  client.write(coolifyBasic + '\r\n');
  client.write(artifyBasic +'\r\n');
  client.write(compression + '\r\n');
  client.write(decklist + '\r\n');
  client.write('ENDDECK\r\n');
};
