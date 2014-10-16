JSLIB.depend('remote',['dom.js','popup.js','menu.js'],
        function remote(dom,popup,menu) {

    var logger=this.logger;
    logger.log('AJAX');

    var remote=this;        

    var xmlRequest=this.xmlRequest=function(xhr,method,url,content,success,failure,completed) {
        if (url.indexOf('?')<0) url+='?';
        url+="&request="+new Date().getTime();
        logger.url(url);
        if (content) logger.extra('Request',content);
        xhr.abort();
        xhr.open(method,url);
        if (method=='POST') xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        xhr.onreadystatechange = logger.wrap(function () {
            
            if (xhr.readyState != 4) return;
	        logger.extra('Response: '+url,xhr.responseText);
            var succeeded=xhr.status == 200;                    

            try {
                if (succeeded) {
                    if (success) success(xhr);
                } else {
                    if (failure) failure(xhr);
                }
            } finally {
                if (completed) completed();
            }
        });

        xhr.send(content);
    };
    


    var Queue=this.Queue=function (name,timeout) {
        var queue=this;
        var jobs=[];
        var inprogress=false;
        var deadJob;

        var idleListeners=[];

        var currentJob;

        function idle() {
            inprogress=false;            
            for (var i=0;i<idleListeners.length;i++) idleListeners[i]();            
        }

        var next=this.next=function() {
            //logger.log('Next:'+name+' '+jobs.length);
            if (deadJob) {                
                window.clearTimeout(deadJob);
                deadJob=null;
            }
            if (jobs.length==0) idle();
            else {
                inprogress=true;
                currentJob=jobs.shift()();
                if (timeout) {
                    deadJob=window.setTimeout(next,timeout);
                }
            }
            return inprogress;
        };
        this.add=function(job) {
            jobs.push(job);
            if (!inprogress) next();
            function cancel() {
                jobs.remove(job);
            }
            function abort() {
                if (currentJob==job && currentJob.abort) currentJob.abort();
                next();
            }
            return {cancel:cancel,abort:abort};
        };

        this.empty = function() {
            jobs=[];
            if (currentJob && currentJob.abort) currentJob.abort();

        };

        this.onIdle=function(callback) {
            idleListeners.push(callback);
        };
        this.isIdle=function() {
            return !inprogress;
        };
    };


    function createXHR() {
        if (window.XMLHttpRequest) return new XMLHttpRequest();
        if (window.ActiveXObject) return new ActiveXObject('MSXML2.XMLHTTP.3.0');
        throw 'No XMLHttpRequest';
    }

    var encodeParameters=this.encodeParameters=function encodeParameters(params) {
        var encode='';
        if (params) {
            for (var n in params) {
                if (!params[n])
                    encode+="&"+n;
                else if (params[n].join)
                    encode+="&"+n+"="+encodeURIComponent(params[n].join(','));
                else
                    encode+="&"+n+"="+encodeURIComponent(params[n]);
            }
        }
        return encode;
    };

    this.getFormParameters=function(form) {
        var parameters={};
        var elts=form.elements;
        for (var i=0;i<elts.length;i++) {
			var e=elts[i];
            if (e.type=='submit') continue;
            if ((e.type=='checkbox' || e.type=='radio') && !e.checked) continue;
            if (e.type=='text' && e.value=='') continue;
			var value=e.value;
			if (e.selectedIndex) {
				value=e.options[e.selectedIndex].value;
			}
            if (e.name && e.nodeName!='BUTTON') {
                if (!parameters[e.name]) parameters[e.name]=[];
                parameters[e.name].push(value);
            }

        }
        return parameters;
    };


    this.XHRStatusWrapper=function(xhrQueue,text) {
        var loadingImage=dom.span(dom.img(remote.dirname(remote.path)+'loading.gif','loading'));
        var error=dom.styleSpan({display:'none'},'FAILED');
        var info=this.content=dom.styleSpan({display:'none'},text,loadingImage,error);
        
        this.request=function(method,url,content,parameters,success,failure) {
            loadingImage.style.display='inline';
            info.style.display='inline';
            error.style.display='none';
            function succeeded(xhr) {
                info.style.display='none';
                error.style.display='none';
                if (success) success(xhr);
            }
            function failed() {
                info.style.display='inline';
                loadingImage.style.display='none';
                error.style.display='inline';
                if (failure) failure();
            }
            xhrQueue.request(method,url,content,parameters,succeeded,failed);
        };
    };


    this.XHRQueue=function(base,defaultParameters) {
        

        var queue=this.queue=new Queue('XHR:'+base+' '+encodeParameters(defaultParameters));
        var xhr=createXHR();

        this.empty=function(){queue.empty();};

        var open=this.open=function(method,url,parameters,content,success,failure) {            

            url=(base?base:'')+(url?url:'');

            var encode=encodeParameters(defaultParameters)+encodeParameters(parameters);

            if (!method) method=content || encode.length>1024?'POST':'GET';

            if (method=='POST' && content==null) content=encode;
            else url+='?'+encode.substring(1);


            logger.log('Queue',url);
            return queue.add(function(){
                function next() {
                    queue.next();
                }
                function completed() {
                    logger.log('Finished',url);                    
                    next();
                }

                
                xmlRequest(xhr,method,url,content,success,failure,completed);
                function abort() {
                    xhr.abort();
                }
                return {abort:abort};
            });
        };

        this.request=function(method,url,content,parameters,success,failure) {
            return open(method,url,parameters,content,success,failure);
        };
        
        this.get=function(url,parameters,success,failure) {
            return open("GET",url,parameters,null,success,failure);
        };
        this.post=function(url,parameters,content,success,failure) {
            return open("POST",url,parameters,content,success,failure);
        };


    };

    this.XHRParameters=function(queue,base,defaultParameters) {
        this.request=function(method,url,content,parameters,success,failure) {
            var p={};
            for (var x in defaultParameters) p[x]=defaultParameters[x];
            for (var y in parameters) p[y]=parameters[y];
            return queue.request(method,base+url,content,p,success,failure);
        };
    };


    var jsonCallbacks=window.jsonCallbacks=[];

    var  jsonRequest=this.jsonRequest=function(url, callback) {

        var number=jsonCallbacks.length;
        jsonCallbacks[number]=logger.wrap(callback);
        var script = document.createElement('script');
        var src=script.src = url+"&callback=window.jsonCallbacks["+number+"]";
        //var src=script.src = url+"&callback=alert";
        document.body.appendChild(script);
        logger.url(src);
    };

    this.JSONQueue=function(base,defaultParameters) {
        var queue=this.queue=new Queue('JSON:'+base+' '+encodeParameters(defaultParameters),5000);

        base=''||base;

        this.request = function(url,parameters,callback) {
            return queue.add(function() {
                url=base+url;
                var encode=encodeParameters(defaultParameters)+encodeParameters(parameters);
                url+=encode.replace('&','?');
                jsonRequest(url,function(results){queue.next();callback(results);});
            });
        };
    };



    this.JSONParameters=function(queue,base,defaultParameters) {
        this.request = function(url,parameters,callback) {
            var p={};
            for (var x in defaultParameters) p[x]=defaultParameters[x];
            for (var y in parameters) p[y]=parameters[y];
            return queue.request(base+url,p,callback);
        };
    };

    this.JSONCache=function(queue,idParameter) {

        var archive=new Object();
        this.get=function(id,callback) {            
            var item=archive[id];
            if (!item) {
                archive[id]=item=new remote.After();
                var p={};
                p[idParameter]=id;
                queue.request('',p,function(result){
                    item.done(result);
                });
            }
            item.register(callback);
            
        };
    };

    this.getHTML=function(xhr) {
        var root = dom.div();
        root.innerHTML = xhr.responseText;
        return root;
    }


    this.enhanceDynamicPopup=function(loadingImage,regexp,replacement) {
        var r=new this.XHRQueue();

        //r.image('image/loading.gif');
        return function(node) {
            logger.log('Dynamic',node,node.nodeName);
            if (node.nodeName.toUpperCase()=='A') {
                var content=dom.div();
                if (node.parentNode.className=='nopopup') {
                    content.style.display='none';
                    node.parentNode.appendChild(content);
                    node=node.parentNode;
                } else {
                    var p=new popup.Popup(content);
                    p.attachClick(node);
                }
                var loaded=false;
                dom.onclick(node,function(){
                    if (!loaded) {
                        r.get(node.href.replace(regexp,replacement),{},
                              function(xhr){loaded=true;content.innerHTML=xhr.responseText;},
                              null,'Loading');
                    }
                });
            }
        };
    };
    
    this.enhanceSuggest=function(loadingImage,parameters) {
        var ct=0;
        return function(node) {

                var header=dom.div('Title');
                var p=new menu.MenuTablePopup(header);
                var mect=ct++;
                logger.log('MTP',mect);
                var input=node.elements[0];

                function makeItem(text) {
                    return new menu.MenuItem(dom.tr(text),function(){
                        input.value=text;
                    });
                }
            
                function loaded(xhr){
                    logger.log('XHR '+xhr.responseXML);
                    var options=xhr.responseXML.getElementsByTagName('div');
                    logger.log('Results '+options.length);
                    for (var i=0;i<options.length;i++) {
                        var text=options[i].firstChild.nodeValue;
                        logger.log('tr'+text);
                        var item=makeItem(text);
                        logger.log('tr2'+item.innerHTML);
                        p.add(item);
                        logger.log('row');
                    }
                    logger.log('fillpopup',p.content);
                    logger.log('popup',mect,p.content.innerHTML);
                }
            
                function load() {
                    p.empty();
                    r.get(node.action,parameters,loaded,null,'Loading');
                }
            
                p.attachClick(input,load,null);
                var r=new XHRQueue();
                //r.image('image/loading.gif');
                        
                dom.onchange(input,load);
        };
    };
});