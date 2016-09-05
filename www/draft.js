$(document).ready(function(){
	$.get('/sets', function(dataraw, status){
		var data = JSON.parse(dataraw);
		var expansions = {};
		var other = {};
		if(Array.isArray(data) && data.length > 0){
			data.forEach(function(item){
				console.log(item);
				if(item.type=='core' || (item.type=='expansion' && !item.block)){
					item.type = 'expansion';
					item.block = 'Core';
				}
				if(item.type=='expansion'){
					if(!expansions[item.block])expansions[item.block] = [];
					expansions[item.block].push(item);
				}else{
					if(!other[item.type])other[item.type] = [];
					other[item.type].push(item);
				}
			});
			for(var expansion in expansions){
				var block = expansions[expansion][0].block?expansions[expansion][0].block.replace(/[ ']/g,''):'other';
				console.log('new block ' + block);
				$('select').append($('<optgroup>', {label: expansion,style:'font-size:80%;',id:'groupBlock'+block}));
				expansions[expansion].forEach(function(set){
					$('#groupBlock'+block).append($('<option>', {value: set.name,style:'font-size:70%;'}).text(set.name));
				});
			}
			for(var type in other){
				$('select').append($('<optgroup>', {label: type,style:'font-size:80%;',id:'groupOthers'+type}));
				other[type].forEach(function(set){
					$('#groupOthers'+type).append($('<option>', {value: set.name,style:'font-size:70%;'}).text(set.name));
				});
			}
		}
	});

	$('#generate').click(function(){
		var set = $('select').val().replace(new RegExp('\'','g'),'');
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

			$.ajax({
				type: 'POST',
				url: '/newdraft',
				data: reqobj,
				timeout: 10000,
			}).done(function(dataraw){
				var data = JSON.parse(dataraw);
				if(data.status === 0){
					window.location = '/draftresults.html?deck='+data.deckId;
				}else{
					console.log(data);
					var errMsg = data.errObj.message;

					console.log(data.errObj);
					$('#packErr').text(errMsg);

					$('body').removeClass('loading');
					$('#packErr').removeClass('hidden');
				}
			}).fail(function(){
				$('body').removeClass('loading');
				$('#packErr').removeClass('hidden');

				$('#packErr').text('Server is down :(');
			});
		}
	});
});
