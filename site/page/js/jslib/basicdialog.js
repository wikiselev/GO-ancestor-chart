JSLIB.depend('basicdialog', ['dom.js', 'progressive.js', 'lightbox.js'], function(dom, progressive, lightbox) {
    var logger = this.logger;

	this.BasicDialog = function BasicDialog(caption, dialogSource, buttons, onOpen) {
		var dialogHelp = dom.find(dialogSource, 'div', 'dialog-help');

		var initialState = [];
		var currentState = null;

		function getValue(obj) {
			return (obj.type == 'checkbox' || obj.type == 'radio') ? (obj.checked ? 'Y' : 'N') : obj.value;
		}

		function setValue(obj, value) {
			//logger.log('setValue - obj.name = ' + obj.name + '  obj.type = ' + obj.type + '  value = ' + value);
			if (obj.type == 'checkbox' || obj.type == 'radio') {
				dom.setCheckbox(obj, value == 'Y' ? 1 : 0);
			}
			else {
				obj.value = value;
			}
		}

		function superSplit(s) {
		    s = s.replace(/^[, \t\n]+|[, \t\n]+$/g, '');
		    return (s == '') ? new Array() : s.split(/[, \t\n]+/);
		}

		function restoreState(state) {
			if (state) {
				for (var i = 0; i < state.length; i++) {
					state[i]();
				}
			}
		}

		var config = {};
		
		if (dialogHelp) {
			config['Help'] = dialogHelp;
		}

		if (onOpen) {
			config['Open'] = onOpen;
		}

		function bindButton(btnDef) {
			var label = btnDef['label'];
			if (label) {
				config[label] = btnDef['action'];
			}
		}

		if (buttons) {
			if (buttons instanceof Array) {
				for (i = 0; i < buttons.length; i++) {
					bindButton(buttons[i]);
				}
			}
			else {
				bindButton(buttons);
			}

			if (!config['Reset']) {
				config['Reset'] = function() { restoreState(initialState); };
			}
			if (!config['Cancel']) {
				config['Cancel'] = function() { restoreState(currentState); };
			}
		}

		var dialogBox = this.dialogBox = new lightbox.LightBox({ title:dom.div(caption), content:dialogSource, actions:config });
		this.content = dialogBox.content;

		this.enhance = function(tagName, className, callback) {
			progressive.enhance(dialogSource, tagName, className, callback)
		};

		var inputElements = [];

		var findInputElements = this.findInputElements = function() {
            inputElements = [];

			var elts = dom.findAll(dialogSource, 'input');
			for (var i = 0; i < elts.length; i++) {
				inputElements.push(elts[i]);
			}

			elts = dom.findAll(dialogSource, 'textarea');
			for (i = 0; i < elts.length; i++) {
				inputElements.push(elts[i]);
			}

			elts = dom.findAll(dialogSource, 'select');
			for (i = 0; i < elts.length; i++) {
				inputElements.push(elts[i]);
			}

            return inputElements;
		};

		function saveState() {
			var state = [];

			findInputElements();
			for (var i = 0; i < inputElements.length; i++) {
				var obj = inputElements[i];

				if (obj.className == 'noauto' || obj.type == 'submit') continue;

				var value = getValue(obj);

				//logger.log("saveState: i = " + i + "  name = " + obj.name + "  type " + obj.type + "  value = [" + value + "]");

				// Use a self-executed anonymous function to induce scope
				(function() {
					var o = obj;
					var v = value;
					state.push(function() {setValue(o, v);});
				})();
			}

			return state;
		}

		this.getState = function() {
			var state = {};

			findInputElements();
		    for (var i = 0; i < inputElements.length; i++) {
		        var obj = inputElements[i];
		        if (obj.className == 'noauto' || obj.type=='submit') {
		            continue;
		        }

		        var value = obj.value;
			    if (obj.type == 'checkbox' || obj.type == 'radio') {
				    if (!obj.checked) {
					    continue;
				    }
				    else {
						value = value || '{null}';
				    }
			    }

		        var v = superSplit(value);
		        var n = obj.name;
		        var a = state[n];
			    state[n] = a ? a.concat(v) : v;
		    }

			return state;
		};

		var saveInitialState = this.saveInitialState = function() {
			initialState = saveState();
		};

		var show = this.show = function(evt) {
			if (evt) {
				dom.stop(evt);
			}

			currentState = saveState();
			dialogBox.show();
		};

		saveInitialState();

		this.fix = dialogBox.fix;
		this.setButtonState = dialogBox.setButtonState;
		this.showButton = dialogBox.showButton;
	}
});
