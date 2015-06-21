$(document).ready(function(){
	$.get('/sets', function(dataraw, status){
		var data = JSON.parse(dataraw);
		if(Array.isArray(data) && data.length > 0){
			data.forEach(function(item){
				console.log(item);
				$('select').append($('<option>', {value: item}).text(item));
			});
		}
	});

	function makeid(){
		var text = "";
		var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		for( var i=0; i < 6; i++ )
			text += possible.charAt(Math.floor(Math.random() * possible.length));

		return text;
	}

	$('#generate').click(function(){
		var set = $('select').val().replace(new RegExp(' ','g'), '_');
		var deckName = set+'_'+makeid();
		var n = $('#packCount').val();
		var error = false;
		if(n <= 0 || n > 24){
			$('#packCount').addClass('error');
			$('#packErr').text('Must be between 1 and 24');
			error = true;	
		}
		if(!error){
			$('body').addClass('loading');
			var reqobj = {};
			reqobj.set = set;
			reqobj.n = n;
			reqobj.deckName = deckName;
			
			$.post('/newdraft', reqobj, function(dataraw, status){
				var data = JSON.parse(dataraw);
				if(data.status == 0){
					window.location = '/draftresults.html?deck='+deckName;
				}
				$('body').removeClass('loading');
			});
		}
	});
});