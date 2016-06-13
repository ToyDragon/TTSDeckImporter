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
					amt: 3,
					names: {
						"Gideon, Ally Of Zendikar": 1,
						"Ajani Steadfast": 1,
						"Chandra, Roaring Flame": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "Gideon, Ally of Zendikar\n"
				         +"Ajani Steadfast\n"
				         +"Chandra, Roaring Flame",
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