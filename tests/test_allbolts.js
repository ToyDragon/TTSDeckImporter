var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 1,
			deckDescriptions: [
				{
					amt: 60,
					names: {
						"Lightning Bolt": 1,
						"Mountain": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "lightning bolt\n"
				         +"4 lightning bolt\n"
				         +"12 Lightning bolt\n"
				         +"3x lightning bolt\n"
				         +"20 lightning bolt\n"
				         +"2 Mountain\n"
				         +"18 Mountain",
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