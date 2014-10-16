JSLIB.depend('quickgoTermList',['jslib/dom.js','jslib/progressive.js','jslib/tabs.js','jslib/lightbox.js','jslib/remote.js','jslib/hash.js','jslib/parameters.js','quickgoSelection.js','quickgoChart.js','quickgoAnnotation.js'],
function(dom,progressive,tabs,lightbox,remote,hash,params,quickgoSelection,quickgoChart,quickgoAnnotation) {

    var logger = this.logger;
	logger.log('LOADING quickgoTermList');

	var queue=new remote.JSONQueue('GMultiTerm',{format:'json'});

    function TermInfo(container) {
        var chartThumbnail = dom.find(container, 'i', 'chartthumbnail');
        var termInfo = dom.find(container, 'div', 'terminfo');
        var chartLocation=dom.find(document.body, 'i', 'comparisonchart');
        var ifNoTerms = dom.find(document.body, 'div', 'if-no-terms');
        var displayOptionsForm = dom.find(document.body, 'form', 'displayOptions');
		var chart = this.chart = new quickgoChart.AncestryChart(displayOptionsForm);

        var displayToolbar=dom.find(dom.findParent(chartLocation,'div'),'div','jsonly');
        if (displayToolbar) {
			displayToolbar.style.display='block';
            chart.attachToolbar(displayToolbar);
        }

		dom.replaceElement(chartLocation, chart);
        dom.replaceElement(chartThumbnail, chart.thumbnail);

        var clickedTerm = this.clickedTerm = function(id, name) {
            var idLink = dom.link('GTerm?id=' + id, id);
	        if (termInfo) {
				dom.replaceContent(termInfo, idLink, ' ', name);		        
	        }
            chart.addRemoveTermAuto(id);
            if (ifNoTerms) {
	            ifNoTerms.style.display = 'none';
            }
        };

        this.makeTermLink = function(id) {
	        var img = dom.img('image/delete.png');
	        quickgoSelection.makeRemoveLink(img, id);

	        var link = dom.link('GTerm?id=' + id, id);
	        link.className = 'compare-' + id;

	        var divTerm  = dom.styleDiv({ whiteSpace:'nowrap' }, img, link);
	        dom.addList(divTerm, arguments, 1);
	        return divTerm;
        };

        this.registerTerm = function(id, elt) {
	        //chart.addChartTermListener(id, function(colour){ elt.style.backgroundColor = colour||'#fff'; });
	        chart.addChartTermListener(id, function(colour){ elt.style.backgroundColor = colour||'transparent'; });
        };

        this.addRemoveTerms = function(ids, add) {
	        if (termInfo) {
		        dom.empty(termInfo);
	        }
            if (ifNoTerms) {
	            ifNoTerms.style.display = 'none';
            }
            chart.addRemoveTerms(ids, add);
        };
    }

    function MultiTermAnnotationPage(annotationPage) {
        var termFilterInfo = dom.div();

        dom.replaceElement(dom.find(annotationPage,'div','choose-terms'),termFilterInfo);
        var compressedTermFormField = dom.find(annotationPage, 'input', 'selected-terms');
        dom.find(annotationPage, 'input', 'termUseSlim').checked = true;

        compressedTermFormField.value = hash.get('a');
        var annotationForm = new quickgoAnnotation.AnnotationForm(annotationPage);

        this.setTerms = function(count, compressed, selection) {
            compressedTermFormField.value = compressed;
            annotationForm.fetchAnnotations();

	        var divSummary = dom.style(dom.div(count + ' terms selected:'), { marginTop:'5px', marginBottom:'5px' });
	        var s = '';
	        for (var id in selection) {
				s += (id + '  ');		        
	        }
	        var divSelection = dom.div(s);
            dom.replaceContent(termFilterInfo, divSummary, divSelection);

        };
    }

	function enhanceTermList(anchor) {
		var holder = dom.findParent(anchor, 'div');

		var termTable = dom.find(holder, 'table', 'termList');
		var tbodies = dom.findAll(termTable, 'tbody');

		var termInfo = new TermInfo(dom.find(holder, 'div', 'termcomparison'));

        var multiTermAnnotationPage = new MultiTermAnnotationPage(dom.find(holder, 'div', 'annotation-page'));

		function makeAddAllButton(element) {
			function loadAll() {
				var rows = dom.findAll(element, 'tr');
				var ids = '';
				var sep = '';
				for (var i = 0; i < rows.length; i++) {
					var id = rows[i].id;
					if (id) {
						ids = ids + sep + id;
						sep = ', ';
					}
				}
				loadTerms(ids);
			}

			return dom.imgbutton(function(e){ dom.stop(e); loadAll(); }, 'image/edit_add.png');
		}

        function makeRemoveAllButton(element) {
            function removeAllTerms() {
                var rows = dom.findAll(element, 'tr');
                var ids = [];
                for (var i = 0; i < rows.length; i++) {
	                if (rows[i].id) {
		                ids.push(rows[i].id);
	                }
                }
                for (var j = 0; j < ids.length; j++) {
                    dom.remove(termMap[ids[j]]);
                    delete termMap[ids[j]];    
                }
                updatedTermList();
            }

	        return dom.imgbutton(function(e){ dom.stop(e); removeAllTerms(); }, 'image/edit_remove.png');
        }

        function makeSelectAllButton(element) {
            var add = true;

	        var img = dom.img('image/chart_add.png');

            function setButton() {
	            img.src = add ? "image/chart_add.png" : "image/chart_delete.png";
            }

            function selectAllTerms(event) {
                var rows = dom.findAll(element, 'tr');
                var ids = [];
                for (var i = 0; i < rows.length; i++) {
	                if (rows[i].id) {
		                ids.push(rows[i].id);
	                }
                }
                termInfo.addRemoveTerms(ids, add);
                add = !add;
                setButton();
            }

	        return dom.button(function(e){ dom.stop(e); selectAllTerms(); }, img);
        }

		function makeRemoveGlobalButton() {
		    function removeTermsGlobal() {
			    var ids = [];
			    for (var body = 0 in ontologyTbodies) {
				    var rows = dom.findAll(ontologyTbodies[body], 'tr');
				    for (var ixRow = 0, rowCount = rows.length; ixRow < rowCount; ixRow++) {
					    if (rows[ixRow].id) {
						    ids.push(rows[ixRow].id);
					    }
				    }
			    }

		        for (var i = 0, idCount = ids.length; i < idCount; i++) {
		            dom.remove(termMap[ids[i]]);
		            delete termMap[ids[i]];
		        }
		        updatedTermList();
		    }

			return dom.imgbutton(function(e){ dom.stop(e); removeTermsGlobal(); }, 'image/edit_remove.png');
		}

		function makeSelectGlobalButton() {
		    var add = true;

			var img = dom.img('image/chart_add.png');

		    function setButton() {
			    img.src = add ? "image/chart_add.png" : "image/chart_delete.png";
		    }

		    function selectTermsGlobal(event) {
			    var ids = [];
			    for (var body in ontologyTbodies) {
				    var rows = dom.findAll(ontologyTbodies[body], 'tr');
				    for (var ixRow = 0, rowCount = rows.length; ixRow < rowCount; ixRow++) {
					    if (rows[ixRow].id) {
						    ids.push(rows[ixRow].id);
					    }
				    }
			    }

		        termInfo.addRemoveTerms(ids, add);
		        add = !add;
		        setButton();
		    }

			return dom.button(function(e){ dom.stop(e); selectTermsGlobal(); }, img);
		}

		var ontologyTbodies = {};

		for (var i = 0; i < tbodies.length; i++) {
			var className= tbodies[i].className;
			if (className) {
				if (className == 'AllOntologies') {
					dom.add(dom.find(tbodies[i], 'td', 'term-remove-all'), makeRemoveGlobalButton());
					dom.add(dom.find(tbodies[i], 'td', 'term-select-all'), makeSelectGlobalButton());
				}
				else {
					ontologyTbodies[className] = tbodies[i];
					dom.add(dom.find(tbodies[i], 'td', 'term-remove-all'), makeRemoveAllButton(tbodies[i]));
					dom.add(dom.find(tbodies[i], 'td', 'term-select-all'), makeSelectAllButton(tbodies[i]));
				}
            }
		}

        var selectionTable = dom.find(holder, 'table', 'selection');
        var selectionList = dom.tbody();

		var caption = dom.td("Term Basket");
		caption.colSpan = "2";
		caption.style.fontWeight = "bold";
		var selectionHeader = dom.tr(dom.td(makeAddAllButton(selectionList)), dom.td(makeSelectAllButton(selectionList)), caption);

		dom.add(selectionTable, selectionHeader);
		dom.add(selectionTable, selectionList);

		function makeAddToChartButton(id, name) {
			var chart = termInfo.chart;

			var img = dom.img(chart.termInChart(id) ? 'image/chart_delete.png' : 'image/chart_add.png');
			var btn = dom.button(function(e){ dom.stop(e); termInfo.clickedTerm(id, name); }, img);

			function toggleState(colour) {
				img.src = (colour == null) ? 'image/chart_add.png' : 'image/chart_delete.png'
			}
			chart.addChartTermListener(id, toggleState);

			return btn;
		}

        function selectionTermRow(id, name) {
	        var addButton = dom.imgbutton(function(e){ dom.stop(e); loadTerms([id]); }, 'image/edit_add.png');

	        var tdName = dom.td(name);
	        termInfo.registerTerm(id, tdName);

	        var childRow = dom.row(addButton, makeAddToChartButton(id, name), termInfo.makeTermLink(id));
	        childRow.id = id;
	        dom.add(childRow, tdName);

            return childRow;
        }

        function refreshSelection(list) {
            dom.empty(selectionList);
            for (var i = 0; i < list.length; i++) {
                dom.add(selectionList, selectionTermRow(list[i].id, list[i].name));
            }
        }

        quickgoSelection.registerSelectionListener(refreshSelection);

		var termMap = {};
		var populateComparisonChart = false;

		function termRow(id, ontology, name, count) {
			if (termMap[id]) {
				return termMap[id];
			}
			else {
				count = count >> 0;

				var removeButton = dom.imgbutton(function(e){ dom.stop(e); removeTerm(id); }, 'image/edit_remove.png');
				
				var tdName = dom.td(name);
				termInfo.registerTerm(id, tdName);

				var row = dom.row(removeButton, makeAddToChartButton(id, name), quickgoSelection.makeSelectableGoTerm(id));
				dom.add(row, tdName);

				termMap[id] = row;

				var tbody = ontologyTbodies[ontology];
				row.count = count;
	            row.id = id;
				var reference = tbody.firstChild;
				while (reference && (reference.count == null || count < reference.count)) {
					reference = reference.nextSibling;
				}
				dom.insertBefore(tbody, row, reference);

				return row;
			}
		}

		function gotTermData(termData) {
			var terms = termData['terms'];
			var ids = [];
			for (var i = 0; i < terms.length; i++) {
				var info = terms[i]['info'];
				var name = info['name'];
				var id = info['id'];
				var count = terms[i]['count'];
				var ontology = info['ontology'];
				if (populateComparisonChart) {
					ids.push(id);
				}
				termRow(id, ontology, name, count);
			}
            updatedTermList();
			if (populateComparisonChart) {
				termInfo.addRemoveTerms(ids, true);
			}
        }

        var count = 0;

        function getCompressedTerms() {
            var ids = [];
            for (var termId in termMap) {
	            ids.push(termId);
            }
            count = ids.length;
            return params.compressTerms(ids);
        }        

        function updatedTermList() {
            var compressedTerms = getCompressedTerms();
            hash.set('a', compressedTerms);
            multiTermAnnotationPage.setTerms(count, compressedTerms, termMap);
		}

		function loadTerms(ids) {
            dom.show(termTable);
			queue.request('', {id:ids}, gotTermData);
		}

		function loadCompressedTerms(compressed) {
            dom.show(termTable);
			queue.request('', {a:compressed}, gotTermData);
		}

		function removeTerm(id) {
			termInfo.addRemoveTerms([id], false);
			dom.remove(termMap[id]);
            delete termMap[id];
            updatedTermList();
		}
		
		function enhanceBatchLink(input) {
			function onLoadTerms(e) {
				dom.stop(e);
				loadCompressedTerms(input.value);

				var row = dom.findParent(button, 'tr');
				if (row) {
					row.style.background = '#ff0';
				}
			}

			var button = dom.imgbutton(onLoadTerms, 'image/edit_add.png');
			dom.replaceElement(input, button);
		}

		var addBatchLinks = dom.findAll(holder, 'input', 'compressed');
		for (i = 0; i < addBatchLinks.length; i++) {
			enhanceBatchLink(addBatchLinks[i]);
		}

		var addAll = dom.find(holder, 'textarea', 'terms');
		var addAllButton = dom.find(holder, 'button', 'addterms');

		var hashParameter = hash.get('a');
		if (hashParameter) {
			loadCompressedTerms(hashParameter);
		}
		if (hash.get('c')) {
			populateComparisonChart = true;
		}

		dom.onclick(addAllButton, function(e){ dom.stop(e); loadTerms(addAll.value); });
	}

	this.afterDOMLoad(function(){        
        progressive.enhance(document.body, 'i', 'multiterm', enhanceTermList);
    });

	logger.log('query', window.location.search);
}
);

