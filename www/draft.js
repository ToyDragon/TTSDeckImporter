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
			//$('select').append($('<optgroup>', {label: 'Expansions', id:'groupExpansions'}));
			for(var expansion in expansions){
				var block = expansions[expansion][0].block?expansions[expansion][0].block.replace(/[ ']/g,''):'other';
				console.log('new block ' + block);
				$('select').append($('<optgroup>', {label: expansion,style:'font-size:80%;',id:'groupBlock'+block}));
				expansions[expansion].forEach(function(set){
					$('#groupBlock'+block).append($('<option>', {value: set.name,style:'font-size:70%;'}).text(set.name));
				});
			}
			//$('select').append($('<optgroup>', {label: 'Others', id:'groupOthers'}));
			for(var type in other){
				$('select').append($('<optgroup>', {label: type,style:'font-size:80%;',id:'groupOthers'+type}));
				other[type].forEach(function(set){
					$('#groupOthers'+type).append($('<option>', {value: set.name,style:'font-size:70%;'}).text(set.name));
				});
			}
			//$('select').append($('<option>', {value: item.name}).text(item.name));
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
			
			$.post('/newdraft', reqobj, function(dataraw, status){
				var data = JSON.parse(dataraw);
				if(data.status == 0){
					window.location = '/draftresults.html?deck='+data.name;
				}
				else{
					alert("Oops! Error creating packs. BFZ should be online soon!");
				}
				$('body').removeClass('loading');
			});
		}
	});
});
