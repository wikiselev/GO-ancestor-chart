JSLIB.depend('quickgoChart', ['jslib/dom.js', 'jslib/remote.js', 'jslib/progressive.js', 'jslib/parameters.js', 'jslib/lightbox.js', 'jslib/tabs.js', 'quickgoSelection.js', 'jslib/hash.js'],
function(dom, remote, progressive, parameters, lightbox, tabs, quickgoSelection, hash){
	var quickgoChart = this;

    var logger = this.logger;
	logger.log('LOADING quickgoChart');

    var queue = new remote.JSONCache(new remote.JSONQueue('GTerm', { format:'json' }), 'id');

	var infoText = dom.div();
	var infoBox = new lightbox.LightBox({ title:dom.div('QuickGO Help'), content:infoText });
	var xhrq = new remote.XHRQueue("reference.html");
	var status = new remote.XHRStatusWrapper(new remote.XHRParameters(xhrq, '', { format:'raw' } ), 'Loading...');
	
    var relationColours = {
	    '=':'#000', '?':'#fff', 'I':'#000', 'P':'#00f', 'R':'#fc0', 'PR':'#0f0', 'NR':'#f00', '+':'#0f0', '-':'#f00'
    };

	var colourPalette = [
		"#808080",
		"#ff8080",
		"#80ff80",
		"#8080ff",
		"#ff80ff",
		"#80ffff",
		"#ffff80",
		"#b7edc9",
		"#dad2a0",
		"#afcfcf",
		"#dbb999",
		"#97a797",
		"#cc98e6",
		"#c38fbb",
		"#c2aea0",
		"#a9a9a9",
		"#c69191",
		"#fae5d7",
		"#91c691",
		"#e6bf98",
		"#ededb7",
		"#dfdfdf",
		"#a8bfba",
		"#cfcfaf",
		"#c691b5",
		"#9797a7",
		"#f5e3ce",
		"#e7da9d",
		"#ffbf80",
		"#edb7ed",
		"#ececf9",
		"#acacd5",
		"#c58b8b",
		"#91c6b3",
		"#b5a092",
		"#c6b591",
		"#80bfff",
		"#80ffbf",
		"#91b5c6",
		"#9bd7ee",
		"#edc9b7",
		"#d6f4f4",
		"#ad9f99",
		"#a797a7",
		"#e598cc",
		"#cce598"
	];

	var chartSettings = {};
	
	this.AncestryChart = function(displayOptionsForm, chartName) {
		this.attachToolbar = function (toolbar) {
			dom.onclick(dom.find(toolbar, 'div', 'toolbar-button'), displayOptions);
		};
		
		var holder = this.content = dom.div();
        var thumbnail = this.thumbnail = dom.div();

		for (var ix = 0; ix < displayOptionsForm.elements.length; ix++) {
			var e = displayOptionsForm.elements[ix];
			if (chartSettings[e.name]) {
				e.value = chartSettings[e.name];
			}
		}

		function refreshChart() {
			dom.replaceContent(holder, loadingBig());
			dom.replaceContent(thumbnail, loadingBig());
			var ids = '';
			for (var id in terms) {
				ids += id + terms[id] + ',';
			}
			displayOptionsForm.elements['id'].value = ids;
            getLayout.post('', remote.getFormParameters(displayOptionsForm), null, loaded);
            if (chartName) {
	            hash.set(chartName, ids);
            }
            chartLightBox.close();
		}

		function displayOptions() {
			settingsTab.select();
			chartLightBox.show();
		}

        var getLayout = new remote.XHRQueue("GMultiTerm", {embed:'html'});

        function processResults(xhr, name, className) {
		    var root = dom.div();
		    root.innerHTML = xhr.responseText;

			return dom.find(root, name, className);
	    }

		var termInfo = dom.div('Click on a term rectangle in the chart view to display information about the term here...');
		var settings = dom.findParent(displayOptionsForm, 'div');
		var lightBoxTabs = new tabs.Tabs({name:'chartTab'});
		var termInfoTab = lightBoxTabs.createTab('info', 'Term info', termInfo);
		var settingsTab = lightBoxTabs.createTab('settings', 'Settings', settings);

        var chartLightBox = new lightbox.LightBox({ title:'Term Ancestry', content:lightBoxTabs, actions:{ 'OK':refreshChart }, className:'chart_lightbox' });

		chartLightBox.fix(lightBoxTabs.tabcontent, 120, 160);

        function loadingBig() {
	        return dom.styleDiv({textAlign:'center',fontSize:'200%'}, dom.div('LOADING'), dom.div(dom.img('image/ajax-loader-big.gif')));
        }

		var terms = {};

        function loaded(xhr) {
            function addArea(input) {
                var area = dom.classEl('area', 'clickable');
                area.setAttribute('coords', input.getAttribute('coords'));
                area.setAttribute('shape', input.getAttribute('shape'));

	            var clickMsg = ' - click for more information';
	            var areaType = input.getAttribute('type');
	            if (areaType == 'term') {
		            var areaID = parameters.parseHREF(input.getAttribute('href'))['id'];
		            area.setAttribute('alt', areaID + clickMsg);
		            area.setAttribute('title', areaID + clickMsg);
	            }
	            else if (areaType == 'legend') {
		            var topic = input.getAttribute('topic');
		            area.setAttribute('alt', topic + clickMsg);
		            area.setAttribute('title', topic + clickMsg);
	            }

                function termRow(id, name) {
	                var toggle = dom.img(terms[id] == null ? 'image/chart_add.png' : 'image/chart_delete.png');
	                var childRow = dom.row(toggle, quickgoSelection.makeSelectableGoTerm(id), name);
                    childRow.style.backgroundColor = terms[id]||'#fff';
					dom.hoverStyle(childRow, { textDecoration:'underline', cursor:'pointer' });
	                dom.onclick(toggle, function(){ addRemoveTermAuto(id); });
                    return childRow;
                }

                function loadedTermData(termData) {
					var id = termData['term']['id'];

	                var tblDefinition = dom.classTable("tab-holder");
	                dom.add(tblDefinition, dom.classTr("shadowbox", dom.classTd("shadowbox", quickgoSelection.makeSelectableGoTerm(id, '  ', termData['term']['name']))));

					var definitionDiv = dom.styleDiv({ margin:'10px', width:'50%' }, termData['termInfo']['definition']);

	                var tblDetails = dom.classTable("tab-holder");
                    var childTerms = dom.classTable("quickgo-standard");

                    var children = termData['termInfo']['children'];
                    for (var i = 0; i < children.length; i++) {
                        var childId = children[i]['child']['id'];
                        var childName = children[i]['child']['name'];
                        dom.add(childTerms, termRow(childId, childName));
                    }

	                dom.add(tblDetails, dom.classTr("shadowbox", dom.classTd("shadowbox", "Child Terms")), childTerms);

                    dom.replaceContent(termInfo, dom.div(tblDefinition, definitionDiv, tblDetails));
                }


                function showTermInfo(event) {
                    dom.stop(event);
                    //logger.log('addterm:',areaID);
                    dom.replaceContent(termInfo,loadingBig());
					termInfoTab.select();
                    chartLightBox.show();
                    queue.get(areaID,loadedTermData);
                }

	            function showLegendInfo(event) {
		            function infoLoaded(xhr) {
			            infoText.innerHTML = xhr.responseText;
			            var content = dom.find(infoText, 'div', 'snippet');
			            dom.replace(infoText, content);
			            infoBox.setTitle(topic);
			            infoBox.show();
		            }

		            function infoUnavailable() {
			            alert('Sorry - detailed information for ' + topic + ' is not available.');
		            }
		            status.request(null, '', null, { section:topic }, infoLoaded, infoUnavailable);
	            }

                dom.onclick(area, areaType == 'term' ? showTermInfo : showLegendInfo);
                return area;
            }

            var results = processResults(xhr, 'div');
            //logger.log('XHR' + xhr.responseText.length + " " + results);
            var areas = dom.findAll(results, 'area', 'popup');
            var map = dom.el('map');
            //map.setAttribute('name',);
            var name = dom.find(results, 'map').getAttribute('name') + "i";
            map.name = name;
            map.id = name;
            for (var i = 0; i < areas.length; i++) {
                dom.add(map, addArea(areas[i]));
            }

            var errorDiv = dom.find(results, 'div', 'error');
            if (errorDiv) {
                dom.add(errorDiv, dom.div('Click here to adjust the limit'));
                dom.onclick(errorDiv, displayOptions);
            }

            var srcimg = dom.find(results,'img');

			function zoom(event) {
				dom.stop(event);
                dom.show(holder);				
			}

            var width = dom.find(results, 'input', 'width').value;
            var height = dom.find(results, 'input', 'height').value;

			var fullimg = dom.img(srcimg.getAttribute('src'));
            fullimg.useMap = "#"+name;
			fullimg.setAttribute('width', width);
			fullimg.setAttribute('height', height);
			
			var thumbimg = dom.img(srcimg.getAttribute('src'));
			
			var widthAvailable = thumbnail.offsetWidth||document.documentElement.offsetWidth/3;
			var scale = Math.min(1/2, widthAvailable / width);

			thumbimg.setAttribute('width', (width*scale)>>0);
			thumbimg.setAttribute('height', (height*scale)>>0);
			dom.onclick(thumbimg, zoom);

            dom.replaceContent(holder, dom.div(map, fullimg, errorDiv));
            //logger.log(holder.innerHTML);
            dom.replaceContent(thumbnail, dom.div(thumbimg));
        }

		var count = 0;
        var chartListeners = {};

        function addTermInternal(id) {
            return terms[id] = colourPalette[count++%colourPalette.length];
        }

        function addTermsInternal(ids) {
            for (var i = 0; i < ids.length; i++) {
                addTermInternal(ids[i]);
            }
        }

        var addRemoveTerms = this.addRemoveTerms = function addRemoveTerms(ids,add) {
            for (var j = 0; j < ids.length; j++) {
                var colour = null;
                var id = ids[j];
                var alreadyAdded = (terms[id] != null);
                //logger.log('Add remove ',ids[j],add,alreadyAdded);
                if (add == alreadyAdded) {
	                continue;
                }
                if (!add) {
	                delete terms[id];
                }
                else {
	                colour = addTermInternal(id);
                }
                var listeners = chartListeners[id];
                if (listeners) {
	                for (var i = 0; i < listeners.length; i++) {
		                listeners[i](colour);
	                }
                }
            }
            refreshChart();
        };

        var addRemoveTermAuto=this.addRemoveTermAuto=function addRemoveTermAuto(id) {
            addRemoveTerms([id], terms[id] == null);
        };

		var termInChart = this.termInChart = function(id) {
			return terms[id] != null;
		}; 

        this.addChartTermListener = function(id, callback) {
            (chartListeners[id] = chartListeners[id]||[]).push(callback);
            callback(terms[id]);
        };

        this.getTermColours = function getTermColours() {
            return terms;
        };

        var initialIds = hash.get(chartName);
        if (initialIds) {
	        addTermInternal(initialIds.split(','));
        }
	};

    function makeAncestry(elt) {
		//logger.log('make ancestry');
		var parent = dom.findParent(elt,'div');
		var displayOptionsForm = dom.find(parent,'form');
        var holder = dom.find(parent, 'div', 'results');
        var id = displayOptionsForm.elements['id'].value;

		var chart = new quickgoChart.AncestryChart(displayOptionsForm);

		dom.add(holder, chart);

		//logger.log('add toolbar');
		var displayToolbar = dom.find(parent, 'div', 'jsonly');
		if (displayToolbar) {
			displayToolbar.style.display = 'block';
			chart.attachToolbar(displayToolbar);
		}

		//logger.log('add term',id);
		chart.addRemoveTermAuto(id);

		//logger.log('made ancestry');
    }

	function readCookie(name) {
		var nameEQ = name + "=";
		var ca = document.cookie.split(';');
		for(var i = 0; i < ca.length; i++) {
			var c = ca[i];
			while (c.charAt(0) == ' ') {
				c = c.substring(1, c.length);
			}
			if (c.indexOf(nameEQ) == 0) {
				return c.substring(nameEQ.length, c.length);
			}
		}
		return null;
	}

	function getChartSettings() {
		if (navigator.cookieEnabled) {
			var qgCookie = readCookie('quickgo');
			//logger.log("getChartSettings: cookie = " + qgCookie);
			if (qgCookie) {
				var kvPairs = qgCookie.split('_');
				for (var i = 0; i < kvPairs.length; i++) {
					var kvPair = kvPairs[i];
					if (kvPair.substring(0, 2) == 'c$') {
						var kv = kvPair.substring(2).split('-', 2);
						chartSettings['chart_' + kv[0]] = kv[1];
					}
				}
			}
		}
	}

	var enhanceChart = this.enhanceChart = function(node) {
		getChartSettings();
		progressive.enhance(node, 'i', 'ancestrychart', makeAncestry);
	};

    this.afterDOMLoad(function() {
        enhanceChart(document.body);
        logger.log("chart enhanced");
    });


});