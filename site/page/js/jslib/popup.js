JSLIB.depend('popup', ['dom.js'], function(dom) {
    var logger = this.logger;
    logger.log('dynamically displayed and positioned popup divs');
    
	var absolutePositioning = false;

	this.setAbsoluteMode = function(flag) {
		absolutePositioning = flag;
	};

    var Popup = this.Popup = function(contents) {
        var popup = this;

        function Container(contents) {
            var children = {};
            
            var replace = this.replace = function(type, popup) {
                if (children[type]) {
	                children[type].close();
                }
                children[type] = popup;
            };
            
            dom.onclick(contents, function(event){ replace('click', null); dom.stopPropagation(event); });
            dom.onmouseover(contents, function(event){ replace('mouseover', null); dom.stopPropagation(event); });
        }
        
        this.autoWidth = false;
        this.above = false;

        var container = this.container = dom.styleDiv({ border:'1px solid #770', backgroundColor:'#ffc', padding:'5px', position:'absolute', zIndex:'2000' }, contents);
        container.container = new Container(container);

        this.setId = function(id) {
            container.id = id;
        };
            
        var open = this.open = function(anchor, type) {
            if (anchor.content) {
	            anchor = anchor.content;
            }
            var parent = anchor;

            if (!document.body.container) {
	            document.body.container = new Container(document.body);
            }
            
            while (parent && !parent.container) {
	            parent = parent.parentNode;
            }

	        popup.anchor = anchor;
	        popup.parent = parent;

            parent.container.replace(type, popup);
	        parent.appendChild(container);
	        popup.adjustPosition();

            return false;
        };

	    var adjustPosition = this.adjustPosition = function() {
		    if (popup.anchor && popup.parent) {
			    var anchorPos = absolutePositioning ? dom.whereAbs(popup.anchor, popup.parent) : dom.where(popup.anchor, popup.parent);

				var left = anchorPos.left;
				if (left + container.offsetWidth > window.innerWidth) {
					left -= (left + container.offsetWidth - window.innerWidth)
				}
				if (left < 0) {
					left = 0;
				}
			    container.style.left = left + "px";

			    if (popup.above) {
				    container.style.top = anchorPos.top - container.offsetHeight + "px";
			    }
			    else {
				    container.style.top = anchorPos.bottom + "px";
			    }

			    if (popup.autoWidth) {
				    container.style.width = this.anchor.offsetWidth + 'px';
			    }
		    }
	    };
        
        this.close = function() {
            if (container.parentNode) {
	            container.parentNode.removeChild(container);
            }
            return true;
        };
        
        this.attachClick = function(target, anchor, opened) {
            dom.onclick(target,
                    function(event) {
                        open(anchor || target, 'click');
                        if (opened) {
	                        opened();
                        }
                        dom.stopPropagation(event);
                    });
            return target;
        };
        
        this.attachHover = function(target, anchor, opened) {
            dom.onmouseover(target,
                    function(event) {
                        open(anchor || target, 'mouseover');
                        if (opened) {
	                        opened();
                        }
                        dom.stopPropagation(event);
                    });
            return target;
        };
    };
    
    this.enhancePopup = function() {
        return function(node) {
            var pdiv = dom.find(dom.findParent(node, 'div', 'holder'), 'div', 'popup');
            pdiv.style.display = 'block';
            new Popup(pdiv).attachClick(node);
        };
    };
});
