$(document).ready(function(){
	function getParameterByName(name) {
		var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
		return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
	}
	
	$('.deckname').html(getParameterByName('deck'));
	
	$.get('decks/'+getParameterByName('deck')+'.json', function(data, status){
		var deck = JSON.stringify(data);
		console.log(deck);
		$('#deckjson').html(deck);
		
		$('#decka').attr('href', 'decks/'+getParameterByName('deck')+'.json');
		$('#decka').attr('download', getParameterByName('deck')+'.json');
	});
});
