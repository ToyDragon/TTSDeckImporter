var jsdom = require('jsdom');
var config = require('./settings.json');
var fs = require('fs');

function url(pagenum){
	return 'http://gatherer.wizards.com/Pages/Search/Default.aspx?output=standard&page='+pagenum+'&action=advanced&text=+%5btransform%5d';
}

function saveTransformMap(){
	console.log('Saving to ' + config.transformMapDir);

	var prettyStr = '{\r\n';
	for(var cardName in transformMap){
		prettyStr += '"' + cardName + '": "' + transformMap[cardName] + '",\r\n';
	}
	prettyStr = prettyStr.substr(0, prettyStr.length - 3) + '\r\n}';

	fs.writeFileSync(config.transformMapDir, prettyStr);
}

var transformMap = {
	/*
	"cardFrontName": "cardBackName"
	*/
};

var totalPages = -1;

function ScrapePage(pageNum){
	console.log('Scraping page ' + pageNum);
	if(pageNum == 0 && totalPages != -1){
		saveTransformMap();
		return;
	}
	jsdom.env(url(pageNum),
		["http://code.jquery.com/jquery.js"],
		function(err, window){

			if(pageNum == 0){
				var pageNumReg = /\/Pages\/Search\/Default.aspx\?page=([0-9]+)/;
				var href = window.$('.paging a:last').attr('href');
				var results = pageNumReg.exec(href);
				if(results) totalPages = results[1];
			}

			var cards = [
				/*
				{
					"name": "cardName",
					"gathererUrl": "url"
				}
				*/
			];
			window.$('.cardInfo').each(function(index, card){
				var cardTitle = window.$(card).find('.cardTitle').text().toLowerCase().trim();
				var url = window.$(card).find('.cardTitle a').attr('href').replace('..','http://gatherer.wizards.com/Pages');
				cards.push({
					"name": cardTitle,
					"gathererUrl": url
				});
			});

			var amtRemaining = cards.length;
			for(var i = 0; i < cards.length; i++){
				(function(card){
					jsdom.env(card.gathererUrl,
						["http://code.jquery.com/jquery.js"],
						function(err, subwindow){
							subwindow.$('#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_ctl03_cardImage').each(function(index, img){
								var transformName = subwindow.$(img).attr('alt').toLowerCase().trim();
								if(transformName != card.name){
									transformMap[card.name] = transformName;
								}
							});

							amtRemaining--;
							if(amtRemaining == 0){
								//assume only one page caus i'm lazy
								saveTransformMap();
							}
						}
					);
				})(cards[i]);
			}
		}
	);
}

ScrapePage(0);