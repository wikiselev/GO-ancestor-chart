JSLIB.depend('quickgoDialog', ['jslib/dom.js', 'jslib/progressive.js', 'jslib/tabs.js', 'jslib/lightbox.js', 'jslib/remote.js', 'quickgoChart.js', 'jslib/basicdialog.js'], function(dom, progressive, tabs, lightbox, remote, quickgoChart, basicDlg) {
	var logger = this.logger;
	logger.log('LOADING quickgoDialog');

	this.DialogBoxManager = function DialogBoxManager() {
		this.getCheckboxSingle = function(root, name) {
			var checkbox = dom.find(root, 'input', name);
			return checkbox ? checkbox.checked : null;
		};

		this.getText = function(root, name) {
			var textbox = dom.find(root, 'input', name);
			return textbox ? textbox.value : null;
		};

		this.setText = function(root, name, value) {
			var textbox = dom.find(root, 'input', name);
			if (textbox) {
				textbox.value = value;
			}
		};

		this.getSelection = function(root, name) {
			var dropdown = dom.find(root, 'select', name);
			return dropdown ? dropdown.value : null;
		};

		this.makeInfoBox = function(elt) {
			var infoDiv = dom.find(elt, 'div', 'supplementary-info');
			if (infoDiv) {
				var topicDiv = dom.find(infoDiv, 'div', 'info-topic');
				if (topicDiv) {
					var infoBox = new lightbox.LightBox({ title:topicDiv, content:infoDiv, scrollmode:'content' });

					elt.onmouseover = function() {
						elt.style.textDecoration = 'underline';
						elt.style.cursor = 'pointer';
					};

					elt.onmouseout = function() {
						elt.style.textDecoration = 'none';
						elt.style.cursor = 'default';
					};

					function showInfo() {
						infoBox.show();
						quickgoChart.enhanceChart(infoDiv);
					}

					elt.onclick = showInfo;
				}
			}
		};

		this.Toolbar = function Toolbar(id) {
			var root = null;

			var toolbars = dom.findAll(document.body, 'div', 'toolbar');
			for (var i = 0; i < toolbars.length; i++) {
				var tb = toolbars[i];
				if (tb.getAttribute('id') == id) {
					root = tb;
					break;
				}
			}

            if (root!=null) {
                root.style.display='block';
            }

			var getButton = this.getButton = function(id) {
				if (root != null) {
					var buttons = dom.findAll(root, 'div', 'toolbar-button');
					for (var i = 0; i < buttons.length; i++) {
						var button = buttons[i];
						if (button.getAttribute('id') == id) {
							return button;
						}
					}
				}
				return null;
			};

			var setButtonImg = this.setButtonImg = function(btn, img) {
				if (btn != null) {
					dom.find(btn, 'img').src = img; 
				}
			};

			var setBackgroundColour = this.setBackgroundColour = function(rgb) {
				if (root != null) {
					dom.style(root, {backgroundColor:rgb});
				}
			};
		};
		
		function TabbedDialog(config) {
			var caption = config['caption'] || '';
			var tabControl = new tabs.Tabs({ name:caption, onSwitch:config['onSwitch'], stateless:config['stateless'] });

			var dialogBody = dom.styleDiv({}, tabControl);//removed height:100% to support IE
			var dialogBox = new basicDlg.BasicDialog(caption, dialogBody, config['buttons'], config['openAction']);

			var addTab = this.addTab = function(elt) {
				var captionDiv = dom.find(elt, 'div', 'tab-caption');
				if (captionDiv) {
					var tabCaption = captionDiv.firstChild.data;
					var idDiv = dom.find(elt, 'div', 'tab-id');
					var tabId = idDiv ? idDiv.firstChild.data : tabCaption;

					var tabContent = dom.find(elt, 'div', 'tab-content');
					if (tabContent) {
						tabControl.createTab(tabId, tabCaption, tabContent);
					}
				}
			};

			var addTabs = this.addTabs = function(elt) {
				var captionDefs = dom.findAll(elt, 'div', 'tab-definition');
				for (var i = 0; i < captionDefs.length; i++) {
					addTab(captionDefs[i]);
				}
			};

			var tabDefinitions = config['tabs'];
			if (tabDefinitions && tabDefinitions.length > 0) {
				for (var i = 0; i < tabDefinitions.length; i++) {
					addTabs(tabDefinitions[i]);
				}

				var limits = config['limits'];
				if (limits && limits['width'] && limits['height']) {
					dialogBox.fix(tabControl.content, limits['width'], limits['height']);
				}
			}
			else {
				dom.add(dialogBody, 'No data available');
			}

            this.findInputElements = dialogBox.findInputElements;
			this.getState = dialogBox.getState;

			dialogBox.saveInitialState();

			this.show = dialogBox.show;
			this.setButtonState = dialogBox.setButtonState;
			this.showButton = dialogBox.showButton;
			this.content = dialogBox.content;

			this.selectByName = function(name) {
				tabControl.selectByName(name);
			};

			this.selectByIndex = function(index) {
				tabControl.selectByIndex(index);
			};
		}

		var enableDialog = this.enableDialog = function(dlg, enabled) {
			function doNothing() {
				return false;
			}

			if (dlg) {
				var f = enabled ? function(e) {dlg['dlg'].show(e);} : doNothing;

				var anchors = dlg['anchors'];

				for (var i = 0; i < anchors.length; i++) {
					dom.findParent(anchors[i], 'a').onclick = f;
				}
			}
		};

		function bindDialogLinks(dlg, enabled, linkClass) {
			var anchors = dom.findAll(document.body, 'i', linkClass);
			var dlgInfo = { dlg:dlg, anchors:anchors, name:linkClass, findInputElements:dlg.findInputElements, getState:dlg.getState, content:dlg.content };

			enableDialog(dlgInfo, enabled);

			return dlgInfo;
		}

		this.makeDialog = function(config, enabled, linkClass, openAction) {
			var dlg = new basicDlg.BasicDialog(config['caption'], config['source'], config['buttons'], openAction);
			return dlg ? bindDialogLinks(dlg, enabled, linkClass) : null;
		};

		this.makeTabbedDialog = function(config, enabled, linkClass) {
			var dlg = new TabbedDialog(config);
			return dlg ? bindDialogLinks(dlg, enabled, linkClass) : null;
		};
	};

    this.afterDOMLoad(function(){
    });

});