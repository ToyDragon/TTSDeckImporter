var fs = require('fs');
var config = require('../settings.json');

var byMonth = false;
var countAll = false;

process.argv.forEach(function (val, index, array) {
	if(val === '-month' || val === '-m'){
		byMonth = true;
	}
	if(val === '-all' || val === '-a'){
		countAll = true;
	}
});

if(fs.existsSync(config.deckDir)){
	var total = 0;
	var dates = {};
	var files = fs.readdirSync(config.deckDir);
	for(var filei in files){
		var file = files[filei];
		if(file == '.' || file == '..')continue;
		if(!file.match(/\.json$/)) continue;
		if(!countAll && !file.match(/^[a-z]{16}\.json/)) continue;
		var stats = fs.statSync(config.deckDir + '/' + file);
		var fileObj = {
			date:stats.ctime,
			file:config.deckDir+'/'+file
		};
		if(stats.ctime.getHours() <= 6){
			stats.ctime.setDate(stats.ctime.getDate() - 1);
		}
		var year = stats.ctime.getYear();
		var month = stats.ctime.getMonth();
		var day = stats.ctime.getDate();
		dates[year] = dates[year] || {};
		dates[year][month] = dates[year][month] || {};
		dates[year][month][day] = dates[year][month][day]+1 || 1;
		total = total + 1;
	}

	for(var year in dates){
		for(var month in dates[year]){
			var monthTotal = 0;
			var dispMonth = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][month];
			var dispYear = parseInt(year)+1900;
			for(var day in dates[year][month]){
				var dispDay = day < 10 ? '0'+day : day;
				monthTotal += dates[year][month][day];
				if(!byMonth){
					console.log(dispYear + '\t' + dispMonth + '\t' + dispDay + '\t' + dates[year][month][day]);
				}
			}
			if(byMonth){
				console.log(dispYear + '\t' + dispMonth + '\t' + monthTotal);
			}
			
		}
	}
	console.log('Total: ' + total);

}else{
	console.log('Couldn\'t find ' + config.deckDir);
}
