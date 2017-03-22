var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 1,
			deckDescriptions: [
				{
					amt: 2,
					names: {
						"Swamp": 2
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "\n"
				         +"1 swamp [pca]<142>\n"
				         +"1 swamp\n",
				sidelist: "",
				backURL: '',
				hiddenURL: '',
				name: '',
				coolify: false,
				artify: true,
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