function offsetAnchor() {
    if(location.hash.length !== 0 && window.location.hash != '#chooseLanguage') {
        window.scrollTo(window.scrollX, window.scrollY - 55);
    }
	console.log(window.location.hash);
}

$(window).on("hashchange", function () {
    offsetAnchor();
});

window.setTimeout(function() {
    offsetAnchor();
}, 1);