$(document).ready(function(){
	$.get('/decks', function(dataraw, status){
		$('#decks').html('');
		var data = JSON.parse(dataraw);
		if(Array.isArray(data) && data.length > 0){
			data.forEach(function(item){
				var scrub = item.name.replace(/</g,'&lt;');
				$('#decks').append('<li><a href="/decks/'+item.uid+'.json" download="'+scrub+'.json">'+scrub+'</a></li>');
			});
		}else{
			$('#decks').append('<li>Unable to load decks!</li>');
		}
	});
});
