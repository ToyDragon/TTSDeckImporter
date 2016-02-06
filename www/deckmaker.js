$(document).ready(function(){
	var options = ['Cool Basic','Sideboard','Commander'];
	var isActive = [true, false, false];
	var badLines = {};

	var isChrome = navigator.userAgent.toLowerCase().indexOf('chrome') > -1;

	$('#deckName').val('');

	$('.qualityButtons button').click(function(event){
		$('.qualityButtons button').removeClass('btn-info').addClass('btn-default');
		$(event.currentTarget).addClass('btn-info').removeClass('btn-default');
	});

	$('.toggleButton').click(function(event){
		var src = $(event.currentTarget);
		var active = src.hasClass('btn-success');
		var oldClass = active ? 'btn-success' : 'btn-default';
		var newClass = active ? 'btn-default' : 'btn-success';
		src.removeClass(oldClass).addClass(newClass);

		var btnText = src.text();

		for(var i = 0; i < options.length; i++){
			if(new RegExp(options[i]+'$').test(btnText)){
				isActive[i] = !active;
			}
		}
		UpdateSections();
	});
	
	$('#generate').click(function(){
		$('body').addClass('loading');
		var list = 'MAINBOARD\n'+$('.userlist.mainboard').val();
		if(isActive[1]){
			list += '\nSIDEBOARD\n'+$('.userlist.sideboard').val();
		}
		if(isActive[2]){
			list += '\nCOMMANDER\n'+$('.userlist.commander').val();
		}
		
		var backURL = $('#backURL').val().trim();
		var hiddenURL = $('#hideURL').val().trim();
		var deckName = $('#deckName').val().trim().length > 0?$('#deckName').val().trim():'frogtown_deck';
		var coolify = $('#coolify').hasClass('btn-success');
		var compression = $(".qualityButtons .btn-info").attr("value");
		
		var reqobj = {};
		reqobj.decklist = list;
		reqobj.backURL = backURL;
		reqobj.hiddenURL = hiddenURL;
		reqobj.deckName = deckName;
		reqobj.coolify = coolify;
		reqobj.compression = compression;

		console.log(compression);
		
		$.post('/newdeck', reqobj, function(dataraw){
			var data = JSON.parse(dataraw);
			if(data.status == 0){
				window.location = '/deck.html?deck='+data.name+'.json&name='+deckName;
			}else{
				console.log(data);
				badLines = {};
				var errMsg = data.errObj.message;
				$('body').removeClass('loading');
				$('.error').removeClass('hidden');
				if(data.errObj && data.errObj.badCards){
					for(var erri = 0; erri < data.errObj.badCards.length; erri++){
						badLines[data.errObj.badCards[erri]]=true;
					}
				}

				console.log(data.errObj);

				$('.error').text(errMsg);
				
				$('.userlist').each(function(i, src){ UpdateErrors($(src)); });
			}
		});
	});

	function UpdateErrors(src){
		src.siblings('.displaylist').val(src.val());
		var listi = 0;
		if(src.hasClass('sideboard'))listi=1;
		if(src.hasClass('commander'))listi=2;

		var errorval = '';
		var curLines = src.val().split('\n');
		console.log('updating errors');
		console.log(JSON.stringify(curLines));
		console.log(JSON.stringify(badLines));
		for(var i = 0; i < curLines.length; i++){
			if(badLines[curLines[i].trim()]){
				if(isChrome){
					errorval += curLines[i] + '\n';
				}else{
					errorval += '*\n'; 
				}
			}else{
				errorval += '\n';
			}
		}

		src.siblings('.errorlist').val(errorval);
		src.siblings('.displaylist,.errorlist').scrollTop(src.scrollTop());
	}

	$('textarea.userlist').bind('input propertychange', function(event) {
		var src = $(event.currentTarget);
		UpdateErrors(src);
	});

	$('textarea.userlist').bind('scroll', function(event) {
		var src = $(event.currentTarget);
		src.siblings('.displaylist,.errorlist').scrollTop(src.scrollTop());
	});

	function UpdateSections(){
		$(".optionalList").addClass("hidden");
		if(isActive[1]) $("#sideboard").removeClass("hidden");
		if(isActive[2]) $("#commander").removeClass("hidden");
		UpdateTextareas()
	}

	function UpdateTextareas(){
		if(isChrome){
			var boards = ['.mainboard','.sideboard','.commander'];
			for(var i = 0; i < boards.length; i++){
				var board = boards[i];

				var displaylist = $(board+'.displaylist');
				var errorlist = $(board+'.errorlist');
				var userlist = $(board+'.userlist');

				errorlist.width(userlist.width());
				errorlist.height(userlist.height());

				displaylist.width(userlist.width());
				displaylist.height(userlist.height());
			}
		}else{
			$('.displaylist').remove();
			$('.errorlist').addClass('notchrome').prop('disabled',true);

			var boards = ['.mainboard','.sideboard','.commander'];
			for(var i = 0; i < boards.length; i++){
				var board = boards[i];

				var errorlist = $(board+'.errorlist');
				var userlist = $(board+'.userlist');

				errorlist.width(userlist.width());
				errorlist.height(userlist.height());
			}
		}
	}

	UpdateTextareas();
});