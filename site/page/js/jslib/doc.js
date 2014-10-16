JSLIB.depend('doc',[],function() {

    var logger=this.logger;
    logger.log('DOM manipulation utilities');    

    this.DOM=function(document) {

        this.document=document;

        var dom=this;

        function domConvert(callback,child) {
			
            var failed=false;
            //try {
                if (child == null) {
	                return;
                }

                var tp = typeof(child);				
	            if (tp == "string" || tp == 'number') {
		            callback(document.createTextNode(child));
	            }
	            else if (child.nodeType) {
		            callback(child);
	            }
	            else if (child.content) {		            
		            domConvert(callback, child.content);
	            }
	            else if (child.length) {
		            domConvertList(callback, child,0,child.length);
	            }
	            else {
		            failed = true;
	            }
//            }
//            catch (e) {
//                logger.log('Failed');
//                throw new Error("Unable to add "+child+' '+e);
//            }
            if (failed) {
	            throw new Error("Unable to add "+child);
            }
            
        }
		function domConvertList(callback,list,start,end) {
			for (var i=start;i<end;i++) domConvert(callback,list[i]);
		}

		function addChild(target,child) {
			if (target.content) target=target.content;
			domConvert(function(el){target.appendChild(el);},child);
			return target;
		}
        var addList=this.addList=function addList(target,list,start) {
			if (target.content) target=target.content;
			domConvertList(function(el){target.appendChild(el);},list,start,list.length);
            return target;
        };
        var addAll=this.addAll=function(target,list) {
            addList(target,list,0);
            return target;
        };
        var add=this.add=function(target) {
            addList(target,arguments,1);
            return target;
        };
        this.insertBefore=function(parent,newChild,refChild) {
            if (newChild.content) newChild=newChild.content;
            try {
                if (refChild) parent.insertBefore(newChild,refChild);
                else parent.appendChild(newChild);
            } catch (e) {
                throw new Error("Unable to add "+e);
            }
        };
        var empty=this.empty=function empty(target) {            
            try {
                while (target.firstChild) target.removeChild(target.firstChild);
            } catch (e) {
                throw new Error("Unable to remove. "+e);
            }
            return target;
        };
		this.replaceElement=function replaceElement(target) {
			if (target.content) target=target.content;
			var parentNode=target.parentNode;
			domConvertList(function(el){parentNode.insertBefore(el,target)},arguments,1,arguments.length);
			parentNode.removeChild(target);
		};
        this.replace=this.replaceContent=function replaceContent(target) {
            empty(target);
            addList(target,arguments,1);
            return target;
        };
        var adoptChildren=this.adoptChildren=function(from,to) {
            while (from.firstChild) to.appendChild(from.firstChild);
        };
        var remove=this.remove=function(target) {
            if (target.content) target=target.content;
            try {
                if (target.parentNode) target.parentNode.removeChild(target);
            } catch (e) {
                throw new Error("Unable to remove "+e);
            }
            return target;
        };
        var el=this.el=function (name) {
            return document.createElement(name);
        };
        var div=this.div=function () {
            return addAll(el('div'),arguments);
        };
        var span=this.span=function () {
            return addAll(el('span'),arguments);
        };

        var img=this.img=function (src) {
            var e=el("img");
            e.setAttribute("src",src);
            return e;
        };

        var tbody=this.tbody=function () {
            var e=el('tbody');
            return addAll(e,arguments);
        };

        var table=this.table=function () {
            var t=el('table');
            t.style.borderCollapse='collapse';
            return addAll(t,arguments);
        };
        var td=this.td=function () {
            return addAll(el('td'),arguments);
        };



        this.tr = function () {
            return dom.addAll(el('tr'), arguments);
        };

        this.row = function () {
            return dom.addCells(el('tr'), arguments);
        };

        this.addCells = function (target, cells, start) {
	        return addElements(target, 'td', cells, start);
        };

	    this.list = function() {
		    return dom.addListItems(el('ul'), arguments);
	    };

	    this.orderedList = function() {
		    return dom.addListItems(el('ol'), arguments);
	    };

	    this.addListItems = function(target, items, start) {
		    return addElements(target, 'li', items, start);
	    };

	    var addElements = this.addElements = function(target, tag, elements, start) {
	        start = start || 0;
	        for (var i = start, n = elements.length; i < n; i++) {
	            if (elements[i] != null) {
		            addChild(target, addChild(dom.el(tag), elements[i]));
	            }
	        }
	        return target;
	    };

        var Input=this.Input=function Input(inp,value) {
            value=value||'';
            this.content=inp;
            this.value=inp.value=value;
            var changeListeners=[];
            var previous=inp.value;
            var e=this;
            function changed() {
                e.value=inp.value;
                if (previous==inp.value) return;
                previous=inp.value;
                for (var i=0;i<changeListeners.length;i++) changeListeners[i](inp.value);
            }

            dom.addListener(inp,"keyup", function() {
                changed();
            });

            dom.addListener(inp,"change", function() {
                changed();
            });

            this.onvaluechange=function (action) {
                changeListeners.push(action);
                return this;
            };
            this.oncompletion=function(action) {
                dom.oncompletion(inp,action);
            };
            this.setValue=function(v) {
                inp.value=v;
                changed();
            };
            this.getValue=function(v) {
                return inp.value;
            };
            this.setClass=function(name) {
                inp.className=name;
                return this;
            };
        };


        this.input=function (value,width,action) {
            var inp=el('input');

            var e=new Input(inp,value);

            inp.type='text';
            if (value) inp.value=value;
            if (width) {
	            //inp.size=width;
	            inp.style.width = width + 'em';
            }
	        else {
	            inp.style.width='10em';
            }
            inp.style.fontSize='80%';
            if (action) e.onvaluechange(action);

            return e;
        };


        this.textarea=function (value,cols,rows) {

            var inp=el('textarea');

            var e=new Input(inp,'');

            if (cols) {
                inp.setAttribute("cols",cols);
                inp.setAttribute("wrap","PHYSICAL");
            } else {
                inp.style.width='90%';
            }

            if (rows)
                inp.setAttribute("rows", rows);
            else {
                function resize() {
                    var lines=inp.value.replace(/[^\n]/g,"").length+1;
                    inp.setAttribute("rows", lines);
                }
                e.onvaluechange(resize);
            }
            e.setValue(value||'');

            return e;
        };



        this.setCheckbox=function(cb,state) {
            cb.checked=state;
            cb.defaultChecked=state;
        };

        this.checkbox=function(state,action) {
            var e=el('input');
            e.type='checkbox';
            dom.setCheckbox(e,state);
            if (action) dom.oncheckboxchange(e,action);
            return e;
        };
        this.fakecheckbox=function(state,action) {
            var e=el('input');
            e.style.width='1em';
            e.type='text';
            e.value=state?'X':'';
            //if (action) onchange(e,function(){action(e.checked);logging.log('Change!',e.checked);});
            onclick(e,function(){state=!state;e.value=state?'X':'';action(state);});
            return e;
        };

        var button=this.button=function(action) {
            var e=el('button');
            if (action) onclick(e,action);
            addList(e,arguments,1);
            return e;
        };

        this.imgbutton=function(action,imgsrc) {
            var i=img(imgsrc);
            var e=button(action,i);
            e.style.padding='0';
            e.style.margin='0';
            i.style.margin='-1px';
            return e;
        };

        this.imgtextbutton=function(action,imgsrc,text) {
            var c=dom.classDiv('iconholder',img(imgsrc),dom.div(text));
            dom.onclick(c,action);
            return c;
        };

        this.imglink=function(href,imgsrc) {

            var e=el('a');
            if (typeof href=='function') {
                onclick(e,function(){e.href=href();});
            } else {
                e.href=href;
            }
            e.target='_blank';
            var img=dom.img(imgsrc);
            img.style.border='none';
            dom.add(e,img);
            return e;
        };

        this.link=function(href) {
            var e=el('a');
            e.href=href;
            e.target='_blank';
            addList(e,arguments,1);
            return e;
        };

        this.anchor=function(name) {
            var e=el('a');
            e.name=name;
            addList(e,arguments,1);
            return e;
        };

        this.where = function(target, relative) {
            var left = 0;
            var top = 0;
            if (!relative) {
	            relative = document.body;
            }
            var parent = target;
            
            while (parent && (parent.offsetParent != relative.offsetParent)) {
                //logger.log('Where: ', parent, parent.offsetLeft, parent.offsetTop, parent.offsetParent);
                left += parent.offsetLeft;
                top += parent.offsetTop;
                parent = parent.offsetParent;
            }

            var pos = {
	            left:left,
	            top:top,
                right:left + target.offsetWidth,
	            bottom:top + target.offsetHeight,
                width:target.offsetWidth,
	            height:target.offsetHeight,
                toString:function() { return "[(" + left + ", " + top + "), (" + pos.right + ", " + pos.bottom + ")]"; }
            };

	        return pos;
        };

	    this.whereAbs = function(target, relative) {
		    var pos = dom.where(target, relative);

		    var deltaX, deltaY;
		    if (window.pageXOffset) {
			    deltaX = window.pageXOffset;
			    deltaY = pageYOffset;
		    }
		    else {
			    var d = document.documentElement;
		        var b = document.body;
			    deltaX = (d.scrollLeft + b.scrollLeft);
			    deltaY = (d.scrollTop + b.scrollTop);
		    }

		    pos.left += deltaX;
		    pos.top += deltaY;

		    pos.right += deltaX;
		    pos.bottom += deltaY;

		    return pos;
	    };

        this.cssFloat=function(target,where) {
            target.style.cssFloat=where;
            target.style.styleFloat=where;
            return target;
        };

        this.floatDiv=function (where) {
            return dom.cssFloat(addList(el('div'),arguments,1),where);
        };

        this.style=function (el,style,prevStyle) {
            for (var x in style) {
				try {
				    if (prevStyle) prevStyle[x]=el.style[x];
					el.style[x]=style[x];
				} catch (e) {logger.log('Style set failed',x,style[x]);}
			}
            return el;
        };

		this.hoverStyle=function (el,style) {
			var prevStyle;
			dom.onmouseover(el,function(){dom.style(el,style,prevStyle={})});
			dom.onmouseout(el,function(){dom.style(el,prevStyle)});

            return el;
        };

        this.styleSpan=function(style) {
            return dom.style(addList(el('span'),arguments,1),style);
        };

        this.classSpan=function(className) {
            var e=addList(el('span'),arguments,1);
            e.className=className;
            return e;
        };

        this.classDiv=function(className) {
            var e=addList(el('div'),arguments,1);
            e.className=className;
            return e;
        };

		
		var classEl = this.classEl = function(elType, className) {
			var e = el(elType);
			e.className = className;
			return e;
		};

	    var classTable = this.classTable = function(className) {
	        var t = classEl('table', className);
	        t.style.borderCollapse='collapse';
	        return addList(t, arguments, 1);
	    };

	    var classTr = this.classTr = function(className) {
	        return addList(classEl('tr', className), arguments, 1);
	    };

	    var classTd = this.classTd = function(className) {
	        return addList(classEl('td', className), arguments, 1);
	    };

		this.setMaxHeight=function(elt,height) {
			var useMaxHeight=typeof document.body.style.maxHeight!="undefined";
			if (useMaxHeight) elt.style.maxHeight=height+"px";
			else elt.style.height=height+"px";
		};

		this.setMaxWidth=function(elt,width) {
			var useMaxHeight=typeof document.body.style.maxHeight!="undefined";
			if (useMaxHeight) elt.style.maxWidth=width+"px";
			else elt.style.width=width+"px";
		};

        this.styleDiv=function(style) {
            return dom.style(addList(el('div'),arguments,1),style);
        };

        this.displayDiv=function (display) {
            var el=dom.div();
            el.style.border = '1px solid red';
            el.style.display=display;
            addList(el,arguments,1);
            return el;
        };

        function showElement(target) {
            if (!target || !target.style) return;
            if (target.show) {
                target.show();
            } else {
                if (target.style.display=='none') target.style.display='block';
                if (target.style.visibility=='hidden') target.style.visibility='show';
            }
            showElement(target.parentNode);
            
        }

        this.show=function (target) {
            showElement(target)            
        };

        var move=this.move=function(target,x,y) {
            target.style.left=x+"px";
            target.style.top=y+"px";
            return target;
        };


        this.find=function(target,el,className) {

            if (!target) throw new Error("Null target");
            var childer=target.getElementsByTagName(el);
            for (var i=0;i<childer.length;i++)
                if (!className || childer[i].className.split(' ').indexOf(className)>=0) return childer[i];
            return null;
        };
        this.findAll=function(target,tagName,className) {

            if (!target) throw new Error("Null target");
            var childer=target.getElementsByTagName(tagName);
            //if (arguments.length==2) return childer;            -- always return non-live lists
            var results=[];
            for (var i=0;i<childer.length;i++)
                if (!className || childer[i].className.split(' ').indexOf(className)>=0) results.push(childer[i]);
            return results;
        };
        this.findParent=function findParent(target,el,className) {
            el=el.toUpperCase();
            while (target) {
                if (target.nodeName.toUpperCase()==el && (!className || className==target.className)) return target;
                target=target.parentNode;
            }
            return null;
        };

        this.getInnerText=function(element) {
            if (element.nodeValue) return element.nodeValue;
            var t="";
            var c=element.firstChild;
            while (c) {t+=this.getInnerText(c);c=c.nextSibling;}
            return t;
        };

        this.attributes=function(element) {
            var o={};
            var a=element.attributes;
            for (var i=0;i<a.length;i++) o[a[i].name]=a[i].value;
            return o;
        };


        var keycode=this.keycode={
            Tab:9,
            Enter:13,

            Escape:27,
            PageUp:33,
            PageDown:34,
            End:35,
            Home:36,
            Left:37,
            Up:38,
            Right:39,
            Down:40,
            Insert:45,
            Delete:46,

            F1:112,
            F2:113,
            F3:114,
            F4:115,
            F5:116,
            F6:117,
            F7:118,
            F8:119,
            F9:120,
            F10:121,
            F11:122,
            F12:123,

            KEY_0:48,
            KEY_1:49,
            KEY_2:50,
            KEY_3:51,
            KEY_4:52,
            KEY_5:53,
            KEY_6:54,
            KEY_7:55,
            KEY_8:56,
            KEY_9:57



        };

        function eventWrap(action) {
            return logger.wrap(function(e) {
                e=e||window.event;
                if (action(e)==false) dom.stopDefault(e);
            });
        }

        var addListener=this.addListener=function (target,name,action) {
            if (target.content) target=target.content;
            action=eventWrap(action);
            if (target.addEventListener) target.addEventListener(name,action,false);
            else if (target.attachEvent) target.attachEvent('on'+name,action);
            else throw 'No attachment for '+target;
            function remove() {
                if (target.removeEventListener) target.removeEventListener(name,action,false);
                else target.detachEvent('on'+name,action);
            }
            return {remove:remove};
        };


        this.oncheckboxchange=function(cb,action) {            
            onclick(cb,function(event){
                action && action(cb.checked);
                cb.defaultChecked=cb.checked;
            });
        };

        var oncompletion=this.oncompletion=function (inp,action) {
            return addListener(inp,"keyup", function(ev) {
                if (ev.keyCode==13) action(inp.value);
            });
        };
        var onchange=this.onchange=function (inp,action) {
            return addListener(inp,"change", action);
        };
        var onclick=this.onclick=function(target,action) {
            return addListener(target,'click',action);
        };
        var onmousedown=this.onmousedown=function(target,action) {
            return addListener(target,'mousedown',action);
        };
        var onmousescroll=this.onmousescroll=function(target,action) {
            var l1=addListener(target,'DOMMouseScroll',action);
            var l2=addListener(target,'mousewheel',action);
            return {remove:function(){l1.remove();l2.remove();}};
        };
        var onmouseover=this.onmouseover=function(target,action) {
            return addListener(target,'mouseover',action);
        };
        var onmouseup=this.onmouseup=function(target,action) {
            return addListener(target,'mouseup',action);
        };
        var onmouseout=this.onmouseout=function(target,action) {
            return addListener(target,'mouseout',action);
        };
        var onmousemove=this.onmousemove=function(target,action) {
            return addListener(target,'mousemove',action);
        };
        var onkeyup=this.onkeyup=function(target,action) {
            return addListener(target,'keyup',action);
        };
        var onkeydown=this.onkeydown=function(target,action) {
            return addListener(target,'keydown',action);
        };
        var onkeypress=this.onkeypress=function(target,action) {
            return addListener(target,'keypress',action);
        };
        var onfocus=this.onfocus=function(target,action) {
            return addListener(target,'focus',action);
        };
        var onblur=this.onblur=function(target,action) {
            return addListener(target,'blur',action);
        };

        var onscroll=this.onscroll=function(target,action) {
            return addListener(target,'scroll',action);
        };


        var stop=this.stop=function(e) {
            e=e||window.event;
            e.cancelBubble = true;
            if (e.stopPropagation) e.stopPropagation();
            e.returnValue=false;
            if (e.preventDefault) e.preventDefault();
            return e;
        };

        var stopPropagation=this.stopPropagation=function(e) {
            e=e||window.event;
            e.cancelBubble = true;
            if (e.stopPropagation) e.stopPropagation();
            return e;
        };

        var stopDefault=this.stopDefault=function(e) {
            e=e||window.event;
            e.returnValue=false;
            if (e.preventDefault) e.preventDefault();
            return e;
        };

        var sendEvent=this.sendEvent=function(target,eventName,eventClass,props) {

            if (!target) return;
            try {
                var e={};
                if (document.createEventObject) {
                    e=document.createEventObject();
                    //logger.log('createEventObject');
                } else if (document.createEvent) {
                    e=document.createEvent(eventClass);

                    //logger.log('createEvent '+eventClass);
                    if (e.initMouseEvent) {
                        //logger.log('initMouseEvent');
                        e.initMouseEvent(eventName,true,true,window,0,0,0,0,0,false,false,false,false,0,null);
                    } else if (e.initKeyEvent) {
                        //logger.log('initKeyEvent');
                        e.initKeyEvent(eventName,true,true,null,false,false,false,false,props['keyCode'],props['keyCode']);
                    } else {
                        //logger.log('initEvent');
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
                    //logger.log('fireEvent');
                } else if (target.dispatchEvent) {
                    target.dispatchEvent(e);
                    //logger.log('dispatchEvent');
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
            //sendEvent(input,'keyup','HTMLEvents',{keyCode:13});
            sendEvent(input,'keyup','KeyboardEvent',{keyCode:13});
        };

        this.sendClick=function (target) {
            //sendEvent(target,'click','HTMLEvents',null);
            sendEvent(target,'click','MouseEvents',null);
        };

        
        dom.schedule=function(action,delayMS) {
            var handle=window.setTimeout(logger.wrap(action),delayMS);
            return {cancel:function(){window.clearTimeout(handle);}};
        };

        dom.scheduleInterval=function(action,delayMS) {
            var handle=window.setInterval(logger.wrap(action),delayMS);
            return {cancel:function(){window.cancelInterval(handle);}}
        };

        if (document.body) {
            dom.onclick(document.body,function(event) {
                if (event.ctrlKey) {
                    dom.where(event.target);
                }
            });
        }

    };
});