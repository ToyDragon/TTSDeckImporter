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
						"Kongming, \"sleeping Dragon\"": 1,
						"Kongming, Sleeping Dragon": 1,
						"Who/what/when/where/why": 1,
						"Index": 1,
						"Vizzerdrix": 1,
						"Pang Tong, Young Phoenix": 1,
						"Pang Tong, \"young Phoenix\"": 1,
						"Ow": 1,
						"Ach! Hans, Run!": 1
					}
				}
			]
		}
	};

	var input = {
		req: {
			body: {
				decklist: "\n"
				         +"Kongming, \"Sleeping Dragon\"\n"
				         +"Kongming, Sleeping Dragon\n"
				         +"Who/What/When/Where/Why\n"
				         +"Index\n"
				         +"Vizzerdrix\n"
				         +"Pang Tong, Young Phoenix\n"
				         +"Pang Tong, \"Young Phoenix\"\n"
				         +"Ow\n"
				         +"Ach! Hans, Run!",
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