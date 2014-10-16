JSLIB.depend('lightbox', ['dom.js', 'progressive.js', 'popup.js'], function(dom, progressive, popup) {
	var lightbox = this;
    var logger = this.logger;

	var imageBase = this.dirname(this.path);

	var closePrevious;

    function button(action, text) {
        var button = dom.button(action, text);
        button.className = text.replace(/ /g, '');
        return button;
    }

    this.LightBox = function LightBox(config) {
	    var allButtons = {};
	    var actionButtons = [];

		var className = config['className'] || 'default-lightbox';

	    var helpSource;
	    var helpVisible = config['helpVisible'] || 'no';
	    var actionOpen;
	    var actionClose;
		var actionCancel;
	    var actionReset;
	    var actionDefault = null;
	    var btnDefault;

	    var active = false;

	    var scrollMode = config['scrollmode'] ||'';
	    var honourCR = config['honourCR'] || 'N';

	    var actionCfg = config['actions'];
	    if (actionCfg) {
		    for (var k in actionCfg) {
				switch (k) {
				case 'Help':
					(function() {
						helpSource = actionCfg[k];
					})();
					break;

				case 'Open':
					(function() {
						var v = actionCfg[k];
						if (typeof v == 'function') {
							actionOpen = v;
						}
					})();
					break;

				case 'Close':
					(function() {
						var v = config[k];
						if (typeof v == 'function') {
							actionClose = v;
						}
					})();
					break;

				case 'Cancel':
					(function() {
						var v = actionCfg[k];
						if (typeof v == 'function') {
							actionCancel = v;
						}
					})();
					break;

				case 'Reset':
					(function() {
						var v = actionCfg[k];
						if (typeof v == 'function') {
							actionReset = v;
						}
					})();
					break;

				default:
					(function() {
						var action = actionCfg[k];
						if (typeof action == 'function') {
							var btn = button(function() { onAction(action) }, k);
							allButtons[k] = btn;
							if (actionDefault == null) {
								actionDefault = action;
								btnDefault = btn;
							}
							actionButtons.push(btn);
						}
					})();
					break;
				}
		    }
	    }

	    var helpDiv = dom.styleDiv({ marginTop:"10px" }, dom.el('hr'), (typeof helpSource == 'function') ? 'dummy' : helpSource);
	    var btnHelp = allButtons['Help'] = button(onHelp, (helpVisible == 'no') ? 'Show Help' : 'Hide Help');
	    btnHelp.style.display = helpSource ? 'inline' : 'none';

	    var btnReset = allButtons['Reset'] = button(onReset, 'Reset');
	    btnReset.style.display = (actionReset && actionButtons.length > 0) ? 'inline' : 'none';

	    var btnClose = allButtons['Close'] = button(onClose, 'Close');
	    var btnCancel = allButtons['Cancel'] = button(onCancel, 'Cancel');
	    if (actionCancel) {
		    btnClose.style.display = 'none';
		    btnCancel.style.display = 'inline';
	    }
		else {
		    btnClose.style.display = 'inline';
		    btnCancel.style.display = 'none';
	    }
	    
	    function onClose() {
		    close();
	    }

        function onCancel() {
	        close();
			actionCancel && actionCancel();
        }

	    function onReset() {
		    actionReset && actionReset();
	    }

	    function onAction(action) {
			close();
		    action && action();
	    }

	    function onHelp() {
		    if (helpSource) {
			    if (typeof helpSource == 'function') {
					helpSource();				    
			    }
			    else {
				    helpDiv.style.width = contentDiv.offsetWidth + 'px';
				    if (helpDiv.style.display == 'block') {
					    helpDiv.style.display = 'none';
					    btnHelp.innerHTML = 'Show Help';
				    }
				    else {
					    helpDiv.style.display = 'block';
					    btnHelp.innerHTML = 'Hide Help';
				    }
			    }
		    }
	    }

	    function onAccept() {
		    if (actionDefault) {
			    onAction(actionDefault);
		    }
		    else {
			    onCancel();
		    }
	    }

	    function onKeypress(e) {
		    if (active) {
				switch (e.keyCode) {
				case dom.keycode.Escape:
					onCancel();
					break;

				case dom.keycode.Enter:
					if ((honourCR == 'Y') && actionDefault) {
						onAction(actionDefault);
					}
					break;

			    default:
			        break;
				}
		    }
	    }
		
	    var contentDiv = this.content = dom.classDiv('lightbox-content', config['content']);

		var xButton = dom.floatDiv('right', dom.styleSpan({padding:'0 20px'}, dom.img(imageBase + 'icon_close.png')));
		dom.onclick(xButton, onCancel);

		var titleHolder = dom.div();
		var titleDiv = dom.styleDiv({fontSize:'120%',fontWeight:'bold',textAlign:'center'}, xButton, titleHolder);
        
        var buttons = dom.styleDiv({textAlign:'center'});
        if (actionButtons.length > 0) {
	        dom.add(buttons, actionButtons);
        }
	    dom.add(buttons, btnReset, btnClose, btnCancel, btnHelp);

        
	    var scrollArea = dom.styleDiv({overflow:'auto'}, contentDiv);
	    //var container = dom.styleDiv({overflow:'auto'},titleDiv, contentDiv, helpDiv, buttons);
	    var container = dom.div(titleDiv, scrollArea, buttons, helpDiv);

		var containerBox = dom.classDiv('lightbox-container', container);

        var verticalAlign = dom.table(dom.style(dom.tbody(dom.tr(dom.style(dom.td(containerBox),{verticalAlign:'middle'})))));
        verticalAlign.style.margin = '0px auto';
        verticalAlign.style.height = '100%';

        var tableHolder = dom.styleDiv({position:'absolute',left:0,top:0,bottom:0,right:0,width:'100%',height:'100%',textAlign:'center'},verticalAlign);

        var back = dom.styleDiv({backgroundColor:'#000',opacity:0.7,position:'absolute',left:0,top:0,bottom:0,right:0,width:'100%',height:'100%'});

        var fixed = dom.styleDiv({zIndex:2000,left:0,top:0,bottom:0,right:0,width:'100%',height:'100%'}, back, tableHolder);
        fixed.className = 'fixed';
        

		var holder = dom.classDiv(className,fixed);

	    dom.onkeypress(document, onKeypress);
		
		var limits = [];

		// Limit the size of element in a lightbox to the size of size of the window minus the specified excessWidth and excessHeight
		// Sets max-width, so doesn't work in some browsers
		this.limit = function (elt, excessWidth, excessHeight) {
			elt.style.overflow = 'auto';
			limits.push(function(width, height) { dom.setMaxWidth(elt, width - excessWidth); dom.setMaxHeight(elt, height - excessHeight); })
		};

		// Fix the size of an element in a lightbox to the size of size of the window minus the specified excessWidth and excessHeight
		this.fix = function (elt, excessWidth, excessHeight) {
			if (elt.content) {
				elt = elt.content;
			}
			elt.style.overflow = 'auto';
			limits.push(function(width, height) { /*logger.log('Set width, height', width, height, excessWidth, excessHeight);*/ elt.style.width = (width - excessWidth) + "px"; elt.style.height = (height - excessHeight) + "px"; })
		};

	    if (scrollMode == 'content') {
			this.limit(scrollArea, 10, 150);
	    }
	    else {
		    this.limit(container, 50, 50);
	    }

		var setTitle = this.setTitle = function(title) {
			dom.replace(titleHolder, (title && title != '') ? title : 'QuickGO');
		};

		setTitle(config['title']);

	    var setButtonState = this.setButtonState = function(which, enabled) {
		    function setState(btn, enabled) {
			    if (btn) {
				    btn.disabled = !enabled;
			    }
		    }

		    if (typeof which == 'string') {
				setState(allButtons[which], enabled);
		    }
		    else {
				for (var i = 0; i < which.length; i++) {
					setState(allButtons[which[i]], enabled);
				}
		    }
	    };

	    var showButton = this.showButton = function(which, visible) {
		    function show(btn, visible) {
		        if (btn) {
			        btn.style.display = visible ? 'inline' : 'none';
		        }
		    }

		    if (typeof which == 'string') {
				show(allButtons[which], visible);
		    }
		    else {
				for (var i = 0; i < which.length; i++) {
					show(allButtons[which[i]], visible);
				}
		    }
	    };

	    var show = this.show = function() {
			if (closePrevious) {
				closePrevious();
			}
			closePrevious = onCancel;

			var availableWidth = window.innerWidth || document.documentElement.clientWidth;
			var availableHeight = window.innerHeight || document.documentElement.clientHeight;

			for (var i = 0; i < limits.length; i++) {
				limits[i](availableWidth, availableHeight);
			}

		    if (actionOpen) {
			    actionOpen();
		    }
			document.body.style.overflow = 'hidden';
		    popup.setAbsoluteMode(true);

		    dom.add(document.body, holder);
		    helpDiv.style.width = contentDiv.offsetWidth + 'px';
		    helpDiv.style.display = (helpVisible == 'yes') ? 'block' : 'none';
		    (btnDefault ? btnDefault : btnClose).focus();
		    active = true;
		};

	    var close = this.close = function() {
		    if (actionClose) {
			    actionClose();
		    }

		    dom.remove(holder);
			active = false;
			closePrevious = null;
			document.body.style.overflow = 'auto';
		    popup.setAbsoluteMode(false);
	    };

	    var enhanceContent = this.enhanceContent = function() {
		    progressive.enhance(contentDiv, 'a', 'lb-dismiss', function(elt) { elt.onclick = function() { close(); }; });
		    progressive.enhance(contentDiv, 'a', 'lb-cancel', function(elt) { elt.onclick = function() { onCancel(); }; });
		    progressive.enhance(contentDiv, 'a', 'lb-accept', function(elt) { elt.onclick = function() { onAccept(); }; });
	    };

	    enhanceContent();
    };

    this.enhanceLightboxes = function(className) {
        return function(node) {
            dom.onclick(dom.find(node,'span','open'), new lightbox.LightBox({ title:dom.find(node,'div','title'), content:dom.find(node,'div','content'), className:className }).show);
        };
    };
});
