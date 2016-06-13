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
						"Hangarback Walker": 1,
						"Secure The Wastes": 1,
						"Nissa, Voice Of Zendikar": 1,
						"Linvala, The Preserver": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "\n"
				         +"Hangarback Walker\n"
				         +"Secure the Wastes\n"
				         +"Nissa, Voice of Zendikar\n"
				         +"Linvala, the Preserver",
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