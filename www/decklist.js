$(document).ready(function(){
	$.get('/decks', function(dataraw, status){
		$('#decks').html('');
		var data = JSON.parse(dataraw);
		if(Array.isArray(data) && data.length > 0){
			data.forEach(function(item){
				$('#decks').append('<li><a href="/decks/'+item.uid+'.json" download="'+item.name+'.json">'+item.name+'</a></li>');
			});
		}else{
			$('#decks').append('<li>Unable to load decks!</li>');
		}
	});
});