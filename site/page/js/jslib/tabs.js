JSLIB.depend('tabs',['dom.js','hash.js'],
        function(dom,hash) {

    var logger=this.logger;
    logger.log('Tabbed layouts');

    var Tabs = this.Tabs = function(config) {
        var current;

	    var name = config['name'] || '';
	    var useHash = (config['stateless'] == undefined || !config['stateless']);
	    var f = config['onSwitch'];
	    var onSwitch = (f && typeof f == 'function') ? f : null;

        var Tab = this.Tab = function(tabName, title, content) {
            var contentElement = content;
            if (content.content) {
	            contentElement = content.content;
            }
            this.name = tabName;
            var tab = this.tab = dom.span(dom.classSpan(name + '-' + tabName, title));

            var thisTab = this;
            this.title = title;

            var select = this.select = content.show = function() {
                if (current == thisTab) {
	                return;
                }

                if (content.onshow) {
	                content.onshow();
                }

	            if (onSwitch) {
					onSwitch(thisTab, current);		            
	            }

                if (current) {
                    current.unselect();
                    if (useHash) {
	                    hash.set(name, tabName);
                    }
                }

                //logging.log('Select '+tab.innerHTML+' '+thisTab.tabs.current);
                contentElement.style.display = "block";
                tab.className = 'activetab';
                current = thisTab;
            };

            var unselect = this.unselect = function() {
                if (content.onhide) {
	                content.onhide();
                }
                //logging.log('unselect '+tab.innerHTML);
                contentElement.style.display = "none";
                tab.className = 'tab';
            };

            dom.onclick(tab,select);

            this.content=contentElement;
        };

	    var tabs = [];
        var selectedTab = useHash ? hash.get(name) : null;

        var tabarea = dom.classDiv('tabs');
        var tabcontent = this.tabcontent = dom.classDiv('tabcontent');
        //tabcontent.style.position = 'relative';

        var content = this.content = dom.div(tabarea, dom.classDiv("tabframe", tabcontent));

        this.add = function(t) {
	        tabs.push(t);
            dom.add(tabarea, t.tab);
            dom.add(tabcontent, t.content);
            //logging.log('add '+this.current);
            if (!current && selectedTab==null || selectedTab == t.name) {
	            t.select();
            }
	        else {
	            t.unselect();
            }
			return t;
        };

        this.createTab = function(name, title, content) {
            return this.add(new Tab(name, title, content));
        };

	    this.selectByName = function(tabName) {
			for (var i = 0, n = tabs.length; i < n; i++) {
				var tab = tabs[i];
				if (tab.name == tabName) {
					tab.select();
					break;
				}
			}
	    };

	    this.selectByIndex = function(tabIndex) {
		    if (tabIndex >= 0 && tabIndex < tabs.length) {
			    tabs[tabIndex].select();
		    }
	    };
    };

    this.enhanceTabs = function(dividerNode) {
        var countTabSets = 0;
        dividerNode = dividerNode.toUpperCase();
        return function(node) {
            countTabSets++;
            var tabs = new Tabs({ name:node.getAttribute('name')||countTabSets });
            var content = null;
            var child = node.firstChild;
            var count = 0;
            while (child) {
                var next = child.nextSibling;
                //logging.log('Next!',child.nodeName);
                if (child.nodeName.toUpperCase() == dividerNode) {
                    child.parentNode.removeChild(child);
                    var title = dom.span();
                    while (child.firstChild) {
	                    title.appendChild(child.firstChild);
                    }
                    count++;
                    var name = child.getAttribute('name')||count;
                    var tab = tabs.createTab(name, title, content = dom.div());
					if (child.className == 'selected') {
						tab.select();
					}
                }
                else if (content != null) {
                    content.appendChild(child);
                }

                child = next;
            }
            node.appendChild(tabs.content);
        };
    };
});
