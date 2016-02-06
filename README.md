# TTSDeckImporter
MTG deck importer for Table Top Simulator

# settings.json
{
	"deckDir": "decks/",
	"imageDir": "images/",
	"setAssetDir": "setAssets/",
	"sendgrid": {
		"key" : "sendgrid api key",
		"alertEmail" : "frogtownmatt@gmail.com",
		"sourceEmail" : "error@frogtown.me"
	},
	"port": "34251",
	"defaultBackImage": "http://i.imgur.com/P7qYTcI.png",
	"tokenListDir": "misc/tokens.json",
	"tokenImageDir": "tokenAssets/",
	"transformMapDir": "misc/transforms.json",
	"expectedLocalHostName": "Matt-PC",
	"expectedHostName": "http://www.frogtown.me",
	"cardNameReplacements":[
		{
			"hard":"æ",
			"easy":"ae"
		},{
			"hard":"é",
			"easy":"e"
		},{
			"hard":"à",
			"easy":"a"
		}
	],
	"hardUrls": [
		{
			"name": "Ach! Hans, Run!",
			"url": "http://magiccards.info/scans/en/uh/116.jpg"
		},{
			"name": "Kongming, \"Sleeping Dragon\"",
			"url": "http://magiccards.info/scans/en/vma/33.jpg"
		},{
			"name": "Pang Tong, \"Young Phoenix\"",
			"url": "http://magiccards.info/scans/en/p3k/14.jpg"
		},{
			"name": "Ow",
			"url": "http://magiccards.info/scans/en/ug/36.jpg"
		},{
			"name": "Pang Tong, Young Phoenix",
			"url": "http://magiccards.info/scans/en/p3k/14.jpg"
		},{
			"name": "Vizzerdrix",
			"url": "http://magiccards.info/scans/en/9eb/7.jpg"
		},{
			"name": "Index",
			"url": "http://magiccards.info/scans/en/m13/55.jpg"
		},{
			"name": "Pang Tong, Young Phoenix",
			"url": "http://magiccards.info/scans/en/p3k/14.jpg"
		},{
			"name": "Pang Tong, Young Phoenix",
			"url": "http://magiccards.info/scans/en/p3k/14.jpg"
		},{
			"name": "Pang Tong, Young Phoenix",
			"url": "http://magiccards.info/scans/en/p3k/14.jpg"
		}
	],
	"mythicErrors": [
		{
			"name": "kozilekschanneler",
			"correction": "kozliekschanneler"
		},
		{
			"name": "tandemtactics",
			"correction": "unisonstrike"
		}
	]
}