JSLIB.depend('quickgoMain', [
	'jslib/dom.js', 'jslib/progressive.js', 'jslib/tabs.js', 'jslib/lightbox.js', 'jslib/remote.js','jslib/popup.js','jslib/menu.js','jslib/parameters.js',
	'quickgoSelection.js', 'quickgoHelp.js', 'quickgoAnnotation.js', 'quickgoChart.js', 'quickgoDialog.js', 'quickgoTermList.js', 'dynamicRearranger.js', 'quickgoHistory.js'
	], function(dom, progressive, tabs, lightbox, remote, popup, menu, parameters,
    qgSelection, qgHelp, qgAnnotation, qgChart, qgDialog, qgTermList, rearranger, qgHistory) {

    var logger = this.logger;
    logger.log('LOADING quickgoMain');

	function enhanceCoOccur(form) {
		var loadQueue = new remote.XHRQueue('GAnnotation',remote.getFormParameters(form));

		var holder = dom.div(dom.img('image/ajax-loader.gif'), dom.style(dom.span('Loading...'), { marginLeft:'10px' }));
		dom.replace(form, holder);

		loadQueue.get('', {}, loaded);

		function processResults(xhr, name) {
		    var root = dom.div();
		    root.innerHTML = xhr.responseText;
			return dom.find(root, name);
	    }

		function loaded(xhr) {
			var results = processResults(xhr, 'table');
			qgSelection.enhanceSelection(results);
			qgHelp.enhanceHelpLinks(results);
			dom.replace(holder, results);
		}
	}

    function enhanceSearchForm(form) {
        var searchInput = dom.find(form, 'input', 'autosearch');
	    var searchButton = dom.find(form, 'input', 'search');
        var searchQueue = new remote.XHRQueue('GSearch', { format:'mini' });
        var menuPopup = new menu.MenuTablePopup(dom.div());

        function makeItem(tr) {
            return new menu.MenuItem(tr, function() {
                var anchor = dom.find(tr, 'a', 'link');
                window.location = anchor.href;
            });
        }

        function loaded(xhr){
            var root = remote.getHTML(xhr);
            var options = dom.findAll(root, 'tr');

            for (var i = 0; i < options.length; i++) {
                qgSelection.enhanceSelection(options[i]);
                menuPopup.add(makeItem(options[i]));
            }
        }

        var defaultSearch = dom.findAll(dom.find(form, 'div', 'example-search'), 'div', 'search');

	    function cleanse(s) {
		    return s.replace(/[^A-Z0-9:\.\s\-\(\),/']/ig, ' ').replace(/^\s+|\s+$/g, '');
	    }

        function search() {
            menuPopup.empty();
            var v = cleanse(searchInput.value);
            if (v == '') {
                for (var i = 0; i < defaultSearch.length; i++) {
                    menuPopup.add(makeItem(defaultSearch[i]));
                }
            }
            else {
                searchQueue.get(null, { q:v }, loaded, null, 'Loading');
            }
        }

        var previousWait;

        function waitSearch() {
            logger.log("Wait search");
            if (previousWait) {
	            previousWait.cancel();
            }
            previousWait = dom.schedule(search, 500);
        }

        var initial = parameters.get('q');
	    var defaultText = 'Click for example search';
        
        if (!initial) {
	        searchInput.style.color = '#777';
        }

        dom.onclick(searchInput, function() {
            searchInput.style.color='#000';
            if (!initial && (searchInput.value == '' || searchInput.value == defaultText)) {
	            searchInput.value = '';
            }
            search();
        });

	    function onSearch(e) {
			dom.stop(e);

		    var v = cleanse(searchInput.value);
			if (v != '') {
				window.location = 'GSearch?q=' + v;
			}
	    }

	    dom.onclick(searchButton, onSearch);
	    
        logger.log('Search: ' + searchInput.value);

        menuPopup.attach(searchInput);

        new dom.Input(searchInput, initial||defaultText).onvaluechange(waitSearch);
    }

	function enhanceInfoPopup(elt) {
		var infoDiv = dom.find(elt, 'div', 'info');
		if (infoDiv) {
			var topicDiv = dom.find(infoDiv, 'div', 'info-topic');
			if (topicDiv) {
				var infoBox = new lightbox.LightBox({ title:topicDiv, content:infoDiv, scrollmode:'content' });
				elt.onclick = function () { infoBox.show(); };
			}
		}
	}

	function enhanceCollapsOMatic(elt) {
		function toggle(el, img) {
			el.style.display = (el.style.display == 'none') ? 'inline' : 'none';
			if (img) {
				img.src = (el.style.display == 'none' ? 'image/zoom_in.png' : 'image/zoom_out.png');
			}
		}

		var contentDiv = dom.find(elt, 'div', 'com-content');
		if (contentDiv) {
			var indicator = dom.find(elt, 'img', 'com-indicator');
			dom.onclick(elt, function() { toggle(contentDiv, indicator); });
		}
	}

    this.afterDOMLoad(function(){
        logger.log('enhance stuff');
        progressive.enhance(document.body, 'div', 'tabs-h2', tabs.enhanceTabs('h2'));
        progressive.enhance(document.body, 'div', 'tabs-h3', tabs.enhanceTabs('h3'));
		progressive.enhance(document.body, 'form', 'co_occur', enhanceCoOccur);
        progressive.enhance(document.body, 'form', 'search', enhanceSearchForm);
	    progressive.enhance(document.body, 'i', 'info-popup', enhanceInfoPopup);
	    progressive.enhance(document.body, 'div', 'collaps-o-matic', enhanceCollapsOMatic);
    });
});

// global functions
window.rewriteURL = function() {
	var locationSplit = (''+window.location).split('?');
	if (locationSplit.length > 1) {
		var queryHash = locationSplit[1].replace('#', '&');
		var newLocation = locationSplit[0] + '#' +queryHash;
		window.location.replace(newLocation);
	}
};

