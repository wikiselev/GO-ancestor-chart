JSLIB.depend('quickgoSelection', ['jslib/dom.js', 'jslib/progressive.js', 'jslib/tabs.js', 'jslib/lightbox.js', 'jslib/remote.js', 'jslib/parameters.js', 'jslib/popup.js', 'quickgoUtil.js'],
        function(dom, progressive, tabs, lightbox, remote, parameters, popup, util) {
    var logger = this.logger;
	logger.log('LOADING quickgoSelection');
    var quickgoSelection = this;

    var queue = new remote.JSONQueue('Selection', { format:'json' });

    function Term(id, name) {
        this.id = id;
        this.name = name;
    }

    var compressedTerms;
    var selectedTerms;

    function loaded(selectionInfo) {
        selectedTerms = [];
        var terms = selectionInfo["terms"];
        for  (var i = 0; i < terms.length; i++) {
	        //logger.log('loaded: ' + terms[i]['id']);
            selectedTerms.push(new Term(terms[i]['id'], terms[i]['name']));
        }
        changedSelection();
    }

    function loadSelection() {
        queue.request('', null, loaded);
    }

    var selectionListeners = [];

    quickgoSelection.registerSelectionListener = function(callback) {
        selectionListeners.push(callback);
        if (selectedTerms) {
	        callback(selectedTerms);
        }
    };

    function changedSelection() {
        var ids = [];
        for (var i = 0; i < selectedTerms.length; i++) {
	        ids.push(selectedTerms[i].id);
        }
        compressedTerms = parameters.compressTerms(ids);
        //logger.log("selection:", compressedTerms);
        for (var j = 0; j < selectionListeners.length; j++) {
	        selectionListeners[j](selectedTerms);
        }

	    bookmark.href = getURL();
    }

	function getURL(tab, extra) {
		return 'GMultiTerm?a=' + compressedTerms + '&tab=' + (tab || 'edit-terms') + remote.encodeParameters(extra);
	}

	function onCompare() {
		window.location = getURL('chart', { c:'' });
	}

    function onUse() {
        window.location = getURL('edit-terms');
    }

	function onAnnotations() {
		window.location = getURL('annotations');
	}

	function onExport() {
		util.postRequest('Selection', { action:'export' });
	}

	function onEmpty() {
		if (confirm('Do you really want to empty your basket?')) {
			queue.request('', { empty:'true' }, loaded);
		}
	}

	function help() {
		var divHelp = dom.div();
		dom.add(divHelp, dom.style(dom.div('How to use the Term Basket'), { textAlign:'center', fontWeight:'bold', marginBottom:'10px' }));
		dom.add(divHelp, 'You can add a GO term to the Term Basket by clicking on the ', dom.img('image/basket_add.png'), ' icon that appears next to its identifier in QuickGO.', dom.el('p'));
		dom.add(divHelp, 'You can also add them by typing or pasting a list of identifiers into the text box above and clicking on the "Add terms" button.', dom.el('p'));
		dom.add(divHelp, 'You can use the terms that you collect in the Term Basket in several ways, for example:');
		dom.add(divHelp, dom.list('generate a view of how they relate to each other', 'use them as a GO slim'));
		dom.add(divHelp, 'Further information on GO slims can be found ', dom.link('http://www.ebi.ac.uk/QuickGO/GMultiTerm', 'here'));
		return divHelp;
	}

	var basketStatus = dom.span();
    var bookmark = dom.link('GMultiTerm', dom.span(dom.img('image/nav/paging_link.png'), 'Bookmarkable link'));
	var linkSpan = dom.span(' (', bookmark, '):');
	var statusDiv = dom.style(dom.div(basketStatus, linkSpan), { marginBottom:'10px' });

    var selectionList = dom.tbody();
    var selectionTable = dom.table(selectionList);
	var tableDiv = dom.div(selectionTable);

	var inputBox = dom.textarea('', 40, 2);

	function addTerms() {
		var ids = util.superSplit(inputBox.getValue());
		var idList = '';
		for (var i = 0; i < ids.length; i++) {
			if (ids[i].match(/GO:\d{7}/)) {
				if (idList != '') {
					idList += ',';
				}
				idList += ids[i];
			}
		}

		queue.request('', { id:idList }, loaded);
		inputBox.setValue('');
	}

	var addButton = dom.button(addTerms, 'Add terms');
	var inputDiv = dom.div(dom.el('hr'), dom.style(dom.div('Enter a list of terms to be added to your basket:'), { marginBottom:'5px' }), dom.table(dom.row(inputBox), dom.row(addButton)), dom.el('hr'));

    var basketDialog = new lightbox.LightBox({ title:dom.div('Term Basket'), content:dom.div(statusDiv, tableDiv, inputDiv), actions:{ 'Use terms':onUse, 'Display terms in Ancestor Chart':onCompare, 'Find annotations':onAnnotations, 'Export terms':onExport, 'Empty':onEmpty, 'Help':help() }, helpVisible:'yes' });
    var divConfirmation = dom.div();
    var popupConfirmation = new popup.Popup(divConfirmation);

    function enhanceSelectionLink(elt) {
        var count = dom.find(elt, 'span', 'count');

        function termRow(index) {
            var img = dom.img('image/delete.png');
            var id = selectedTerms[index].id;
	        var anchor = dom.link('GTerm?id=' + id, id);
	        makeRemoveLink(img, id);
            return dom.row(img, anchor, dom.style(dom.span(selectedTerms[index].name), { marginLeft:"10px" }));
        }

        function changed() {
            dom.empty(selectionList);
			var termCount = selectedTerms.length;
            for (var i = 0; i < termCount; i++) {
	            dom.add(selectionList, termRow(i));
            }
            dom.replace(count, ': ' + termCount);

	        if (termCount == 0) {
		        dom.replace(basketStatus, 'Your basket is empty.');
		        linkSpan.style.display = 'none';
	        }
	        else {
		        dom.replace(basketStatus, 'Your basket contains the following term(s)');
		        linkSpan.style.display = 'inline';
	        }

	        basketDialog.setButtonState(['Use terms', 'Display terms in Ancestor Chart', 'Find annotations', 'Export terms', 'Empty'], termCount > 0);
        }
        quickgoSelection.registerSelectionListener(changed);

        dom.onclick(dom.findParent(elt, 'div', 'toolbar-button'), function(e) { dom.stopDefault(e); basketDialog.show(); });

        elt.style.visibility = 'inherit';
    }

    function enhanceSelectionAdd(elt) {
        var anchor = dom.findParent(elt, 'a');
	    var id = parameters.parseHREF(anchor.href)['id'];
	    makeAddLink(anchor, id);

        elt.style.visibility = 'inherit';
    }

    function confirmSelection(elt, id) {
        dom.replace(divConfirmation, id + " added to basket");
        popupConfirmation.open(elt, 'mouseover');
    }

	var makeSelectableGoTerm = this.makeSelectableGoTerm = function(id) {
		var img = dom.img('image/basket_add.png');
		quickgoSelection.makeAddLink(img, id);
		var divTerm  = dom.styleDiv({ whiteSpace:'nowrap' }, img, dom.span(' ', dom.link('GTerm?id=' + id, id), ' '));
		dom.addList(divTerm, arguments, 1);
		return divTerm;
	};

	var makeAddLink = this.makeAddLink = function(elt, id) {
        function addTerm(e) {
            dom.stop(e);
            queue.request('', { id:id }, loaded); confirmSelection(elt, id);
        }

		dom.onclick(elt, addTerm);
        var p = new popup.Popup("Add term to basket");
        p.attachHover(elt);
        dom.onclick(p.container, addTerm);
	};

	var makeRemoveLink = this.makeRemoveLink = function(elt, id) {
        function removeTerm(e) {
            dom.stop(e);
            queue.request('', { remove:id }, loaded);
        }

		dom.onclick(elt, removeTerm);
        var p = new popup.Popup("Remove term from basket");
        p.attachHover(elt);
        dom.onclick(p.container, removeTerm);
	};

	function enhanceObsolete(elt) {
		new popup.Popup("Term is obsolete and not used for annotation; click for more information.").attachHover(elt);
	}

	var enhanceSelection = this.enhanceSelection = function(node) {
		progressive.enhance(node, 'i', 'selection', enhanceSelectionAdd);
		progressive.enhance(node, 'i', 'obsolete', enhanceObsolete);
	};

    this.afterDOMLoad(function(){
        progressive.enhance(document.body, 'i', 'selectionlink', enhanceSelectionLink);
        enhanceSelection(document.body);
        loadSelection();
    });
});

