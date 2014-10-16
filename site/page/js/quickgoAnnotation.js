JSLIB.depend('quickgoAnnotation', ['jslib/dom.js','jslib/remote.js','jslib/progressive.js','jslib/parameters.js', 'jslib/lightbox.js','jslib/table.js','quickgoSelection.js','quickgoChart.js','quickgoDialog.js', 'dynamicRearranger.js', 'quickgoUtil.js', 'jslib/basicdialog.js'],
        function (dom,remote, progressive, parameters, lightbox, table, quickgoSelection, quickgoChart, dialogs, rearranger, util, basicDlg) {

    var logger = this.logger;
	logger.log('LOADING quickgoAnnotation');

    function AnnotationForm(annotationPage) {
        var form = dom.find(annotationPage,'form','annotation-table');

        var annotationTableElement = dom.find(form,'table','annotation');
        annotationTableElement.parentNode.style.display = 'block';
		var annotationTable = new table.DynamicTable(annotationTableElement);

	    function getSpanText(root, name) {
		    var elt = dom.find(root, 'span', name);
		    return elt ? elt.firstChild.data : null;
	    }

	    function setSpanText(root, name, text) {
		    var elt = dom.find(root, 'span', name);
			if (elt) {
				elt.innerHTML = text;
			}
	    }

        var filterParameters = {};
	    var defaultFilterParameters = {};

        var statsTbody = dom.find(form, 'tbody', 'stats');
        var annotationTbody = dom.find(form, 'tbody', 'annotation');

        var xhrq = new remote.XHRQueue("GAnnotation", {embed:'html'});

        var annotationStatus = new remote.XHRStatusWrapper(new remote.XHRParameters(xhrq,'',{format:'sample'}),'Loading Annotation');
        var statsStatus = new remote.XHRStatusWrapper(new remote.XHRParameters(xhrq,'',{format:'stats'}),'Loading Statistics');

		function formatWithCommas(nStr){
			var x = (nStr + '').split('.');
			var x1 = x[0];
			var x2 = x.length > 1 ? '.' + x[1] : '';
			var rgx = /(\d+)(\d{3})/;
			while (rgx.test(x1)) {
				x1 = x1.replace(rgx, '$1' + ',' + '$2');
			}
			return x1 + x2;
		}

	    function disableLink(anchor) {
		    anchor.href = null;
		    anchor.onclick = function() {return false;}
	    }

	    function Paginator(root) {
            root.parentNode.style.display='block';

		    var pageSize = 25;
		    var sensibleMaximum = 100000;

		    var sampleBegin = 0;
		    var sampleEnd = 0;

		    var haveNone = true;
		    var havePrev = false;
		    var haveMore = false;

		    var annotationCount = 0;
		    var proteinCount = 0;
		    var annotationTotal = 0;
		    var proteinTotal = 0;

		    function findLink(name) {
			    return dom.find(root, 'a', name);    
		    }

		    function setImage(img, state) {
			    var paginatorImages = {
					img_page_first:{enabled:'image/nav/paging_first.png', disabled:'image/nav/paging_first_grey.png'},
				    img_page_prev:{enabled:'image/nav/paging_previous.png', disabled:'image/nav/paging_previous_grey.png'},
				    img_page_next:{enabled:'image/nav/paging_next.png', disabled:'image/nav/paging_next_grey.png'},
				    img_page_last:{enabled:'image/nav/paging_last.png', disabled:'image/nav/paging_last_grey.png'}
			    };

			    dom.find(root, 'img', img).src = paginatorImages[img][state];
		    }

		    this.disable = function() {
			    dom.replace(dom.find(root, 'span', 'page_info'), 'Loading annotations...', dom.img('image/ajax-loader.gif'));

			    setImage('img_page_first', 'disabled');
			    disableLink(findLink('link_page_first'));

			    setImage('img_page_prev', 'disabled');
			    disableLink(findLink('link_page_prev'));

			    setImage('img_page_next', 'disabled');
			    disableLink(findLink('link_page_next'));

			    setImage('img_page_last', 'disabled');
			    disableLink(findLink('link_page_last'));
		    };

		    function makeLink(anchor, enabled, url) {
			    if (enabled) {
				    anchor.href = url;
				    anchor.onclick = null;
				    pageLink(anchor);
			    }
			    else {
				    disableLink(anchor);
			    }
		    }

		    function makeLinks() {
			    makeLink(findLink('link_page_first'), havePrev, 'GAnnotation?start=0&count=' + pageSize);
			    var start = sampleBegin - pageSize - 1;
			    makeLink(findLink('link_page_prev'), havePrev, 'GAnnotation?start=' + (start < 0 ? 0 : start) + '&count=' + pageSize);
			    makeLink(findLink('link_page_next'), haveMore, 'GAnnotation?start=' + sampleEnd + '&count=' + pageSize);
			    makeLink(findLink('link_page_last'), annotationCount && (annotationCount < sensibleMaximum) && haveMore, 'GAnnotation?start=' + (annotationCount - pageSize) + '&count=' + pageSize);
		    }

		    var getPageSize = this.getPageSize = function() {
				return dom.find(root, 'select', 'page_size').value;
		    };

		    function handlePageSizeChange() {
			    pageSize = getPageSize();
			    makeLinks();
			    if (!haveNone) {
				    filterParameters['start'] = sampleBegin - 1;
					filterParameters['count'] = pageSize;
				    loadSample();
			    }
		    }

			dom.find(root, 'select', 'page_size').onchange = handlePageSizeChange;

		    this.reset = function() {
			    sampleBegin = getSpanText(annotationTbody, 'sample-begin');
			    sampleEnd = getSpanText(annotationTbody, 'sample-end');

			    annotationCount = getSpanText(statsTbody, 'annotation-count');
			    proteinCount = getSpanText(statsTbody, 'protein-count');
			    annotationTotal = getSpanText(statsTbody, 'annotation-total');
			    proteinTotal = getSpanText(statsTbody, 'protein-total');

			    haveNone = getSpanText(annotationTbody, 'sample-none') == 'true';
			    haveMore = !haveNone && getSpanText(annotationTbody, 'sample-more') == 'true';
			    havePrev = !haveNone && getSpanText(annotationTbody, 'sample-previous') == 'true';

			    dom.replace(dom.find(root, 'span', 'page_info'), haveNone ? 'No matching annotations' : 'Displaying annotations ' + formatWithCommas(sampleBegin) + ' to ' + formatWithCommas(sampleEnd) + (annotationCount||0 > 0 ? ' of ' + formatWithCommas(annotationCount) : '') + (proteinCount||0 > 0 ? ' for ' + formatWithCommas(proteinCount) + ' proteins' : ''));

			    setImage('img_page_first', havePrev ? 'enabled' : 'disabled');
			    setImage('img_page_prev', havePrev ? 'enabled' : 'disabled');
			    setImage('img_page_next', haveMore ? 'enabled' : 'disabled');
			    setImage('img_page_last', (annotationCount && (annotationCount < sensibleMaximum) && haveMore) ? 'enabled' : 'disabled');

			    makeLinks();

			    if (annotationCount > 0 && annotationCount < 100) {
				    makeLink(dom.find(root, 'a', 'link_page_all'), true, getSpanText(annotationTbody, 'sample-url'));
				    dom.find(root, 'span', 'page_all').style.display = 'inline-block';
			    }
			    else {
				    dom.find(root, 'span', 'page_all').style.display = 'none';
			    }
		    };

		    this.setFilterInfo = function(filter) {
			    var infoSpan = dom.find(root, 'span', 'page_filter');

			    if (filter == null) {
				    dom.replace(infoSpan, 'None');
			    }
			    else {
					var params = '';
				    var sep = '';
				    for (var p in filter) {
						params += sep + p + "=" + filter[p];
					    sep = '&';
				    }

				    if (params.length < 80) {
					    dom.replace(infoSpan, params);
				    }
				    else {
					    dom.replace(infoSpan, params.substring(0, 80) + '...');
				    }
			    }
		    };

		    this.setBookmark = function(params) {
			    var link = dom.find(root, 'a', 'link_page_link');
			    link.href = 'GAnnotation?' + remote.encodeParameters(params).substring(1);
		    };

		    this.getValues = function() {
				return { sampleBegin:sampleBegin, sampleEnd:sampleEnd, annotationCount:annotationCount, proteinCount:proteinCount, annotationTotal:annotationTotal, proteinTotal:proteinTotal };
		    };
	    }

	    var paginator = new Paginator(dom.find(annotationPage, 'div', 'paginator'));
	    var statsAvailable = false;

	    function fetchAnnotations() {
		    changed();
	        loadAll();
	    }
	    
	    function processResults(xhr) {
		    var root = dom.div();
		    root.innerHTML = xhr.responseText;
		    //automatic(root); // david asserts this is no longer needed
		    progressive.enhance(root, 'a', 'page', pageLink);

		    return root;
	    }

        function annotationLoaded(xhr) {
	        var temp = processResults(xhr);

	        var resultsTbody = dom.find(temp, 'tbody');

	        var rows = resultsTbody.getElementsByTagName('tr');
	        for (var i = 0, j = 0; i < rows.length; i++) {
		        if (rows[i].className == 'data') {
			        rows[i].className = (j % 2 == 0) ? 'annotation-row-even' : 'annotation-row-odd';
			        j++;
		        }
	        }
			quickgoSelection.enhanceSelection(resultsTbody);

	        progressive.enhance(resultsTbody, 'div', 'info-definition', dialogBoxes.makeInfoBox);

	        dom.empty(annotationTbody);
			annotationTable.importRows(resultsTbody, annotationTbody);

			paginator.reset();

	        annotationToolbar.setToolState(['tool_display', 'tool_id', 'tool_filter'], true);
	        annotationToolbar.setToolState(['tool_statistics', 'tool_export'], statsAvailable);
        }

        function selectOptions(){        	        	
        	util.postRequest('GAnnotation', filterParameters, statsOptionsDialogSource.getElementsByTagName("input"));        	
        }
        
        function statsLoaded(xhr) {        	
        	var temp = processResults(xhr);

	        //dom.empty(statsTbody);
            dom.adoptChildren(dom.find(temp, 'tbody'), statsTbody);
			statsAvailable = true;

	        paginator.reset();

	        function downloadStatistics(e) {
	        	
	        	hideShowProteinOption();
	        	filterParameters['format'] = 'downloadStatistics';	        	
	        	statsOptions.show(e);
	        }

	        function onOpen() {
		        statisticsDialog.dlg.showButton('Reset', false);
		        showFilterParameters(statisticsDialog);
		        statisticsDialog.dlg.selectByIndex(0);
	        }

			statisticsDialog = dialogBoxes.makeTabbedDialog({ caption:'Annotation Statistics', tabs:dom.findAll(dom.find(annotationPage, 'tbody', 'stats'), 'td', 'stats-column'), limits:{ width:60, height:150 }, stateless:true, buttons:[{ label:'Download', action:downloadStatistics }], openAction:onOpen, onSwitch:function() { showFilterParameters(statisticsDialog); } }, true, 'statistics-link');

        	statsOptions = new basicDlg.BasicDialog('Statistics Download Options', statsOptionsDialogSource, { label:'Download', action:selectOptions });        
        	exportDialog.setValues(paginator.getValues());
        	annotationToolbar.setToolState(['tool_statistics', 'tool_export'], true);
        }

        /**
         * Hide/Show Check/Uncheck 'Protein' option in the statistics downloading dialog taking into account the filtering options.
         * Any filter parameter -> Visible and checked. Unchecked and hidden otherwise
         */
        function hideShowProteinOption(){        	
        	var proteinList = filterParameters['protein'];
        	var proteinCheck;
        	//Get the row
        	var columns = statsOptionsDialogSource.getElementsByTagName("td");	        		
        	for (var column in columns) {
        		if(columns[column].textContent == 'Protein'){
        			proteinCheck = getProteinCheck();
        			// Hide/Show Check/Uncheck 'Protein' option
        			if(proteinList != null && proteinList.length > 0){
        				columns[column].parentNode.style.display = 'table-row';// The row is the parent element of the 'Protein' column
        				proteinCheck.checked = true;
        			}else{
        				columns[column].parentNode.style.display = 'none';
        				proteinCheck.checked = true;
        			}
        		}
        	}
        }        
        
        /**
         * 
         * @returns Checkbox object corresponding to the 'Protein' option in the statistics downloading dialog 
         */
        function getProteinCheck(){
        	// Get the 'Protein' option check
			var inputs = statsOptionsDialogSource.getElementsByTagName("input");
			for(input in inputs){
				if(inputs[input].value == 'download_protein'){
					return inputs[input];
				}
			}			
        }
        

        function loadAll() {
	        filterParameters['start'] = 0;
		    filterParameters['count'] = paginator.getPageSize();
            loadSample();
            loadStats();
        }

        function loadStats() {
            dom.empty(statsTbody);
	        statsAvailable = false;
	        annotationToolbar.setToolState(['tool_statistics', 'tool_export'], false);
            statsStatus.request(null, '', null, filterParameters, statsLoaded);
        }

        function loadSample() {
	        paginator.disable();
	        annotationToolbar.setToolState(['tool_display', 'tool_id', 'tool_filter'], false);

            dom.empty(annotationTbody);
            annotationStatus.request(null, '', null, filterParameters, annotationLoaded);
        }

        function pageLink(elt) {
            var linkParameters = parameters.parseHREF(elt.href);
	        var sampleStart = linkParameters['start'];
	        var sampleCount = linkParameters['count'];

            function doRefresh(evt) {
                filterParameters['start'] = sampleStart;
	            filterParameters['count'] = sampleCount;
                loadSample();
                dom.stop(evt);
            }

	        elt.onclick = doRefresh;
        }

        function refreshButton(elt) {
            function doRefresh(evt) {
                loadAll();
                dom.stop(evt);
            }
            dom.onclick(elt,doRefresh);
        }

	    var parameterMapping = {};
	    var activeTools = {};

	    function checkFilteringActive() {
		    var activeFilter = {};
		    activeTools = {};

		    var filteringActive = false;
		    var curVal, defVal;

		    for (var p in filterParameters) {
			    switch (p) {
				// don't consider parameters that only relate to the export dialog
				case 'download-format':
				case 'limit':
				case 'gz':
				    break;

				case 'a':
					curVal = filterParameters[p] + '';
					defVal = defaultFilterParameters[p] + '';
					if (curVal !== defVal) {
						activeFilter[p] = '(' + parameters.compressedTermCount(curVal) + ' terms)';
						activeTools[parameterMapping[p]] = true;
						filteringActive = true;
					}
				    break;

				default:
				    curVal = filterParameters[p] + '';
				    defVal = defaultFilterParameters[p] + '';
				    if (curVal !== defVal) {
					    activeFilter[p] = curVal;
					    activeTools[parameterMapping[p]] = true;
						filteringActive = true;
				    }
					break;
			    }
		    }

		    annotationToolbar.setFilterState(filteringActive, activeTools);
		    paginator.setFilterInfo(filteringActive ? activeFilter : null);
		    paginator.setBookmark(filterParameters);
	    }

	    function getFilterParameters() {
		    var params = {};
            var columnInfo = {};
            
            function getFilterParametersFrom(toolName, formElements) {
                for (var i = 0; i < formElements.length; i++) {
                    var inp = formElements[i];
                    //logger.log('Filter element:', inp.className, inp.type, inp.name, inp.value, inp.checked);
                    if (inp.className == 'noauto' || inp.type=='submit') {
                        continue;
                    }

                    var value = inp.value;
	                if (inp.type == 'checkbox' || inp.type == 'radio') {
		                if (!inp.checked) {
			                continue;
		                }
		                else {
							value = value || '{null}';
		                }
	                }

                    if (inp.name == 'col') {
	                    columnInfo[inp.value] = inp.checked;
                    }

                    var v = inp.name == 'q' ? [value] : util.superSplit(value);
                    var n = inp.name;
                    var a = params[n];
	                params[n] = a ? a.concat(v) : v;

	                parameterMapping[n] = toolName;
                }
            }

            getFilterParametersFrom('tool_filter', filterDialog.findInputElements());
            getFilterParametersFrom('tool_display', displayOptionsDialog.findInputElements());
            getFilterParametersFrom('tool_id', idMappingDialog.findInputElements());

            annotationTable.showHideColumns(columnInfo);

		    return params;
	    }

        function changed() {
            filterParameters = getFilterParameters();

	        checkFilteringActive();

            dom.empty(annotationTbody);
            dom.empty(statsTbody);
        }

		function makeSelectionFilter(node) {
			//logger.log("make selection filter",node);
			var holder=dom.findParent(node,'div');

			var selectionList=dom.tbody();

			var idFilterMap={};

			function termRow(id,name) {
				if (idFilterMap[id]) return idFilterMap[id];
                var checkbox=dom.checkbox();
                checkbox.setAttribute("name","goid");
                checkbox.setAttribute("value",id);
				var childRow=dom.row(checkbox,id,name);
				/*dom.onclick(childRow,function(){loadTerm(id);});*/
				return idFilterMap[id]=childRow;
			}

			function refreshSelection(list) {
				dom.empty(selectionList);
				for (var i=0;i<list.length;i++) {
					dom.add(selectionList,termRow(list[i].id,list[i].name));
				}
			}

			quickgoSelection.registerSelectionListener(refreshSelection);


			dom.replace(holder,dom.div(dom.div('Term Basket:'),dom.table(selectionList)));
		}

		//logger.log("quickgo annotation", form);

		for (var i = 0; i < form.elements.length; i++) {
            var elt = form.elements[i];
	        if (elt.nodeName == 'BUTTON') {
		        if (elt.className == 'refresh') {
			        refreshButton(elt);
		        }
	        }
        }

		progressive.enhance(form,'i','selectionFilter',makeSelectionFilter);

		var dialogBoxes = new dialogs.DialogBoxManager();

	    function AnnotationToolbar() {
		    var toolbar = new dialogBoxes.Toolbar('annotation-toolbar');

			var tools = {
				tool_display:{ dlg:displayOptionsDialog, btn:toolbar.getButton('annotation-display'), highlighted:false },
				tool_id:{ dlg:idMappingDialog, btn:toolbar.getButton('annotation-mapping'), highlighted:false },
				tool_filter:{ dlg:filterDialog, btn:toolbar.getButton('annotation-filter'), highlighted:false },
				tool_statistics:{ dlg:statisticsDialog, btn:toolbar.getButton('annotation-statistic'), highlighted:false },
				tool_export:{ dlg:exportDialog, btn:toolbar.getButton('annotation-export'), highlighted:false }
			};

		    var toolImages = {
			    tool_display:{ enabled:'image/mb/tool_display.png', disabled:'image/mb/tool_display_grey.png', highlighted:'image/mb/tool_display_active.png' },
			    tool_id:{ enabled:'image/mb/tool_id.png', disabled:'image/mb/tool_id_grey.png', highlighted:'image/mb/tool_id_active.png' },
			    tool_filter:{ enabled:'image/mb/tool_filter.png', disabled:'image/mb/tool_filter_grey.png', highlighted:'image/mb/tool_filter_active.png' },
			    tool_statistics:{ enabled:'image/mb/tool_statistics.png', disabled:'image/mb/tool_statistics_grey.png' },
			    tool_export:{ enabled:'image/mb/tool_export.png', disabled:'image/mb/tool_export_grey.png' }
		    };

		    this.setFilterState = function(active, activeTools) {
			    //toolbar.setBackgroundColour(active ? '#b7edc9' : '#eef5f5');
			    for (var tool in tools) {
				    if (activeTools[tool]) {
					    toolbar.setButtonImg(tools[tool]['btn'], toolImages[tool][active ? 'highlighted' : 'enabled']);
					    tools[tool]['highlighted'] = active;
				    }
				    else {
					    tools[tool]['highlighted'] = false;
				    }
			    }
		    };

		    this.setToolState = function(which, enabled) {
			    if (which && which.length > 0) {
				    for (var i = 0, n = which.length; i < n; i++) {
					    var tool = which[i];
					    dialogBoxes.enableDialog(tools[tool]['dlg'], enabled);
					    toolbar.setButtonImg(tools[tool]['btn'], toolImages[tool][enabled ? (tools[tool]['highlighted'] ? 'highlighted' :'enabled') : 'disabled']);
				    }
			    }
		    };
	    }

		function makeExportDialog(enabled) {
			var dialogSource = dom.find(document, 'div', 'export-form');

			var defaultLimit = 10000;
			var annotationCount = 0;
			var proteinCount = 0;
			var annotationTotal = 0;
			var proteinTotal = 0;

			function doExport() {
				var format = dialogBoxes.getSelection(dialogSource, 'download-format');
				var isProteinFormat = (format == 'proteinList' || format == 'fasta');

				var limit = dialogBoxes.getText(dialogSource, 'download-limit') << 0;
				if (limit <= 0) {
					limit = defaultLimit;
					dialogBoxes.setText(dialogSource, 'download-limit', limit);
				}

				var action = 'download';
				if (isProteinFormat) {
					if (limit < proteinCount) {
						action = confirm('The download limit (' + formatWithCommas(limit) + ') is less than the number of proteins (' + formatWithCommas(proteinCount) + ') - do you want to continue?') ? 'download' : 'retry';
					}
				}
				else {
					if (limit < annotationCount) {
						action = confirm('The download limit (' + formatWithCommas(limit) + ') is less than the number of annotations (' + formatWithCommas(annotationCount) + ') - do you want to continue?') ? 'download' : 'retry';
					}
					if (action == 'download' && (Math.min(limit, annotationCount) > (annotationTotal / 4))) {
						if (confirm('You have asked to download a large number of annotations; it might be more efficient to download the full UniProt-GOA annotation set from our ftp site - would you like to do that?')) {
							action = 'redirect';
						}
					}
				}

				switch (action) {
					case 'download':
						filterParameters['format'] = format;
						filterParameters['gz'] = dialogBoxes.getCheckboxSingle(dialogSource, 'download-gzip');
						filterParameters['limit'] = limit;

						util.postRequest('GAnnotation', filterParameters);
						break;

					case 'retry':
						exportDialog['dlg'].show();
						break;

					case 'redirect':
						window.open('ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/');
						break;
				}
			}

			var dlg = dialogBoxes.makeDialog({ caption:'QuickGO Download', source:dialogSource, buttons:{ label:'Download', action:doExport } }, enabled, 'export-link');

			dlg.setValues = function(o) {
				annotationCount = o['annotationCount'];
				proteinCount = o['proteinCount'];
				annotationTotal = o['annotationTotal'];
				proteinTotal = o['proteinTotal'];

				dialogBoxes.setText(dialogSource, 'annotation-count', annotationCount);
				if (annotationCount <= defaultLimit) {
					dialogBoxes.setText(dialogSource, 'download-limit', annotationCount);
				}
				dialogBoxes.setText(dialogSource, 'protein-count', proteinCount);

				setSpanText(dialogSource, 'annotation-count', formatWithCommas(annotationCount));
				setSpanText(dialogSource, 'protein-count', formatWithCommas(proteinCount));
			};
			
			return dlg;
		}
		
		var statsOptionsDialogSource = dom.find(document, 'div', 'stats-options-form');
	    var exportDialog = makeExportDialog(false);
	    var statisticsDialog = null; // dialog will be created after stats have loaded

	    var refreshButtonDef = { label:'Submit', action:fetchAnnotations };

		var displayOptionsDialog = dialogBoxes.makeDialog({ caption:'Display Options', source:dom.find(annotationPage, 'div', 'display-options-form'), buttons:refreshButtonDef }, false, 'display-link');

	    var idMappingDialog = dialogBoxes.makeDialog({ caption:'ID Mapping', source:dom.find(annotationPage, 'div', 'idmapping-form'), buttons:refreshButtonDef }, false, 'idmapping-link');
	    idMappingDialog['dlg'].enhance('tr', 'movable', makeMovable);

	    var filterDialog = dialogBoxes.makeTabbedDialog({ caption:'Filter Annotations', tabs:dom.findAll(dom.find(annotationPage, 'div', 'filter-form'), 'div', 'filter-column'), buttons:refreshButtonDef, limits:{ width:60, height:150 }, stateless:true, openAction:function() { showFilterParameters(filterDialog); }, onSwitch:function() { showFilterParameters(filterDialog); } }, false, 'filter-link');

	    var dynamicRearranger = new rearranger.DynamicRearranger();

	    function makeMovable(elt) {
		    elt.onmousedown = function(evt) { return dynamicRearranger.moveBegin(elt, evt) };
	    }

	    function showFilterParameters(dlg) {
		    function displayText(s, limit, thing) {
			    if (s && s.length > 0) {
				    var things = s.split(',');
				    if (things.length > limit) {
						return '(' + things.length + ' ' + thing + ')';
				    }
				    else {
					    return s;
				    }
			    }
			    else {
				    return '(No ' + thing + ')';
			    }
		    }

		    if (dlg && filterDialog) {
			    var divs = dom.findAll(dlg.content, 'div', 'filter-params');
		        var n = divs.length;
		        if (n > 0) {
			        var params = filterDialog.getState();

			        for (i = 0; i < n; i++) {
					    var tblParams = dom.classTable("quickgo-standard");
				        var cnt = 0;
				        for (var p in params) {
					        var value = params[p] + '';
					        if (value != (defaultFilterParameters[p] + '')) {
						        var s;
						        switch (p) {
							        case 'a':
										s = '(' + parameters.compressedTermCount(value) + ' terms)';
							            break;
							        case 'goid':
									    s = displayText(value, 10, 'terms');
							            break;
							        case 'protein':
								        s = displayText(value, 10, 'proteins');
							            break;
							        default:
									    s = value;
							            break;
						        }
						        dom.add(tblParams, dom.row(p, s));
						        cnt++;
					        }
				        }

				        dom.replace(divs[i], (cnt > 0) ? tblParams : dom.div("None"));
			        }
		        }
		    }
	    }

        defaultFilterParameters = getFilterParameters();

	    var annotationToolbar = new AnnotationToolbar();
	    annotationToolbar.setToolState(['tool_statistics', 'tool_export'], false);

		fetchAnnotations();

        this.fetchAnnotations=fetchAnnotations;
        this.form=form;

    }

    function autoAnnotationPage(anchor) {
        new AnnotationForm(dom.findParent(anchor,'div','annotation-page'));
    }

    this.afterDOMLoad(function(){
        progressive.enhance(document.body, 'i', 'auto-annotation-page', autoAnnotationPage);
    });

    this.AnnotationForm=AnnotationForm;
});