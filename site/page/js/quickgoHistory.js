JSLIB.depend('quickgoHistory', ['jslib/dom.js', 'jslib/progressive.js', 'jslib/remote.js', 'jslib/parameters.js', 'quickgoDialog.js'], function(dom, progressive, remote, parameters, dialogs) {
    var logger = this.logger;
	logger.log('LOADING quickgoHistory');

	var dbm = new dialogs.DialogBoxManager();

	function enhanceHistoryToolbar(elt) {
		var parent = dom.findParent(elt,'div');
		var optionsForm = dom.find(parent, 'form');

		initialiseOptions(parameters.parseHREF(window.location + ''));

		function initialiseOptions(defaults) {
			function setOption(which) {
				var v = defaults[which];
				if (v) {
					optionsForm.elements[which].value = v;
				}
			}
			setOption("id");
			setOption("what");
			setOption("limit");
			setOption("from");
			setOption("to");

			var v = defaults["period"];
			if (v) {
				var groups = /^([0-9]+)(d|w|m|y)$/.exec(v);
				optionsForm.elements["period_length"].value = groups[1];
				optionsForm.elements["period_unit"].value = groups[2];
			}
		}

		var toolbar = dom.find(parent, 'div', 'jsonly');
		if (toolbar) {
			toolbar.style.display = 'block';
		}

		function isPlausibleDate(text) {
			var groups = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/.exec(text);
			return (groups.length == 4) ? ((groups[2] > 0 && groups[2] < 13) && (groups[3] > 0 && groups[3] < 32)) : false;
		}

		function refreshHistory() {
			var options = remote.getFormParameters(optionsForm);

			var params = {};
			var v = options["from"];
			if (v) {
				if (isPlausibleDate(v)) {
					params["from"] = v;
				}
				else {
					alert("Invalid FROM date specified: " + v);
					return;
				}
			}
			v = options["to"];
			if (v) {
				if (isPlausibleDate(v)) {
					params["to"] = v;
				}
				else {
					alert("Invalid TO date specified" + v);
					return;
				}

			}
			v = options["period_length"];
			if (v) {
				if (v > 0) {
					params["period"] = v + options["period_unit"];
				}
				else {
					alert("Invalid PERIOD specified: " + v + options["period_unit"]);
					return;
				}
			}
			v = options["limit"];
			if (v) {
				if (v > 0) {
					params["limit"] = v;
				}
				else {
					alert("Invalid LIMIT specified: " + v);
					return;
				}
			}

			v = options["id"];
			if (v) {
				params["id"] = v;
			}

			v = options["what"];
			if (v) {
				params["what"] = v;
			}

			var encoded = remote.encodeParameters(params);
			window.location = "GHistory" + ((encoded.length > 0) ? "?" + encoded.substr(1) : "");
		}

		var optionsDialog = dbm.makeDialog({ caption:'History Options', source:dom.findParent(optionsForm, 'div'), buttons:{ label:'Submit', action:refreshHistory } }, true, 'options-link');
	}

	var enhanceHistory = this.enhanceHistory = function(root) {
		progressive.enhance(root, 'i', 'history-toolbar', enhanceHistoryToolbar);
	};

    this.afterDOMLoad(function(){ enhanceHistory(document.body); });
});
