var fs = require('fs');

var dir = '../decks';
var backupDir = 'decks';

if(!fs.existsSync(dir)){
	dir = backupDir;
}

if(fs.existsSync(dir)){
	var dates = {};
	var files = fs.readdirSync(dir);
	for(var filei in files){
		var file = files[filei];
		if(file == '.' || file == '..')continue;
		if(!file.match(/[a-z]{16}\.json/)) continue;
		var stats = fs.statSync(dir + '/' + file);
		var fileObj = {
			date:stats.ctime,
			file:dir+'/'+file
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
	}

	for(var year in dates){
		for(var month in dates[year]){
			for(var day in dates[year][month]){
				var dispYear = parseInt(year)+1900;
				var dispMonth = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][month];
				var dispDay = day < 10 ? '0'+day : day;
				console.log(dispYear + ' : ' + dispMonth + ' : ' + dispDay + ' : ' + dates[year][month][day]);
			}
		}
	}

}else{
	console.log('Couldn\'t find ' + dir);
}