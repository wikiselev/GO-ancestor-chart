JSLIB.depend('tester',['dom.js'],
function tester(dom) {

    var logger=this.logger;

    this.browserDetect = new (function BrowserDetect() {
        //based on: http://www.quirksmode.org/js/detect.html



        function searchString(data) {
            for (var i=0;i<data.length;i++)	{
                var dataString = data[i].string;
                var dataProp = data[i].prop;
                this.versionSearchString = data[i].versionSearch || data[i].identity;
                if (dataString) {
                    if (dataString.indexOf(data[i].subString) != -1)
                        return data[i].identity;
                }
                else if (dataProp) {
                    return data[i].identity;
                }
            }
            return null;
        };
        function searchVersion(dataString) {
            var index = dataString.indexOf(this.versionSearchString);
            if (index == -1) return null;
            return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
        };
        //noinspection JSUnresolvedVariable
        var dataBrowser=[
            {
                string: navigator.userAgent,
                subString: "Chrome",
                identity: "Chrome"
            },
            { 	string: navigator.userAgent,
                subString: "OmniWeb",
                versionSearch: "OmniWeb/",
                identity: "OmniWeb"
            },
            {
                string: navigator.vendor,
                subString: "Apple",
                identity: "Safari",
                versionSearch: "Version"
            },
            {
                prop: window.opera,
                identity: "Opera"
            },
            {
                string: navigator.vendor,
                subString: "iCab",
                identity: "iCab"
            },
            {
                string: navigator.vendor,
                subString: "KDE",
                identity: "Konqueror"
            },
            {
                string: navigator.userAgent,
                subString: "Firefox",
                identity: "Firefox"
            },
            {
                string: navigator.vendor,
                subString: "Camino",
                identity: "Camino"
            },
            {		// for newer Netscapes (6+)
                string: navigator.userAgent,
                subString: "Netscape",
                identity: "Netscape"
            },
            {
                string: navigator.userAgent,
                subString: "MSIE",
                identity: "Explorer",
                versionSearch: "MSIE"
            },
            {
                string: navigator.userAgent,
                subString: "Gecko",
                identity: "Mozilla",
                versionSearch: "rv"
            },
            { 		// for older Netscapes (4-)
                string: navigator.userAgent,
                subString: "Mozilla",
                identity: "Netscape",
                versionSearch: "Mozilla"
            }
        ];
        var dataOS=[
            {
                string: navigator.platform,
                subString: "Win",
                identity: "Windows"
            },
            {
                string: navigator.platform,
                subString: "Mac",
                identity: "Mac"
            },
            {
                string: navigator.userAgent,
                subString: "iPhone",
                identity: "iPhone/iPod"
            },
            {
                string: navigator.platform,
                subString: "Linux",
                identity: "Linux"
            }
        ];


        this.browser = searchString(dataBrowser) || "unknown";
        this.version = searchVersion(navigator.userAgent)
                || searchVersion(navigator.appVersion)
                || "unknown";
        this.OS = searchString(dataOS) || "unknown";
    });

    this.Tester=function(runTests) {

        var isIdle;
        var waiting;

        var testWindow;
        var testDocument;
        var debugMode=false;

        function debug() {
                        
            debugMode=true;
            
            runTests();
        }


        function run() {
            ui.clear();
            debugMode=false;

            runTests();
        };

        this.run=run;

        this.setTestWindowDocument=function (window,document) {
            testWindow=window;
            testDocument=document;
        };

        this.getUserId=function() {
            return ui.getUserId();
        };



        this.open=function(url,next,failed) {
            //registration(null);
            ui.clear();
            var iframe=ui.load(url);

            wait(function() {
                ui.canStart(false);
                testWindow=iframe.contentWindow;
                testDocument=testWindow && testWindow.document;
                if (testDocument) next();
                else tryagain();
            });

        };


        var ui=new (function UserInterface() {

            var nextAction;
            var results=dom.styleDiv({overflow:'auto'});
            var nextButton=dom.button(function(){nextButton.disabled=true;nextAction();},'Next');
            var runButton=dom.button(run,'Run');
            var debugButton=dom.button(debug,'Debug');
            var userInput=dom.input();
            nextButton.disabled=true;
            var controls=dom.styleDiv({overflow:'auto'},dom.div('Identity:',userInput,runButton,debugButton,nextButton),results);

            var iframeHolder=dom.styleDiv({position:'relative',height:'100%'});

            var cell1=dom.td(controls);
            var cell2=dom.td(iframeHolder);
            cell1.style.width='20%';
            cell2.style.width='80%';
            cell2.style.height='100%';
            cell1.style.verticalAlign='top';
            cell2.style.verticalAlign='top';
            var tab=dom.table(dom.tbody(dom.add(dom.tr(),cell1,cell2)));
            tab.style.height='100%';
            tab.style.width='100%';

            this.result=function(passed,name,info) {
                dom.add(results,info);
                dom.add(results, dom.styleDiv({backgroundColor:passed ? '#7f7' : '#f77'}, name));
            };

            this.print=function(message) {
                logger.log('Print:'+message);
                dom.add(results, dom.styleDiv({},message));
            };

            this.load=function(url) {
                dom.empty(iframeHolder);
                var iframe=dom.el('iframe');
                iframe.src=url;
                iframe.style.width='100%';
                iframe.style.height='100%';
                iframe.style.border = 'none';
                logger.url(url);
                iframeHolder.appendChild(iframe);
                return iframe;
            };

            this.setNext=function (callback) {
                nextAction=callback;
                nextButton.disabled=false;
            };

            this.clear=function() {
                dom.empty(results);                
            };

            this.canStart=function(enable) {
                runButton.disabled = !enable;
                debugButton.disabled = !enable;
            };

            this.getUserId=function() {
                return userInput.value;
            };

            document.body.appendChild(tab);
        });

        var infoDiv;
        var countDown;

        function wait(callback) {
            countDown=20;
            infoDiv=dom.div();
            waiting=callback;
            callback();
        }

        function tryagain() {
            countDown--;
            if (countDown<=0) return false;
            infoDiv=dom.div();
            dom.schedule(logger.wrap(waiting),500);
            return true;
        }

        /*
        function finished() {
            logger.run(function(){
                logger.log('Finished',waiting);
                var next=waiting;
                waiting=null;
                if (next) next();
            });

        }
        function wait(callback) {
            if (isIdle && isIdle()) callback();
            else waiting=callback;
        }

        var targetAPI=null;

        var registration=window.registration=function(jsAPI) {
            targetAPI=jsAPI;
            if (jsAPI) {
                ui.print('Target loaded');
                jsAPI.onIdle(function(){if (jsAPI==targetAPI) finished();});
                isIdle=jsAPI.isIdle;
            } else {
                isIdle=null;
            }
        };*/

        this.result=function (name,passed,next,failed) {
            logger.log("Result:", name, passed);

            if (passed) {
                ui.result(passed,name,infoDiv);
                if (debugMode) {
                    wait(function() {
                        ui.setNext(next);
                    });
                } else {
                    wait(next);
                }
                
            } else {
                if (!tryagain()) {
                    ui.result(passed,name,infoDiv);
                    failed();
                }
            }
        };

        this.print=function(message) {
            ui.print(message);
        };

        function info(message) {
            dom.add(infoDiv,dom.div(message));
        }

        function isVisible(item) {
            var s=item.currentStyle||testWindow.getComputedStyle(item,null);
            logger.log('Is visible:'+s.visibility);
            if (s.visibility=='visible') return true;
            if (s.visibility=='inherit') {
                if (item.parentNode) return isVisible(item.parentNode);
                return true;
            }
            return false;
        }

        function checkVisible(item,name) {

            if (!item) {
                info('Not found:'+name);
                return null;
            }
            if (isVisible(item)) return item;
            info('Not visible:'+name);
            return null;
        }

        this.findID=function (id) {
            logger.log('Find',id);
            return checkVisible(testDocument.getElementById(id),'[id="'+id+'"]');
        };

        this.document=function() {
            return testDocument;
        };

        this.findNameClass=function (target,name,className) {
            if (!target) return null;
            logger.log('FindNC',name,className);
            return checkVisible(dom.find(target,name,className),name+'[class="'+className+'"]');
        };

        this.findNameClassText=function (target,name,className,text) {
            if (!target) return null;
            var message=name+'[class="'+className+'"][text()="'+text+'"]';
            var elts=dom.findAll(target,name,className);
            for (var i=0;i<elts.length;i++) {
                if (dom.getInnerText(elts[i])==text) return checkVisible(elts[i],message);
            }
            return checkVisible(null,message);
        };

        var sendEvent=this.sendEvent=function(target,eventName,eventClass,props) {

            if (!target) return null;
            try {
            var e={};
            if (testDocument.createEventObject) {
                e=testDocument.createEventObject();
                logger.log('createEventObject');
            } else if (testDocument.createEvent) {
                e=testDocument.createEvent(eventClass);

                logger.log('createEvent '+eventClass);
                if (e.initMouseEvent) {
                    logger.log('initMouseEvent');
                    e.initMouseEvent(eventName,true,true,window,0,0,0,0,0,false,false,false,false,0,null);
                } else if (e.initKeyEvent) {
                    logger.log('initKeyEvent');
                    e.initKeyEvent(eventName,true,true,null,false,false,false,false,props['keyCode'],props['keyCode']);
                } else {
                    logger.log('initEvent');
                    e.initEvent(eventName,true,true);
                }

            } else {
                logger.log('Unable to create event');
            }
            if (props) {
                for (var x in props) {
                    try{
                        e[x]=props[x];
                    } catch (e) {
                        logger.log('Unable to set '+x);
                    }
                }
            }
            if (target.fireEvent) {
                target.fireEvent('on'+eventName,e);
                logger.log('fireEvent');
            } else if (target.dispatchEvent) {
                target.dispatchEvent(e);
                logger.log('dispatchEvent');
            } else {
                logger.log('Unable to dispatch event');
            }
            } catch (e) {
                logger.log('Unable to send event'+e);

            }


        };


        this.setValue=function(input,value) {
            if (input==null) return;
            this.sendClick(input);
            input.value=value;
            sendEvent(input,'change','HTMLEvents',null);
            sendEvent(input,'keyup','HTMLEvents',{keyCode:13});
            sendEvent(input,'keyup','KeyboardEvent',{keyCode:13});
        };

        this.sendClick=function (target) {
            sendEvent(target,'click','HTMLEvents',null);
            sendEvent(target,'click','MouseEvents',null);
        };

        this.testValue=function(input,value) {
            if (input && input.value==value) return input;
            if (!input) info('Input does not exists');
            if (input.value!=value) info('Input not set to:'+value);
            return null;
        };

        this.end=function() {
            ui.canStart(true);
        };

    };



});
