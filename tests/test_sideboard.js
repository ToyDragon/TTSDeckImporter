var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 1,
			deckDescriptions: [
				{
					amt: 1,
					names: {
						"Mountain": 1
					}
				},{
					amt: 1,
					names: {
						"Forest": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "Mountain",
				sidelist: "Forest",
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