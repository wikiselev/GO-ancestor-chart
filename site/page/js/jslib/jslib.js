var scriptingEnabled=(document.cookie||'').indexOf('js=off')<0;


function jsEnable(status) {
    var date = new Date();
    date.setTime(date.getTime()+(365*24*60*60*1000));
    document.cookie = "js="+status+"; expires="+date.toGMTString()+"; path=/";
    window.location.reload();
}

if (scriptingEnabled) {
	if (!document.getElementsByTagName) scriptingEnabled=false;
}

if (!scriptingEnabled) {
    this.JSLIB={};
    this.JSLIB.depend=function(){};
} else {




    this.JSLIB=new function JSLIB() {

        function After() {
            var callbacks=[];
            var result;

            this.done=(function done(value) {
                if (callbacks) {
                    result=value;
                    var callbacktemp=callbacks;
                    callbacks=null;
                    for (var i=0;i<callbacktemp.length;i++) logger.run(callbacktemp[i],value);
                }
            });

            this.register=(function register(callback) {
                if (callbacks) callbacks.push(callback);
                else callback(result);
            });

            
        }

        var debugMode=("&"+window.location.search.substring(1)).indexOf("&debug")>=0 || document.cookie.indexOf('debug_on')>=0;

        var logging=new function() {

            var holder=this.content=document.createElement('div');

			
			var linkdiv=document.createElement('div');
			var link=document.createElement('button');
			//link.href=(window.location.search?window.location.search+'&':'?')+'debug';
			link.onclick=function() {
				document.cookie=debugMode?'debug=debug_off':'debug=debug_on';
				window.location.reload();
			};
			link.appendChild(document.createTextNode('Restart '+(debugMode?'without':'with')+' logging'));
			linkdiv.appendChild(link);

			holder.appendChild(linkdiv);


            function getTime() {
                return new Date().getTime();
            }
            var start=getTime();



            this.show=("&"+window.location.search.substring(1)).indexOf("&debug=open")>=0;

			if (this.show) {
				holder.style.position='absolute';
				holder.style.zIndex='2000';
				holder.style.background='#cfc';
				holder.style.width='100%';
				
			}



            var stackTraceEnabled=false;

            if (debugMode) {
                try {
                    throw new Error('test');
                } catch (e) {
                    if (e['stack']) stackTraceEnabled=true;
                }
            }



            function Logger(name) {


                function ctxt(txt) {
                    return document.createTextNode(txt);
                }

                function cin(txt) {
                    var e=document.createElement('input');
                    e.value=txt;
                    e.style.width='100%';
                    e.style.border='none';
                    e.style.background='none';
                    return e;
                }

                function cel(name) {
                    var e=document.createElement(name);
                    for (var i=1;i<arguments.length;i++) e.appendChild(arguments[i]);
                    return e;
                }

                this.content=holder;
                var log=this.log=function() {
                    if (!debugMode) return;
                    var when=(getTime()-start)/1000;
                    var line=cel('div',ctxt(when+' '+name));
                    for (var i=0;i<arguments.length;i++) {
                        if (arguments[i] && arguments[i].parentNode) {
                            var txt='';
                            var p=arguments[i];
                            while (p) {
                                txt='/'+p.nodeName+(p.id?'[id='+p.id+']':'')+txt;
                                p=p.parentNode;
                            }
                            line.appendChild(ctxt(' '+txt));
                        } else {
                            var v=arguments[i];
                            if (v==null) v="null";
                            if (v.toString) v=v.toString();
                            line.appendChild(ctxt(' '+v));
                        }
                    }
                    holder.appendChild(line);
                };

                this.extra=function(title,text){
                    if (!debugMode) return false;
                    var openClose=cel("div",ctxt(title));
                    openClose.style.backgroundColor='#00f';
                    openClose.style.color='#fff';
                    var opened=false;
                    var content=cel('pre',ctxt(text));
                    content.style.display='none';
                    openClose.onclick=function() {
                        opened=!opened;
                        content.style.display=opened?'block':'none';
                    };
                    holder.appendChild(openClose);
                    holder.appendChild(content);
                };



                var error=this.error = function(e) {
                    if (!debugMode) return false;
                    var line=cel('div',ctxt(e.message));
                    line.style.background='#fc0';                    
                    holder.appendChild(line);
                    var stackTbody=cel('tbody');
                    var stackTable = cel('table',stackTbody);
                    stackTable.style.tableLayout='fixed';
                    stackTable.style.width='100%';                    
                    holder.appendChild(stackTable);
                    if (stackTraceEnabled && e['stack']) {
                        var lines=e["stack"].split("\n");
                        for (var l in lines) {
                            var loc=/(.*)@(.*):(.*)/.exec(lines[l]) ||
                                    /at *(.*)\((.*):(.*:.*)\)/.exec(lines[l]) ||
                                    /at *()(.*):(.*:.*)/.exec(lines[l]);
                            if (loc)
                                stackTbody.appendChild(cel('tr',
                                        cel('td',cin(loc[1])),
                                        cel('td',cin(loc[2])),
                                        cel('td',cin(loc[3]))));
                            else
                                stackTbody.appendChild(cel('tr',cel('td',cin(lines[l]))));
                        }
                    } else {
                        log("[No stack]");
                    }
                    return stackTraceEnabled;
                };

                this.url=function(url) {
                    if (!debugMode) return;
                    var link=cel('a',ctxt(url));
                    link.target='_blank';
                    link.href=url;                                        
                    holder.appendChild(cel('div',link));
                };




                this.run=function(f,x) {
                    if (!stackTraceEnabled) return f(x);
                    try {
                        return f(x);
                    } catch (ex) {
                        if (!error(ex)) throw ex;
                        return null;
                    }
                };


                this.make=function(f,x) {
                    if (!stackTraceEnabled) return new f(x);
                    try {
                        return new f(x);
                    } catch (ex) {
                        if (!error(ex)) throw ex;
                        return null;
                    }
                };


                this.wrap=function (f) {
                    if ((typeof f)!="function") throw new Error("Not function:"+f);
                    return function runWrapper(x) {
                        return logger.run(f,x);
                    };
                };

            }





            this.makeLogger=function(name) {
                return new Logger(name);
            };


        };

        function dirname(path) {
            return ((path+'').replace(/\?.*$/,'').replace(/[^\/]*$/,''));
        }

        function basename(path) {
            return ((path+'').replace(/\?.*$/,'').replace(/^.*\//,''));
        }

        function mySrc(jsName) {
            var scripts=document.getElementsByTagName('script');
            for (var i=0;i<scripts.length;i++) {
                var src=scripts[i].getAttribute('src');                
                if (basename(src)==jsName) return src;
            }
            return null;
        }

        function myLocation(jsName) {
            return mySrc(jsName).replace(/\?.*/,'');
        }

        function myParameters(jsName) {
            return mySrc(jsName).replace(/.*\?/,'');
        }


        var logger=logging.makeLogger('?');

        logger.log('initialised logging');

        var afterDomLoad=new After();
        var afterAllLoad=new After();

        window.onload=function() {
            logger.log("window.onload");
            if (logging.show) {
                document.body.appendChild(logging.content);
            }
            logger.run(afterDomLoad.done);
            logger.run(afterAllLoad.done);
        };

        if (window.addEventListener) {
            window.addEventListener("DOMContentLoaded",function() {
                logger.log("window.DOMContentLoaded");
                logger.run(afterDomLoad.done);
            },false);
        }


       
        var modules={};

        function Module(path,loadRequired) {

            this.path=path;

            var moduleLoaded=new After();
            var loading=false;
            var isLoaded=false;

            this.loaded=(function loaded() {
                loading=false;
                isLoaded=true;
                moduleLoaded.done(this);
            });

            if (loadRequired) {

                var s=document.createElement('script');
                var src=path+(debugMode?'?'+Math.random():'');
                s.setAttribute('src',src);
                document.getElementsByTagName('head')[0].appendChild(s);
            }

            this.After=After;

            this.afterLoad=(function afterLoad(cb) {
                //if (loading) throw new Error('Circular dependency for: '+path);
                moduleLoaded.register(cb);
            });

            this.afterDOMLoad=function (cb) {
                afterDomLoad.register(cb);
            };

            this.afterAllLoad=function (cb) {
                afterAllLoad.register(cb);
            };

            this.checkLoad=function() {
                if (loading || isLoaded) return false;
                loading=true;
                return true;
            };

            this.path=path;

            this.logger=logging.makeLogger(path);

            this.load=function (names,callback) {
                jsLoad(path,names,callback);
            };

            this.dirname=dirname;
        }

        function findModule(path,loadRequired) {
            return modules[path]||(modules[path]=new Module(path,loadRequired));
        }

        function normalize(path) {
            var result='';
            var elements=path.split('/');
            for (var i=elements.length-1;i>=0;i--) {
                if (elements[i]=='..') i--;
                else if (elements[i]!='.') result='/'+elements[i]+result;
            }
            return result.substring(1);

        }



        function jsLoad(location,names,callback) {

            var base=dirname(location);



            var response=[];
/*

            function getModule(i) {
                if (i>=names.length) {
                    callback.apply(null,response);
                } else {
                    var path=normalize(base+names[i]);

                    var m=findModule(path,true);
                    m.afterLoad(function loaded(r) {
                        response[i]=r;
                        getModule(i+1);                        
                    });
                }
            }

            domAfterLoad.register(function(){getModule(0);});
*/

            function checkLoaded() {
                if (ct==names.length) callback.apply(null,response);
            }

            function moduleLoad(i) {
                var path=normalize(base+names[i]);
                var m=findModule(path,true);
                m.afterLoad(function loaded(r) {
                    
                    response[i]=r;
                    ct++;
                    checkLoaded();
                });
            }

            var ct=0;

            for (var i=0;i<names.length;i++) moduleLoad(i);

            if (names.length==0) checkLoaded();

        };


        this.depend=(function depend(moduleName,depends,callback) {
            logger.run(function (){
                var scriptLocation=myLocation(moduleName+'.js');

                logger.log('depend '+scriptLocation+'->'+depends);

                

                var module=findModule(scriptLocation,false);
                if (!module.checkLoad()) {
                    logger.log('Script already loaded',scriptLocation);
                    return;
                }

                jsLoad(scriptLocation,depends,function loadModule() {

                    callback.apply(module,arguments);
                    module.loaded();
                });
            });
        });


        var jslibPath=dirname(mySrc('jslib.js'));
//        domAfterLoad.register(function() {
//
//            var cssNode = document.createElement('link');
//            cssNode.type = 'text/css';
//            cssNode.rel = 'stylesheet';
//            cssNode.href = jslibPath+'jslib.css'+(debugMode?'?'+Math.random():'');
//            document.getElementsByTagName("head")[0].appendChild(cssNode);
//        });


        function jsAbilityLinks() {
            var jsEnableLink=document.getElementById('jsEnable');
            var jsDisableLink=document.getElementById('jsDisable');
            if (jsEnableLink && jsDisableLink) {
                var s=jsEnableLink.style.cssText;                
                jsEnableLink.style.cssText=jsDisableLink.style.cssText;
                jsDisableLink.style.cssText=s;
            }
        }

        afterDomLoad.register(jsAbilityLinks);

        document.write("<style>.dynamic {display:none;} </style>");


        document.write("<link rel='stylesheet' type='text/css' href='"+jslibPath+'jslib.css'+(debugMode?'?'+Math.random():'')+"'/>");

        document.write("<!--[if lt IE 7]><style>body .fixed {position: absolute;}</style><![endif]-->");


    };

    if (!Array.prototype.indexOf) {
        Array.prototype.indexOf = function(elt, from) {
            var len = this.length;
            from=from||0;
            if (from < 0) from += len;

            for (var i=from; i < len; i++) {
                if (i in this && this[i] === elt)
                    return i;
            }
            return -1;
        };
    }

    if (!Array.prototype.remove) {
        Array.prototype.remove = function(elt) {
            var count=0;
            while (true) {
                var ind=this.indexOf(elt);
                if (ind<0) return count;
                this.splice(ind,1);
            }            
        };
    }

	if (!Array.prototype.slice) {
        Array.prototype.slice =  function(start, end) {

			if (end == null || end == '') end = this.length;
			if (end < 0) end = this.length + end;
			if (start < 0) start = this.length + start;


			var des = new Array();
			for (var i = 0; i < end - start; i++) {
				des[i] = this[start + i];
			}
			return des;
		};
    }

    if (!String.prototype.leftPad) {
        String.prototype.leftPad = function (l, c) { return new Array(l - this.length + 1).join(c || '0') + this; };
    }
}

