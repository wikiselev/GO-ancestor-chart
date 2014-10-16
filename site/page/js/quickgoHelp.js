JSLIB.depend('quickgoHelp', ['jslib/dom.js', 'jslib/progressive.js', 'jslib/tabs.js', 'jslib/lightbox.js', 'jslib/remote.js', 'quickgoDialog.js'], function(dom, progressive, tabs, lightbox, remote, dialogs) {
    var logger = this.logger;
	logger.log('LOADING quickgoHelp');

	var dbm = new dialogs.DialogBoxManager();

	var helpText = dom.div();
	var helpBox = new lightbox.LightBox({ title:dom.div('QuickGO Help'), content:helpText });
	helpBox.fix(helpText, window.innerWidth/4, window.innerHeight/4);

	var xhrq = new remote.XHRQueue("reference.html");
	var status = new remote.XHRStatusWrapper(new remote.XHRParameters(xhrq, '', { format:'raw' } ), 'Loading QuickGO Reference');

	var prevTopic;
	
	var h2Style = { fontSize:'16px', backgroundColor:'#6a6', color:'#fff' };
	var h3Style = { fontSize:'12px', fontStyle:'italic', backgroundColor:'transparent' };
	var highlightStyle = { fontSize:'12px', fontStyle:'italic', backgroundColor:'#ff0' };

	function helpLoaded(xhr) {
		helpText.innerHTML = xhr.responseText;

		var headers = dom.findAll(helpText, 'h2');
		for (var i = 0; i < headers.length; i++) {
			dom.style(headers[i], h2Style);
		}

		headers = dom.findAll(helpText, 'h3');
		for (i = 0; i < headers.length; i++) {
			dom.style(headers[i], h3Style);
		}

		helpBox.enhanceContent();
	}

	function helpUnavailable() {
		dom.add(helpText, 'QuickGO help is not available.');
	}

	status.request(null, '', null, null, helpLoaded, helpUnavailable);

	function enhanceHelpLink(elt) {
		function showHelp() {
			var topic = elt.getAttribute('topic');
			if (prevTopic) {
				dom.style(prevTopic, h3Style);
			}

			var topicDiv = dom.find(helpText, 'div', topic);
			dom.style(topicDiv, highlightStyle);
			prevTopic = topicDiv;

			helpBox.show();
			topicDiv.scrollIntoView();
		}

		elt.onclick = showHelp;
	}

	var enhanceHelpLinks = this.enhanceHelpLinks = function(root) {
		progressive.enhance(root, 'div', 'context-help', enhanceHelpLink);
		progressive.enhance(root, 'span', 'context-help', enhanceHelpLink);
		progressive.enhance(root, 'div', 'info-definition', dbm.makeInfoBox);
		progressive.enhance(root, 'span', 'info-definition', dbm.makeInfoBox);
	};

    this.afterDOMLoad(function(){ enhanceHelpLinks(document.body); });
});
