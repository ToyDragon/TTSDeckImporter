var tester = require('./frogtowntester.js');

exports.RunTest = function(cb){
	var expectedOutput = {
		success: true,
		successData: {
			amtDecks: 1,
			deckDescriptions: [
				{
					amt: 28,
					names: {
						"Alive/well": 1,
						"Armed/dangerous": 1,
						"Night/day": 1,
						"Fire/ice": 1,
						"Catch/release": 1,
						"Ready/willing": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "alive/well\n"
				         +"alive//well\n"
				         +"3x Armed // Dangerous\n"
				         +"20 night/day\n"
				         +"fire/ice\n"
				         +"catch/release\n"
				         +"ready/willing",
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