var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 2,
			deckDescriptions: [
				{
					amt: 4
				},{
					amt: 4,
					names: {
						"Civilized Scholar": 1,
						"Chandra, Fire Of Kaladesh": 1,
						"Chosen Of Markov": 1,
						"Hanweir Militia Captain": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "Civilized Scholar\n"
				         +"Chandra, Fire of Kaladesh\n"
				         +"Chosen of Markov\n"
				         +"Hanweir Militia Captain",
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