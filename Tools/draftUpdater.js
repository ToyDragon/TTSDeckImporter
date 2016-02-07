// Dependencies
var fs = require('fs');
var os = require('os');
var net = require('net');
var url = require('url');
var http = require('http');
var child_process = require('child_process');
var exec = require('child_process').exec;
var spawn = require('child_process').spawn;
var config = require('../settings.json');

// App variables
var file_url = 'http://mtgjson.com/json/AllSets.json';
var DOWNLOAD_DIR = './files/';
var SET_DIR = config.setAssetDir;

// We will be downloading the files to a directory, so make sure it's there
// This step is not required if you have manually created the directory
var mkdir = 'mkdir -p ' + DOWNLOAD_DIR;

if(os.platform().indexOf('win')!=-1) {
	mkdir = 'mkdir files';
}

function getDeckID(){
	var chars = 'abcdefghijklmnopqrstuvwxyz';
	var name = '';
	for(var i = 0; i < 16; i++){
		name += chars[Math.floor(Math.random()*chars.length)];
	}
	return name;
}

var setList = [];

var load_sets = function(file_name){
	console.log('Loading file: '+DOWNLOAD_DIR + file_name);
	var allSets = JSON.parse(fs.readFileSync(DOWNLOAD_DIR + file_name));
	console.log('File loaded!');
	for(var key in allSets){
		try{
			save_set(allSets[key]);
		}catch(e){
			console.log('Couldn\'t build '+allSets[key].name);
		}
	}
	setList.sort(function(a,b){return a.date == b.date ? 0 : -(a.date > b.date) || 1;})
	fs.writeFileSync(SET_DIR + 'setlist', JSON.stringify(setList));
	var StringDecoder = require('string_decoder').StringDecoder;
	BuildSet(0, setList);
}

function BuildSet(index, setList){
	var client = net.connect({port: config.port});
	var set = setList[index];
	client.on('close', function(){
		console.log('Done with ' + set.name);
		if(index < setList.length - 1){
			BuildSet(index+1, setList);
		}
	});

	client.on('error', function(){
		console.log('error :(');
		console.log(arguments);
	});

	client.write('draft\r\n');
	client.write(getDeckID() + '\r\n');
	client.write(set.name + '\r\n');
	client.write('1\r\n');
	client.write('ENDDECK\r\n');
}

var clean_card_name = function(name){
	var doubles = [
		['Alive','Well','Alive (Alive/Well)'],
		['Armed','Dangerous','Armed (Armed/Dangerous)'],
		['Assault','Battery','Assault (Assault/Battery)'],
		['Beck','Call','Beck (Beck/Call)'],
		['Boom','Bust','Boom (Boom/Bust)'],
		['Bound','Determined','Bound (Bound/Determined)'],
		['Breaking','Entering','Breaking (Breaking/Entering)'],
		['Catch','Release','Catch (Catch/Release)'],
		['Crime','Punishment','Crime (Crime/Punishment)'],
		['Dead','Gone','Dead (Dead/Gone)'],
		['Down','Dirty','Down (Down/Dirty)'],
		['Far','Away','Far (Far/Away)'],
		['Fire','Ice','Fire (Fire/Ice)'],
		['Flesh','Blood','Flesh (Flesh/Blood)'],
		['Give','Take','Give (Give/Take)'],
		['Hide','Seek','Hide (Hide/Seek)'],
		['Hit','Run','Hit (Hit/Run)'],
		['Illusion','Reality','Illusion (Illusion/Reality)'],
		['Life','Death','Life (Life/Death)'],
		['Night','Day','Night (Night/Day)'],
		['Odds','Ends','Odds (Odds/Ends)'],
		['Order','Chaos','Order (Order/Chaos)'],
		['Pain','Suffering','Pain (Pain/Suffering)'],
		['Profit','Loss','Profit (Profit/Loss)'],
		['Protect','Serve','Protect (Protect/Serve)'],
		['Pure','Simple','Pure (Pure/Simple)'],
		['Ready','Willing','Ready (Ready/Willing)'],
		['Research','Development','Research (Research/Development)'],
		['Rise','Fall','Rise (Rise/Fall)'],
		['Rough','Tumble','Rough (Rough/Tumble)'],
		['Spite','Malice','Spite (Spite/Malice)'],
		['Stand','Deliver','Stand (Stand/Deliver)'],
		['Supply','Demand','Supply (Supply/Demand)'],
		['Toil','Trouble','Toil (Toil/Trouble)'],
		['Trial','Error','Trial (Trial/Error)'],
		['Turn','Burn','Turn (Turn/Burn)'],
		['Wax','Wane','Wax (Wax/Wane)'],
		['Wear','Tear','Wear (Wear/Tear)'],
		['Who','','What (Who/What/When/Where/Why)'],
		['What','',''],
		['When','',''],
		['Where','',''],
		['Why','','']
	];
	var badNames = ['zombie token card','squirrel token card','soldier token card','sheep token card','pegasus token card','goblin token card'];
	for(var i in badNames){
		if(name.toLowerCase() == badNames[i]){
			name = '';
		}
	}
	for(var i in doubles){
		if(name == doubles[i][0]){
			name = doubles[i][2];
		}
		if(name == doubles[i][1]){
			name = '';
		}
	}
	return name.replace(/\?/,'');
}

var save_set = function(set){
	set.name = set.name.replace(':','');

	console.log('Writing to ' + SET_DIR+set.name);
	var file = ''
	file += 'Name\n';
	file += set.name+'\n';
	file += 'Code\n';
	file += set.code+'\n';
	if(set.gathererCode){
		file += 'Gatherer Code\n';
		file += set.gathererCode+'\n';
	}
	if(set.booster){
		var counts = {};
		set.booster.forEach(function(e){
			if(Array.isArray(e) && e[0]=='power nine') e = 'Special';
			if(set.code == 'ME4' && e == 'urza land') e = 'Special';
			counts[e] = (counts[e] || 0) + 1;
		});
		file += 'Booster\n';
		for(var i in counts){
			file += i+':'+counts[i]+'\n';
		}
	}

		var count = 0;
	if(set.cards){
		var seen = {};
		var rarities = {};
		set.cards.forEach(function(card){
			if(!rarities[card.rarity])rarities[card.rarity] = [];
			var n = card.multiverseid + ":" + clean_card_name(card.name);
			if(n.length > 0  && !seen[n]){
				seen[n] = true;
				rarities[card.rarity].push(n);
				count = count + 1;
			}
		});
		for(var rarity in rarities){
			if(set.code == 'ME4' && rarity == 'Basic Land'){
				rarity = 'Special';
			}
			file += rarity+'\n';
			for(var card in rarities[rarity]){
				file += rarities[rarity][card]+'\n';
			}
		}
	}
	if(count >= 20 && set.booster){
		fs.writeFileSync(SET_DIR + set.name, file);
		console.log('Done writing to ' + SET_DIR+set.name);

		setList.push({
			name:set.name,
			code:set.magicCardsInfoCode,
			date:set.releaseDate,
			block:set.block,
			type:set.type
		});
	}else{
		console.log('Not writing ' + set.name);
	}
}

// Function to download file using HTTP.get
var options = {
	host: url.parse(file_url).host,
	port: 80,
	path: url.parse(file_url).pathname
};

var file_name = url.parse(file_url).pathname.split('/').pop();

var keep = process.argv[2];

if(keep != '-n' && keep != '-nodownload'){
	var file = fs.createWriteStream(DOWNLOAD_DIR + file_name);
	console.log(file_name + ' downloading to ' + DOWNLOAD_DIR);
	http.get(options, function(res) {
		res.on('data', function(data) {
			file.write(data);
		}).on('end', function() {
			file.end(function(){
				console.log(file_name + ' downloaded to ' + DOWNLOAD_DIR);
				load_sets(file_name);
			});
		}).on('error', function(){
			console.log('error :(');
		});
	});
}else{
	load_sets(file_name);
}
