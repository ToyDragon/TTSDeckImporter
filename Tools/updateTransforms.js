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
var DOWNLOAD_DIR = './Tools/files/';
var TRANSFORM_FILE = config.transformMapDir;

// We will be downloading the files to a directory, so make sure it's there
// This step is not required if you have manually created the directory
var mkdir = 'mkdir -p ' + DOWNLOAD_DIR;

if(os.platform().indexOf('win')!=-1) {
	mkdir = 'mkdir files';
}

var transformMap = {
  //"front card": "back card"
}

var load_sets = function(file_name){
	console.log('Loading file: ' + DOWNLOAD_DIR + file_name);
	var allSets = JSON.parse(fs.readFileSync(DOWNLOAD_DIR + file_name));
	console.log('File loaded!');
	for(var key in allSets){
		try{
			parse_set(allSets[key]);
		}catch(e){
			console.log('Couldn\'t parse '+allSets[key].name);
		}
	}
	fs.writeFileSync(TRANSFORM_FILE, JSON.stringify(transformMap));
}

var clean_card_name = function(name){
	return name.toLowerCase();
}

var parse_set = function(set){
  set.cards.forEach(function(card){
    if(card.layout == "double-faced"){
      transformMap[clean_card_name(card.names[0])] = clean_card_name(card.names[1]);
    }

    if(card.layout == "meld"){
      transformMap[clean_card_name(card.names[0])] = clean_card_name(card.names[0]) + " reverse";
      transformMap[clean_card_name(card.names[1])] = clean_card_name(card.names[1]) + " reverse";
    }
	});
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
