var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 1,
			deckDescriptions: [
				{
					amt: 9,
					names: {
						"Séance": 1,
						"Aetherling": 1,
						"Déjà Vu": 1,
						"Lim-dûl's Vault": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "\n"
				         +"Séance\n"
				         +"Seance\n"
				         +"Aetherling\n"
				         +"aEtherling\n"
				         +"Ætherling\n"
				         +"Déjà Vu\n"
				         +"Deja Vu\n"
				         +"Lim-Dûl's Vault\n"
				         +"lim-dul's vault",
				backURL: '',
				hiddenURL: '',
				name: '',
				coolify: true,
				compression: 0.1
			}
		},
		res: {},
		attempt: 0,
		isDraft: false
	}

	try{
		tester.Test(input, expectedOutput, cb);
	}catch(err){
		console.log('Error: ' + err);
		cb(false);
	}
}