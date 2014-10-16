

var beforeLoad=true;

function findPos(obj) {

    var left=0;
	var top=0;

    var parent=obj;
    while (parent && parent.offsetParent) {
		left += parent.offsetLeft;
		top += parent.offsetTop;
		parent = parent.offsetParent;
	}

    var right=left+obj.offsetWidth;
    var bottom=top+obj.offsetHeight;

    return [left,top,right,bottom];
}

function areaPos(area,left,top) {
    var coords=area.coords.split(/ |\,/);
    var right=left+parseInt(coords[2]);
    var bottom=top+parseInt(coords[3]);
    left+=parseInt(coords[0]);
    top+=parseInt(coords[1]);
    return [left,top,right,bottom];
}


function positionRelative(anchor,popup,horizontal,vertical,area) {

    var position=findPos(anchor);
    if (area) position=areaPos(area,position[0],position[1]);

    var containerPos=findPos(popup.offsetParent);

    var clientWidth=Math.max(document.body.scrollWidth,document.documentElement.scrollWidth);
    var clientHeight=Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);

    var x;
    var y;
    
    if (!horizontal || horizontal=="auto") {
        //x=Math.max(0,Math.min(position[0],clientWidth-popup.offsetWidth));
        x=Math.max(0,Math.min(clientWidth-popup.offsetWidth,position[2]-popup.offsetWidth));
    } else if (horizontal=="alignleft") {
        x=position[0];
    } else if (horizontal=="alignright") {
        x=position[2]-popup.offsetWidth;
    } else if (horizontal=="right") {
        x=position[2];
    } else if (horizontal=="left") {
        x=position[0]-popup.offsetWidth;
    }

    if (!vertical || vertical=="auto") {
        if ((popup.offsetHeight+position[3])>clientHeight) vertical="top";
        if (position[1]-popup.offsetHeight<0) vertical="bottom";
    }

    if (vertical=="top")
        y=position[1]-popup.offsetHeight;
    else
        y=position[3];

    popup.style.left=(x-containerPos[0])+"px";
    popup.style.top=(y-containerPos[1])+"px";
    popup.style.visibility='visible';



    //alert("below "+belowEl.style.left+" "+belowEl.style.top)

    return popup;
}






function findChildrenByNameClass(parent,name,className) {
    var x=parent.getElementsByTagName(name);
    var y=new Array();
    for (var i=0;i<x.length;i++)
        if (x[i].className==className) y.push(x[i]);
    return y;
}

function findChildByNameClass(parent,name,className) {
    var x=parent.getElementsByTagName(name);
    var y=new Array();
    for (var i=0;i<x.length;i++)
        if (x[i].className==className) return x[i];
    return null;
}

function findParentByClass(parent,name) {
    while (parent.parentNode && parent.className!=name) parent=parent.parentNode;
    if (!parent.parentNode) {return document.documentElement;}
    return parent;
}

function findParentByName(parent,name) {
    while (parent.parentNode && parent.nodeName!=name) {
        parent=parent.parentNode;
        debug("SEEK "+parent.nodeName);
    }
    if (!parent.parentNode) return null;
    return parent;
}

function eid(id) {
    return document.getElementById(id);
}

function popupWidth(elt,popup) {
    if (popup==null) return null;
    popup.style.width=elt.offsetWidth+"px";
    positionRelative(elt,popup);

    return popup;
}

function popupLeft(elt,popup) {
    if (popup==null) return null;
    positionRelative(elt,popup,"alignleft");
    return popup;
}

function popupAuto(elt,popup) {
    if (popup==null) return null;
    positionRelative(elt,popup);
    return popup;
}

function popupTop(elt, popup) {
	if (popup != null) {
		positionRelative(elt, popup, "auto", "top");
	}
	return popup;
}


function popupRight(elt,popup) {
    if (popup==null) return null;
    positionRelative(elt,popup,"alignright");
    return popup;
}

function findPopup(elt) {
    if (elt==null) return null;
    var x=elt.getElementsByTagName('div');
    for (var i=0;i<x.length;i++) if (x[i].className=='popup') return x[i];
    return null;
}

function findParentPopup(elt,name) {
    return findPopup(findParentByName(elt,name));
}

function popupImage(img,popup,x,y) {
    if (popup==null) return null;
    positionRelative(img,popup,true,false,x,y);
    return popup;
}

function hide(elt) {
    elt.style.visibility='hidden';
}

function show(elt) {
    elt.style.visibility='visible';
}



function eltString(e) {
    if (e==null) return null;
    var s="";
    while (e) {
        var id="";
        var name=e.nodeName;
        if (e.id) id="[id:"+e.id+"]";
        else {
            var i=0;            
            var n;
            if (e.parentNode) n=e.parentNode.firstChild;
            while (n) {
                if (n==e) id="["+i+"]";
                if (n.nodeName==name) i++;
                n=n.nextSibling;
            }
        }
        s="/"+name+id+s;
        if (e.parentNode) e=e.parentNode; else e=null;
    }
    return s;
}

var terminal=document.createElement("terminal");

var start=new Date().getTime();

function debug(x) {
    x=(new Date().getTime()-start)+" "+x;
    var de=document.createElement("div");
    de.appendChild(document.createTextNode(x));
    terminal.appendChild(de);
}

function clearTerminal() {
    terminal.innerHTML="";
}

function ctx(text) {
    return document.createTextNode(text);
}

function spanclick(contents,onclick) {
    var node = document.createElement("span");
    //node.style.cursor="pointer";
    node.style.padding= "0 1px";
    node.appendChild(contents);
    node.onclick=onclick;
    return node;
}

var terminalHolder;

function closeTerminal() {
    terminal.parentNode.removeChild(terminal);
    terminalHolder.parentNode.removeChild(terminalHolder);
}

function showTerminal() {
    var holder=document.createElement('div');
    var drag=document.createElement("div");
    drag.appendChild(document.createTextNode("Terminal"));
    drag.style.backgroundColor='#77f';
    drag.onmousedown=dragStart;
    var toolbar=document.createElement("div");
    var exec=document.createElement("input");
    exec.type="text";
    exec.onkeyup=execute;
    toolbar.appendChild(spanclick(ctx("Clear"),clearTerminal));
    toolbar.appendChild(spanclick(ctx("Close"),closeTerminal));
    toolbar.appendChild(exec);
    holder.appendChild(drag);
    holder.appendChild(toolbar);
    holder.appendChild(terminal);
    holder.style.width='800px';
    holder.style.display='block';
    holder.style.position='absolute';
    holder.style.zIndex='2000';
    holder.style.backgroundColor='#ccf';
    holder.style.border='1px solid black';
    holder.style.padding='2px';
    document.body.appendChild(holder);
    terminalHolder=holder;
}

var startDragElement;
var startDragX;
var startDragY;

function execute(evt) {
    if (!evt) evt=window.event;
    if (evt.keyCode==13) {
        debug(eval(this.value));
        return false;
    }
    return true;
}

function dragStart(evt) {
    if (!evt) evt=window.event;
    startDragElement=this.parentNode;
    var parent=startDragElement.offsetParent;
    startDragX=startDragElement.offsetLeft-parent.offsetLeft-evt.clientX;
    startDragY=startDragElement.offsetTop-parent.offsetTop-evt.clientY;
    document.body.onmousemove=dragMove;
    document.body.onmouseup=dragEnd;

    return false;

}

function dragMove(evt) {
    if (!evt) evt=window.event;
    var newX=evt.clientX+startDragX;
    var newY=evt.clientY+startDragY;
    startDragElement.style.left=newX+"px";
    startDragElement.style.top=newY+"px";
    return false;
}

function dragEnd(evt) {
    dragMove(evt);    
    document.body.onmousemove=null;
    document.body.onmouseup=null;
    return false;
}

function pickup(elt,evt) {
    startDragElement=elt;
    startDragElement.style.border="1px dotted black";
    startDragElement.style.margin="-1px";
    document.body.onmousemove=moveit;
    document.body.onmouseup=finishmove;
    debug("Dragging "+startDragElement.offsetLeft+","+startDragElement.offsetTop);
    return false;
}

function moveit(evt) {
    var target;
    if (evt.target) target=evt.target;
    else target=evt.srcElement;

    while (target && target.parentNode!=startDragElement.parentNode) {
        target=target.parentNode;
    }
                                                                                
    if (!target || target==startDragElement) return false;

    if (target.offsetTop>startDragElement.offsetTop) target=target.nextSibling;

    if (target) startDragElement.parentNode.insertBefore(startDragElement, target);
    else startDragElement.parentNode.appendChild(startDragElement);

    return false;
}

function finishmove(evt) {
    moveit(evt);
    startDragElement.style.border="";
    startDragElement.style.margin="";
    document.body.onmousemove = null;
    document.body.onmouseup = null;
    startDragElement.getElementsByTagName("input")[0].onchange();
    return false;
}

function showPixies() {
    var em=document.getElementsByTagName("em");
    for (var i=0;i<em.length;i++) {
        em[i].style.display='inline';
    }

    var frm=document.getElementsByTagName("iframe");
    debug("Showing:"+frm.length+" iframes");
    for (var i=0;i<frm.length;i++) {
        frm[i].style.visibility='visible';
        frm[i].style.height='100px';
    }
}

function showEm() {
    var em=document.getElementsByTagName("em");for (var i=0;i<em.length;i++) {em[i].style.display='inline';}
}





function formSubmit(event,form,targetName,loadingID,name,value) {
    //if (event!=null) event.preventDefault();
    form.target=targetName;
    form[name].value=value;
    if (loadingID) eid(loadingID).style.display='inline';
    form.submit();
    debug("Submitted form "+form.action+"->"+form.target);
    return false;
}

function formActionSubmit(event,elt,action) {
    //if (event!=null && event.preventDefault) event.preventDefault();
    var form=findParentByName(elt,"FORM");
    form.action=action;
    form.submit();
    return false;
}



function autoload() {
    if (!beforeLoad) return;
    beforeLoad=false;
    
    debug("autoload");

    automatic(document.body);


}

if (window.attachEvent) {
    window.attachEvent('onload',autoload);
}


if (document.addEventListener) {
    document.addEventListener("DOMContentLoaded", function() {debug("domcontentloaded");autoload();}, false);
    window.addEventListener("load", autoload, false);
}




function DynamicLoad(name,container,target,result,keepLink) {


    var destination = findChildByNameClass(container, "span", "target");

    var holder = document.createElement("span");
    if (destination) destination.parentNode.insertBefore(holder, destination);
    else container.appendChild(holder);

    if (!destination) destination = document.createElement("span");
    holder.appendChild(destination);


    var error = document.createElement("span");
    error.appendChild(document.createTextNode("Error"));
    error.style.display = "none";


    var iframe;
    try {
        // wacky internet explorer doesn't permit you to set an onload of an iframe
        iframe = document.createElement("<iframe onload='this.onchange()' name='" + target + "'></iframe>");
    } catch (e) {
        // w3c
        iframe = document.createElement("iframe");
    }
    iframe.className = "data";
    if (target) iframe.name = target;



    var previous = null;




    var info = document.createElement("span");    
    info.style.display = "none";


    var loading = document.createElement("span");

    info.appendChild(loading);

    if (keepLink=="download") {
        loading.appendChild(document.createTextNode(" Check your downloads for progress. "));
        info.appendChild(document.createTextNode("Bookmark or download again: "));
    } else {
        loading.appendChild(document.createTextNode("Loading"));
        var img = document.createElement("img");
        img.src = "image/ajax-loader.gif";
        loading.appendChild(img);
    }



    holder.appendChild(info);

    var downloadLink = document.createElement("a");
    var hrefText = document.createTextNode(name);
    downloadLink.appendChild(hrefText);
    downloadLink.onclick = function(event) {
        if (event.shiftKey) {
            iframe.className = "";
            return false;
        }
    };
    if (keepLink) {
        info.appendChild(downloadLink);
        info.appendChild(document.createTextNode(" "));
    }


    function reload(event) {
        debug("loaded1");
        if (beforeLoad) return;
        debug("loaded2");

        loading.style.display = 'none';

        debug("loaded loc " + iframe.contentWindow.location);
        debug("loaded doc " + iframe.contentWindow.document);
        var results = iframe.contentWindow.document.getElementById("results");
        if (!results) {
            //error.style.display="inline";
            return;
        }
        debug("get HTML");
        var html = results.innerHTML;
        debug("write HTML:"+eltString(destination)+' '+html);
        //destination.style.display="none";
        destination.innerHTML = html;
        debug("processing results");
        if (result) result(destination);
        else automatic(destination);
        if (!target) iframe.onload = null;
        //destination.style.display="block";
        debug("done");
    }


    holder.appendChild(iframe);

    this.load = function(src) {
        if (src == previous) return;
        debug("load:");
        debug(src);
        info.style.display = "inline";
        loading.style.display = "inline";
        loading.href = src;
        error.style.display = "none";
        downloadLink.href = src;
        //debugLink.style.display="inline";
        debug("onload *");
        iframe.onchange = reload;
        iframe.onload = iframe.onchange;
        previous = src;
        src += "&embed";
        try {
            // prevent history pollution, if possible
            iframe.contentWindow.location.replace(src);
        } catch(e) {
            // IE
            iframe.src = src;
        }
        debug("loading");
    };

    iframe.onchange = function() {};
    iframe.onload = iframe.onchange;


    this.target = target;
}


function popupClick(elt,popup,evt) {

    if (!elt.onclick) elt.onclick=stopPropagation;
    var parent=findParentByClass(elt.parentNode,"popup");
    if (parent.onclick) parent.onclick(evt);

    if (popup==null) return null;

    //debug("PPClick "+eltString(elt)+" "+eltString(popup)+" "+eltString(parent));
    parent.onclick=function(event) {
        //debug("unClick "+eltString(this)+" "+eltString(popup)+" "+eltString(parent));
        if (popup.onclick) popup.onclick(event);
        hide(popup);
        stopPropagation(event);
        parent.onclick=stopPropagation;
    };

    popup.onclick=stopPropagation;

    stopPropagation(evt);

    show(popup);

    return popup;
}

function stopPropagation(evt) {
    if (!evt) evt=window.event;
    if (evt.stopPropagation) evt.stopPropagation(); else evt.cancelBubble = true;
}



function Popup(holder) {

    var popup=findChildByNameClass(holder,"div","popup");
    if (!popup) {
        popup=document.createElement("div");
        popup.className="popup";
        holder.appendChild(popup);
    }

    var closeLink=document.createElement("img");
    closeLink.src = "image/cross.png";

    closeLink.style.cssFloat="right";
    closeLink.style.styleFloat="right";

    if (popup.firstChild) popup.insertBefore(closeLink,popup.firstChild); else popup.appendChild(closeLink);

    if (!popup.onclick) popup.onclick=stopPropagation;
    var parent=findParentByClass(popup.parentNode,"popup");

    this.hide=function(event) {
        if (popup.onclick) popup.onclick(event);
        hide(popup);
        stopPropagation(event);
        parent.onclick=stopPropagation;
    }

    closeLink.onclick=this.hide;

    this.show=function(anchor,event,left,top,area) {
        positionRelative(anchor,popup,left,top,area);
        if (parent.onclick) parent.onclick(event);
        parent.onclick=this.hide;
        popup.onclick=stopPropagation;
        stopPropagation(event);
        show(popup);
    };
    this.element=popup;
}

function dynamicPopup(anchor) {
    anchor=anchor.parentNode;
    
    var span = document.createElement("span");
    anchor.parentNode.insertBefore(span, anchor);
    span.appendChild(anchor);

    var popup=new Popup(span);
    var dynamic=new DynamicLoad("Information",popup.element,anchor.target);
    anchor.onclick=function(event) {
        popup.show(anchor,event);
        dynamic.load(this.href);
        return false;
    };
    
}



function dynamicAreaPopup(anchor) {
    var span = document.createElement("span");

    var parent=findParentByName(anchor,"MAP");
    var img=parent;
    while (img && img.nodeName!='IMG') img=img.previousSibling;
    parent.parentNode.appendChild(span);

    var popup=new Popup(span);
    var dynamic=new DynamicLoad("Information",popup.element,anchor.target);
    anchor.onclick=function(event) {
        popup.show(img,event,"right","bottom",anchor);
        dynamic.load(this.href);
        return false;
    }

}

function formURL(form) {

    var url=form.action+"?";

    var elts=form.elements;
    for (var i=0;i<elts.length;i++) {
        if (elts[i].className=='noauto') continue;
        if (elts[i].type=='submit') continue;
        if ((elts[i].type=='checkbox' || elts[i].type=='radio') && !elts[i].checked) continue;
        if (elts[i].type=='text' && elts[i].value=='') continue;
        if (elts[i].name && elts[i].nodeName!='BUTTON') {
            url=url+elts[i].name;
            if (elts[i].value)
                url=url+"="+encodeURIComponent(elts[i].value);
            url+="&";
        }        
    }
    return url;
}

function automaticForm(form,changeListener,preUpdate) {


    var dynamic=new DynamicLoad("Information",form);

    var timeout;

    function refresh() {
        dynamic.load(formURL(form));
    }
    function Group(master) {
        //debug("Group "+master);
        var count=0;
        var list=new Array();
        this.add=function (item) {
            list[count]=item;
            count=count+1;
            //debug("Adding "+item+" "+count);

        };

        master.onclick=function(event) {
            //debug("Switching "+master.checked+" "+count);
            for (var i=0;i<count;i++) list[i].checked=master.checked;
            return true;
        };
        //debug("Done "+master.onchange+" "+master.nodeName);
    }
    var group;
    var inp=form.elements;
    for (var i=0;i<inp.length;i++) {
        var input=inp[i];

        if (changeListener) {
            input.onkeyup=function(event) {
                if (timeout) window.clearTimeout(timeout);
                timeout=window.setTimeout(refresh,2000);
            }
            input.onchange=input.onkeyup;
        }
        if (input.className=='refresh') {
            input.onclick=function() {dynamic.load(formURL(form)+input.name+"="+input.getAttribute("value"));return false;}
        }
        if (input.className=='selectall') {
            group=new Group(input);
        } else if (input.type=='checkbox') {
            if (group) group.add(input);
        }

    }

    //if (preUpdate) refresh();


}

function clickPopup(span) {

    var popup=new Popup(span);

    span.onclick=function(event) {
        popup.show(span,event);
    };

}

function replaceAnchor(i) {
    var anchor=i.parentNode;
    var span=document.createElement("span");
    var dynamic=new DynamicLoad("Information",span,anchor.target);
    dynamic.load(anchor.href);
    anchor.style.display='none';
    anchor.parentNode.insertBefore(span,anchor);
}

function useSelection(anchor) {
    anchor.onclick=function() {
        eid('termList').value=eid('selectionlist').value;
        return false;
    };
}

function superSplit(s) {
    s=s.replace(/^[, \t\n]+|[, \t\n]+$/g, '');
    if (s=='') return new Array();
    return s.split(/[, \t\n]+/);
}


function summarize(form) {
    var values=new Object();

    for (var i=0;i<form.elements.length;i++) {
        var v;
        var inp=form.elements[i];
        if ((inp.type == 'radio' || inp.type == 'checkbox') && !inp.checked)
            v=new Array();
        else
            v = superSplit(inp.value);
        var n = inp.name;
        var a = values[n];
        if (a) a = a.concat(v); else a = v;
        values[n] = a;
    }
    
    for (var x in values) {
        var summary = document.getElementById("summary-"+x);
        if (!summary) continue;
        var ct=values[x].length;
        var data=ct;
        if (ct==1) data=values[x][0];
        if (ct==0) data="Any";
        summary.innerHTML=data;
    }
}

function clearRows(before,after) {
    while (before.nextSibling && before.nextSibling!=after)
        before.parentNode.removeChild(before.nextSibling);
}

function replaceRows(before,after,source) {
    clearRows(before,after);
    var rows=source.getElementsByTagName("table")[0].rows;
    var i=rows.length-1;
    while (i>=0) {
        var row=rows[i];
        before.parentNode.insertBefore(row,after);
        automatic(row);
        after=row;
        i--;
    }

}


function annotationForm(form) {

    var statsBefore=findChildByNameClass(form,"tr","stats-before");
    var statsAfter=findChildByNameClass(form,"tr","stats-after");
    var annotationBefore=findChildByNameClass(form,"tr","annotation-before");
    var annotationAfter=findChildByNameClass(form,"tr","annotation-after");

    var refreshUI=findChildByNameClass(form,"div","refresh");

    var downLoader=new DynamicLoad("Download",annotationAfter,null,null,"download");

    var statsLoader=new DynamicLoad("Full Statistics Report",annotationAfter,null,
            function(element) {
                replaceRows(statsBefore,statsAfter,element);
            },"keep");
    var annotationLoader=new DynamicLoad("Sample",annotationAfter,null,
            function(element) {

                replaceRows(annotationBefore,annotationAfter,element);
                var page=findChildrenByNameClass(form,"button","refresh");
                for (var i=0;i<page.length;i++) refreshLink(page[i]);
                debug("Activated "+page.length+" buttons");
                statsLoader.load(formURL(form)+"&format=stats");
            },"keep");


    function refresh(name,value) {
        refreshUI.style.display='none';
        var url = formURL(form);
        if (name) url=url+"&"+name;
        if (name && value) url=url+"="+value;
        annotationLoader.load(url+"&format=sample");
    }

    function refreshLink(elt) {
        elt.onclick=function(){refresh(elt.name,elt.value);return false;}
    }

    function changed() {
        summarize(form);
        clearRows(statsBefore,statsAfter);
        clearRows(annotationBefore,annotationAfter);
        refreshUI.style.display='block';
    }

    function refreshElement(elt) {
        elt.onfocus=function() {
            var old=elt.value;
            var was=elt.checked;
            function check() {if (elt.value!=old || elt.checked!=was) changed();}
            elt.onblur=check;
            elt.onkeyup=check;            
        }
        elt.onchange=changed;
    }

    function downloadButton(elt) {
        elt.onclick = function() {downLoader.load(formURL(form)+"&"+elt.name+"="+elt.value);return false;}
    }

    for (var i=0;i<form.elements.length;i++) {
        var elt = form.elements[i];
        if (elt.nodeName=='BUTTON' && elt.className=='refresh')
            refreshLink(elt);
        else if (elt.nodeName=='INPUT' && elt.className=='download')
            downloadButton(elt);
        else if (elt.nodeName=='INPUT' || elt.nodeName=='TEXTAREA')
            refreshElement(elt);
    }


    changed();
    refresh();

    form.style.display='block';
}


function noscript(div) {
    div.style.display='none';
}




function automatic(target) {
    try {
        debug(eltString(target));
    debug("processing divs");
        var divs=target.getElementsByTagName("div");
        for (var i=0;i<divs.length;i++) {
            var div=divs[i];
            if (div.className=='autotab') autoTab(div);
            if (div.className=='noscript') noscript(div);
        }
        debug("processing anchors");
        var anchors=target.getElementsByTagName("i");
    
        for (var i=0;i<anchors.length;i++) {
            var anchor=anchors[i];
            
            if (anchor.className=='replace') replaceAnchor(anchor);
            if (anchor.className=='popup') dynamicPopup(anchor);

            //if (anchor.className=='selection') useSelection(anchor);
        }

        debug("processing forms");
        var forms=target.getElementsByTagName("form");
        for (var i=0;i<forms.length;i++) {
            var form=forms[i];
            if (form.className=='automatic') automaticForm(form,true,false);
            if (form.className=='refresh') automaticForm(form,false,true);
            if (form.className=='annotation') annotationForm(form);
        }
        debug("processing spans");
        var spans=target.getElementsByTagName("span");
        for (var i=0;i<spans.length;i++) {
            var span=spans[i];
            if (span.className=='popupClick') clickPopup(span);
        }

        debug("processing areas");
        var areas=target.getElementsByTagName("area");
        for (var i=0;i<areas.length;i++) {
            var area=areas[i];
            if (area.className=='popup') dynamicAreaPopup(area);
        }

        debug("processing finished");
    } catch(e) {

        debug("Error: "+e);
        if (e.stack) {
            var lines=e.stack.split("@");
            for (var i=0;i<lines.length;i++)
            debug(lines[i]);
        }

    }
}

function activateTab(contents,tab) {
    /*debug("Contents:"+eltString(contents));
    debug("Tab:"+eltString(tab));*/
    var n=tab.parentNode.firstChild;
    while (n) {n.className='tab_inactive';n=n.nextSibling;}
    tab.className='tab_active';
    n=contents.parentNode.firstChild;
    while (n) {n.style.display='none';n=n.nextSibling;}
    contents.style.display='block';
    return false;
}

function tabClick(tabContents,tab) {
    return function() {return activateTab(tabContents,tab);};
}

function autoTab(elt) {
    debug("autotab");
    elt.className='tabowner';
    var parent=elt.parentNode;
    var after=elt.nextSibling;
    //debug("remove");
    parent.removeChild(elt);
    //debug("removed");
    var child=elt.firstChild;
    var tabDiv=document.createElement("div");
    tabDiv.className='tablist';

    var tabContainer=document.createElement("div");

    var tabContents;
    var firstTab;

    var i=0;

    while (child) {
        var next=child.nextSibling;

        if (child.nodeName.toUpperCase()=='H2') {
            //debug("tab start");
            var tab=document.createElement("a");
            if (!firstTab || child.className=='selected') firstTab=tab;
            var anchors = child.getElementsByTagName("a");
            var name=anchors.length>0?anchors[0].name:"tab"+i;
            i=i+1;
            if ('#'+name==window.location.hash) firstTab=tab;
            tab.href="#"+name;
            tab.appendChild(child.firstChild);
            tabContents=document.createElement("div");
            tabContents.style.display='none';
            tabDiv.appendChild(tab);
            tabContainer.appendChild(tabContents);
            tab.onclick=tabClick(tabContents,tab);
            child.parentNode.removeChild(child);
            //debug("tab end");
        } else {
            //debug("tcr start");
            if (tabContents!=null) tabContents.appendChild(child);
            //debug("tcr end");
        }
        child=next;
    }

    //debug("itabs");

    parent.insertBefore(tabDiv,after);
    //debug("icontents");
    parent.insertBefore(tabContainer,after);

    //debug("click");
    firstTab.onclick();
    debug("autotab done");
}

/*

function superSplit(s) {
    s=s.replace(/^[, \t\n]+|[, \t\n]+$/g, '');
    if (s=='') return new Array();
    return s.split(/[, \t\n]+/);
}

function getValues(values,inps) {
    for (var i=0;i<inps.length;i++) {
        var v;
        if ((inps[i].type == 'radio' || inps[i].type == 'checkbox') && !inps[i].checked)
            v=new Array();
        else
            v = superSplit(inps[i].value);
        var n = inps[i].name;
        var a = values[n];
        if (a) a = a.concat(v); else a = v;
        values[n] = a;
    }
}

function previewAnnotation(elt,start) {
    var form=findParentByName(elt,"FORM");
    form.style.display='block';
    eid('simpleform').style.display='none';
    var values=new Object();
    getValues(values,form.getElementsByTagName("input"));
    getValues(values,form.getElementsByTagName("textarea"));
    for (var x in values) {
        var summary = document.getElementById("summary-"+x);
        if (!summary) continue;
        var ct=values[x].length;
        var data=ct;
        if (ct==1) data=values[x][0];
        if (ct==0) data="Any";
        summary.innerHTML=data;
    }
    if (!start) start=0;
    debug("previewing from "+start+" "+form+" "+form["count"].value+" "+form.action);
    form["start"].value = start;
    formSubmit(null,form,"annotation-frame","annotation-loading","format","sampleEmbed");
    clearRows("annotation-before","annotation-after");
    clearRows("stats-before","stats-after");
    return false;
}

function displayAnnotation(elt) {
    if (!refreshRows(elt,"annotation-before","annotation-after","annotation-loading","results")) return;
    loadStats(elt);
}

function loadStats(elt) {
    var form=findParentByName(elt,"FORM");
    formSubmit(null,form,"stats-frame","stats-loading","format","statsEmbed");

}

function downloadAnnotation(elt,format,event) {
    var form=findParentByName(elt,"FORM");
    formSubmit(null,form,"download-frame","download-loading","format",format);
    //event.preventDefault();
    return false;
}

function openAnnotation(elt,event) {
    var form=findParentByName(elt,"FORM");
    formSubmit(null,form,"_blank",null,"format","form");
    //event.preventDefault();
    return false;
}







var currentMenu;
var currentIndex;
var menuItems;

function setMenu(e) {
    if (e==null) return;
    debug("Menu: "+eltString(e));
    currentMenu=e;
    currentIndex=-1;
    menuItems=findChildrenByNameClass(e,'tr','menuitem');
    for (var i=0;i<menuItems.length;i++)
        menuItems[i].style.backgroundColor=null;
}

function setMenuIndex(i) {
    if (menuItems[currentIndex]) menuItems[currentIndex].style.backgroundColor=null;
    if (i>=menuItems.length) i=menuItems.length-1;
    if (i<-1) i=-1;
    currentIndex=i;
    if (menuItems[currentIndex]) menuItems[currentIndex].style.backgroundColor='#ccf';
}

function refreshDivName(iframe,parentClass,targetClass,loadingClass) {
    debug("refreshName "+eltString(iframe)+", "+eltString(targetClass)+", "+eltString(loadingClass));
    var parent=findParentByClass(iframe,parentClass);
    var target =  findChildByNameClass(parent,"div",targetClass);
    var loading = findChildByNameClass(parent,"div",loadingClass);
    debug("refresh "+eltString(parent)+", "+eltString(target)+", "+eltString(loading));
    refreshDivElt(iframe,target,loading);
}

function refreshDiv(elt,targetID,loadingID) {
    refreshDivElt(elt,eid(targetID),eid(loadingID));
}

function refreshDivElt(elt,target,loading) {
    var results = elt.contentWindow.document.getElementById("results");
    if (!results) return;
    target.innerHTML=results.innerHTML;
    setMenu(findChildByNameClass(target,'div','menu'));
    if (loading!=null) loading.style.display='none';
    
}

function clearRows(beforeID,afterID) {
    var before=eid(beforeID);
    var after=eid(afterID);

    while (before.nextSibling && before.nextSibling!=after)
        before.parentNode.removeChild(before.nextSibling);
}

function refreshRows(iframe,beforeID,afterID,loadingID,resultsID) {

    if (beforeLoad) return;

    debug("Refresh rows "+iframe.id+" "+beforeID+" "+afterID+" "+loadingID+" "+resultsID);
    var before=eid(beforeID);
    var after=eid(afterID);


    var dump = document.getElementById('dump');

    var ifcwd=iframe.contentWindow.document;
    debug("CWD "+ifcwd);
    if (ifcwd==null) return false;
    var results = ifcwd.body;
    debug("Results "+results+" Loading: "+eid(loadingID));
    if (!results) {
        return false;
    }
    dump.innerHTML=results.innerHTML;
    var table=document.getElementById(resultsID);

    var current=table.getElementsByTagName("tr")[0];
    debug("Insert betweeen ("+before+")-("+after+")");
    while (current) {
        var next=current.nextSibling;
        //before.parentNode.insertBefore(document.importNode(r,true),after);
        before.parentNode.insertBefore(current,after);
        current=next;
    }
    eid(loadingID).style.display='none';
    return true;
}


var VK_UP=38;
var VK_DOWN=40;
var VK_RETURN=13;


function menuKeypress(event) {
    if (!event) event=window.event;
    var keyCode=event.keyCode;

    if (keyCode==VK_UP) {
        return false;
    }
    if (keyCode==VK_DOWN) {
        return false;
    }
    if (keyCode==VK_RETURN) {
        if (currentIndex>=0) return false;
    }
    return true;
}


function menuKeydown(input,event) {
    if (!event) event=window.event;
    var keyCode=event.keyCode;

    if (keyCode==VK_UP) {
        setMenuIndex(currentIndex-1);

        return false;
    }
    if (keyCode==VK_DOWN) {
        setMenuIndex(currentIndex+1);
        return false;
    }
    if (keyCode==VK_RETURN) {
        if (currentIndex>=0) {
            menuselect(menuItems[currentIndex],input);
            return false;
        }
    }

    return true;
}

function menuselect(e,input) {
    var inputElts=e.getElementsByTagName("input");
            var anchorElts=e.getElementsByTagName("a");
            if (inputElts.length>0) input.value=inputElts[0].value;
            else if (anchorElts.length>0) window.location=anchorElts[0].href;
}
*/
function expandTextarea(e,evt) {
    e.rows=e.value.replace(/\n*$/g,"").replace(/[^\n]/g,"").length+2;
    //e.parentNode.parentNode.onclick(evt);
}



/*

function narrower(e,evt) {
    var cell=findParentByName(e,"TD");
    var rowcells=cell.parentNode.getElementsByTagName("TD");
    var i=0;
    while (i<rowcells.length && rowcells[i]!=cell) i++;
    var cols=findParentByName(e,"TABLE").getElementsByTagName("col");
    var w=cols[i].style.width.match(/[0-9]* /)/*
;
    var width=w[1]*1;
    cols[i].style.width=(width-1)+w[2];
}


function wider(e,evt) {
    var cell=findParentByName(e,"TD");
    var rowcells=cell.parentNode.getElementsByTagName("TD");
    var i=0;
    while (i<rowcells.length && rowcells[i]!=cell) i++;
    var table = findParentByName(e,"TABLE");
    var cols=table.getElementsByTagName("col");
    var w=cols[i].style.width.match(/([0-9]*)(.*)/);
    var width=w[1]*1;
    cols[i].style.width=(width+1)+w[2];
    table.style.tableLayout="auto";
    table.style.tableLayout="fixed";
    debug("change "+i+" "+w[1]+" "+cols[i].style.width);

}*/
