var imgur = require('imgur-node-api');

imgur.setClientID('f7bb51dd7271c7a');

var decknames = [];
for(var i = 2; i < process.argv.length; i++){
	decknames[i-2] = process.argv[i];
}

var upload_count = 0;
var upload = function(deck_i){
	var deck = decknames[deck_i];
	imgur.upload(__dirname + '/' + deck, function(req, res){
		console.log(res.data.link);
		if(deck_i < decknames.length-1){
			upload(deck_i+1);
		}else{
			process.exit(1);
		}
	});
};

upload(0);