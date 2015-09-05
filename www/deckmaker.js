$(document).ready(function(){
	$('#deckName').val('');
	$('#frogtown').click(function(){
		$('.host-btn').removeClass('btn-danger');
		$('#frogtown').addClass('btn-danger');
	});
	
	$('#imgur').click(function(){
		$('.host-btn').removeClass('btn-danger');
		$('#imgur').addClass('btn-danger');
	});
	
	$('#generate').click(function(){
		if($('#deckName').val().trim().length == 0){
			$('#deckName').addClass('error');
			//location.href = "#deckName";
			$('html,body').animate({
				scrollTop: $('#deckName').offset().top - 125
			}, 500);
		}else if($('#decklist').val().trim().length > 0){
			$('body').addClass('loading');
			var useImgur = $('#imgur').hasClass('btn-danger');
			var list = 'MAINBOARD\n'+$('#decklist').val();
			if($('#toggleSideboard').prop('checked')){
				list += '\nSIDEBOARD\n'+$('#sidelist').val();
			}
			if($('#toggleCommander').prop('checked')){
				list += '\nCOMMANDER\n'+$('#commandername').val();
			}
			
			var backURL = 'default';
			var hiddenURL = 'default';
			var deckName = $('#deckName').val().trim().length > 0?$('#deckName').val().trim():'default';
			var coolify = $('#toggleBack').prop('checked');
			var compression;// = $('#toggleComp').prop('checked')?0.2:0.5;
			var selected = $("input[type='radio'][name='quality']:checked");
			if (selected.length > 0) {
				compression = selected.val();
			}
			
			if($('#toggleBack').prop('checked')){
				backURL = $('#backURL').val().trim().length > 0?$('#backURL').val().trim():'default';
			}
			if($('#toggleHidden').prop('checked')){
				hiddenURL = $('#hideURL').val().trim().length > 0?$('#hideURL').val().trim():'default';
			}
			
			
			var reqobj = {};
			reqobj.decklist = list;
			reqobj.imgur = useImgur;
			reqobj.backURL = backURL;
			reqobj.hiddenURL = hiddenURL;
			reqobj.deckName = deckName;
			reqobj.coolify = coolify;
			reqobj.compression = compression;
			
			$.post('/newdeck', reqobj, function(dataraw, status){
				var data = JSON.parse(dataraw);
				if(data.status == 0){
					console.log(status+':'+data.name);
					window.location = '/deck.html?deck='+data.name+'.json&name='+deckName;
				}else{
					console.log('/decks/'+data.name+'.json');
					$.get('/decks/'+data.name+'.json', {}, function(wrongdata, status){
						console.log('got it');
						console.log(wrongdata);
						$('body').removeClass('loading');
						$('#wrong').html('Can\'t find: '+JSON.stringify(wrongdata));
					});
				}
			});
		}
	});
	
	toggleSideboard();
	toggleCommander();
	toggleHidden();
	toggleBack();
});

function toggleSideboard(){
	var invisible = !$('#toggleSideboard').prop('checked');
	if(invisible)
		$('#sideboard').hide();
	else
		$('#sideboard').show();	
}
function toggleCommander(){
	var invisible = !$('#toggleCommander').prop('checked');
	if(invisible)
		$('#commander').hide();
	else
		$('#commander').show();	
}
function toggleBack(){
	var invisible = !$('#toggleBack').prop('checked');
	if(invisible)
		$('#back').hide();
	else
		$('#back').show();
	
	if($('#back').css("display") != 'none' || $('#hidden').css("display") != 'none'){
		$('#options').show();
		$('#hostingName').text('5. Choose Hosting Service');
		$('#generateName').text('6. Generate Deck File');
	}else{
		$('#options').hide();
		$('#hostingName').text('4. Choose Hosting Service');
		$('#generateName').text('5. Generate Deck File');
	}
}
function toggleHidden(){
	var invisible = !$('#toggleHidden').prop('checked');
	if(invisible)
		$('#hidden').hide();
	else
		$('#hidden').show();
		
	if($('#back').css("display") != 'none' || $('#hidden').css("display") != 'none'){
		$('#options').show();
		$('#hostingName').text('5. Choose Hosting Service');
		$('#generateName').text('6. Generate Deck File');
	}else{
		$('#options').hide();
		$('#hostingName').text('4. Choose Hosting Service');
		$('#generateName').text('5. Generate Deck File');
	}
}
function updateName(){
	if($('#deckName').val().trim().length > 0){
		$('#deckName').removeClass('error');
	}else{
		$('#deckName').addClass('error');
	}
}