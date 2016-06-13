var jsdom = require('jsdom');
var config = require('./settings.json');
var fs = require('fs');

var tokenRegex = /([0-9]+\/[0-9]+ )?((?:(?:colorless|black|red|green|white|blue)(?: |and|, )?)+ [^\.0-9]+? token)s?(?: named ([a-zA-Z]+))?/i;
var illegalCharsRegex = /[^0-9a-z_]/g;

function url(pagenum){
	return 'http://gatherer.wizards.com/Pages/Search/Default.aspx?output=standard&page='+pagenum+'&action=advanced&text=|[emblem]|[token]';
}

function saveTokenMap(){
	console.log('Saving to ' + config.tokenListDir);

	var prettyStr = '{\r\n';
	for(var cardName in tokenMap){
		prettyStr += '"' + cardName + '": ';

		var tokenList = '[';
		for(var tokeni = 0; tokeni < tokenMap[cardName].length; tokeni++){
			tokenList += '"' + tokenMap[cardName][tokeni] + '"';
			if(tokeni < tokenMap[cardName].length - 1) tokenList += ', '; 
		}
		tokenList += ']'

		prettyStr += tokenList + ',\r\n';
	}
	prettyStr = prettyStr.substr(0, prettyStr.length - 3) + '\r\n}';

	fs.writeFileSync(config.tokenListDir, prettyStr);
}

function checkExistingTokens(){
	var tokenSet = {};
	for(var cardName in tokenMap){
		for(var tokenName in tokenMap[cardName]){
			tokenSet[tokenMap[cardName][tokenName]] = 1;
		}
	}

	for(var tokenName in tokenSet){
		if (tokenSet.hasOwnProperty(tokenName)) {
			var path = config.tokenImageDir + '/' + tokenName + '.jpg';
			try {
				fs.accessSync(path, fs.F_OK);
			} catch (e) {
				console.log('Unable to find ' + path);
			}
		}
	}
}

var tokenMap = {
	/*
	cardName: ['token1ImageName','token2ImageName']
	*/
	'godsire': ['8_8redgreenwhitebeastcreaturetoken'],
	'hazezon tamar': ['1_1redgreenwhitesandwarriorcreaturetoken'],
	'wurmcoil engine': ['wurmcoilenginea','wurmcoilengineb']
};

var totalPages = -1;

function ScrapePage(pageNum){
	console.log('Scraping page ' + pageNum);
	if(pageNum == 0 && totalPages != -1){
		saveTokenMap();
		checkExistingTokens();
		return;
	}
	jsdom.env(url(pageNum),
		["http://code.jquery.com/jquery.js"],
		function(err, window){
			window.$('.cardInfo').each(function(index, card){
				var cardTitle = window.$(card).find('.cardTitle').text().toLowerCase().trim();
				if(!tokenMap[cardTitle]){
					var cardText = window.$(card).find('.rulesText').text().trim();

					if(cardText.indexOf('emblem') >= 0){
						tokenMap[cardTitle] = tokenMap[cardTitle] || [];
						tokenMap[cardTitle].push(cardTitle.replace(/[\s,'-]+/g,'_')+'_emblem');
					}

					while(true){
						var results = tokenRegex.exec(cardText);
						if(cardTitle.indexOf('linvala')>=0){
							console.log(cardText);
							console.log(JSON.stringify(results));
						}
						if(!results)break;

						var tokenStr = results[1] ? results[1] : '';
						tokenStr += results[2];
						if(results[3]) tokenStr += results[3];

						cardText = cardText.substr(results.index + tokenStr.length);

						if(results[3])tokenStr = results[3];

						tokenStr = tokenStr.toLowerCase().replace(/\//g,'_').replace(illegalCharsRegex,'');

						tokenMap[cardTitle] = tokenMap[cardTitle] || [];
						tokenMap[cardTitle].push(tokenStr);
					}
				}
			});

			if(pageNum == 0){
				var pageNumReg = /\/Pages\/Search\/Default.aspx\?page=([0-9]+)/;
				var href = window.$('.paging a:last').attr('href');
				var results = pageNumReg.exec(href);
				totalPages = results[1];

				ScrapePage(totalPages);
			}else{
				ScrapePage(pageNum-1);
			}
		}
	);
}

ScrapePage(0);