var tests = [
	'allbolts',
	'characters',
	'twofaced',
	'hardnames',
	'sideboard',
	'tokens',
	'emblems',
	'transforms'
];

if(process.argv && process.argv.length > 2){
	tests = new Array(process.argv.length-2);
	for(var i = 0; i < tests.length; i++){
		tests[i] = process.argv[i+2];
	}
}

var testRoutines = new Array(tests.length);
for(var i = 0; i < testRoutines.length; i++){
	testRoutines[i] = require('./test_' + tests[i] + '.js');
}

var results = {
	passed: 0,
	failed: 0
};

function TestCompleted(index){
	return function(result){
		console.log('Test['+tests[index]+']: ' + (result ? 'passed' : 'failed'));
		if(result)results.passed++;
		else results.failed++;
		RunTest(index+1);
	}
};

function RunTest(index){
	if(index >= tests.length){
		console.log('Done running tests!');
		console.log(results.failed + ' tests failed!');
		console.log(results.passed + ' tests passed!');
	}else{
		testRoutines[index].RunTest(TestCompleted(index));
	}
};

RunTest(0);