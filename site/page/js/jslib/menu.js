JSLIB.depend('menu', ['dom.js', 'popup.js'], function menu(dom, popup) {
    var logger = this.logger;
    logger.log('menu lists');

    var currentMenu;

    var MenuItem = this.MenuItem = function(content, action) {
        this.content = content;
        this.action = action;
    };

    var Menu = this.Menu = function() {
        var items = [];
        var finished = null;
        var currentItem;
        var index = -1;

        this.empty = function() {
            currentItem = null;
            items = [];
            index = -1;
        };

        this.add = function(item) {
            var index = items.length;
            dom.onclick(item.content, function() { select(index); accept(); });
            dom.onmouseover(item.content, function() { select(index); });
            items.push(item);
        };

        function select(newIndex) {
            if (currentItem) {
	            currentItem.content.style.backgroundColor = "";
            }
            if (newIndex >= items.length) {
	            newIndex = items.length - 1;
            }
            logger.log("Select " + newIndex);
            if (newIndex < 0 ) {
	            newIndex = -1;
            }
            else {
                currentItem = items[newIndex];
                currentItem.content.style.backgroundColor = "#ccf";
            }

            index = newIndex;
        }

        function accept() {
            logger.log("Accept");
            if (index >= 0) {
                if (items[index].action() && (finished==null || finished())) {
	                close();
                }
                return false;
            }
            return true;
        }

        this.keyhandler = function(e) {
            if (e.keyCode == 38) {
	            select(index - 1);
            }
            else if (e.keyCode == 40) {
	            select(index + 1);
            }
            else if (e.keyCode == 13) {
	            return accept();
            }
            return true;
        };

        function close() {
            currentMenu = null;
        }

        this.open = function(finishedFunction) {
            finished = finishedFunction;
            currentMenu = this;
        };
    };

    dom.afterDOMLoad(function(){
        dom.onkeydown(document.body,function(e){
            return currentMenu ? currentMenu.keyhandler(e) : true;
        });
    });


    var MenuTablePopup = this.MenuTablePopup = function(header) {
        var tbody = dom.tbody();
        var table = dom.table(tbody);

        var menu = new Menu();
        var content = this.content = dom.div(header, table);
        //content.style.border='1px solid blue';

        var p = new popup.Popup(content);

        this.setId = function(id) {
            p.setId(id);
        };

        this.attach = function(target, opened, finished, anchor) {
            p.attachClick(target, anchor, function() {
                if (opened) {
	                opened(target);
                }
                menu.open(function(item){
                    return (!finished || finished(item)) && p.close();
                });
            });
            return target;
        };
        //table.style.border='1px solid red';

        this.add = function(item) {
            menu.add(item);
            dom.add(tbody, item);
            return this;
        };

        this.addRow = function(action) {
            var row = dom.addCells(dom.tr(),arguments,1);
            menu.add(new MenuItem(row, action));
            dom.add(tbody, row);
            return row;
        };

        this.empty = function() {
            dom.empty(tbody);
            menu.empty();
        };

		this.close = function() {
			p.close();
		};
    };

    this.InputMenuTablePopup = function(header) {
        var currentInput;
        var mtp = new MenuTablePopup(header);

        this.setId = function (id) {
            mtp.setId(id);
        };

        this.attach = function(input,opened,finished,anchor) {
            function openedInput() {
                currentInput = input;
                if (opened) {
	                opened();
                }
            }

            mtp.attach(input, openedInput, finished, anchor);
        };

        this.addValue = function(value) {
	        var args = Array.prototype.slice.call(arguments);
		    var row = dom.addCells(dom.tr(), [dom.styleSpan({ fontWeight:'bold', marginRight:'4px' }, args[1]), args.slice(2)]);
            mtp.add(new MenuItem(row, function() { currentInput.setValue(value); logger.log('Accept ' + value); return true; }));
            return row;
        };

	    this.addItem = function(item) {
			var row = dom.row(dom.span(item));
	        mtp.add(new MenuItem(row, function() { currentInput.setValue(item); logger.log('Accept ' + item); return true; }));
	        return row;
	    };

        this.empty = function() {
            mtp.empty();
        };

        this.close = function() {
			mtp.close();
        };
    };
});
