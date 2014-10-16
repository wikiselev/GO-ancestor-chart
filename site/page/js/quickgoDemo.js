JSLIB.depend('quickgoDemo', [
	'jslib/doc.js','jslib/dom.js', 'jslib/parameters.js','jslib/progressive.js', 'jslib/tabs.js', 'jslib/lightbox.js', 'jslib/remote.js','jslib/audioplayer.js'
	], function(doc,dom, parameters,progressive, tabs, lightbox, remote,audioPlayer) {

    var logger = this.logger;
    logger.log('QuickGO demonstration script support');

    var startTime=new Date().getTime();

    var audioEnabled=parameters.get('audio')!=null;
    var autoPlay=parameters.get('auto')!=null;

    

    var infoBox;

    function getTargetDom() {
        var targetDocument=window.parent.frames['target'].document;
        return targetDocument==null?null:new doc.DOM(targetDocument);        
    }

    function openURL(url,loaded) {
        var frame=window.parent.frames['target'];
        //if (frame.location!=url) frame.location=url;
        //logger.log('Loading',url,loaded);
        
        frame.location=url;
        
    }


    var player;
    if (audioEnabled) player=new audioPlayer.AudioPlayer();


    function Anchor(anchor) {
        
        if (!anchor) return;
        var hrefParts=anchor.href.split("#")[1].split('.');
        var tagName=hrefParts[0];
        var className=hrefParts[1];
        
        
        this.find=function(dom) {
            if (!dom) return null;
            if (!dom.document.body) return null;
            return dom.find(dom.document.body,tagName,className);
        };

        this.toString=function() {
            return tagName+'.'+className;
        }
    }


    
    var hiliter=new function Hiliter() {
        var previousHilite;

        var previousElement;
        var previousPosition;

        this.hiliteElement=function (dom,element) {



            var reference=element.offsetParent;
            
            var position=dom.where(element);
            if (element==previousElement && previousPosition.left==position.left && previousPosition.top==position.top) return;
            previousElement=element;
            previousPosition=position;

            if (previousHilite) previousHilite.parentNode.removeChild(previousHilite);

            //var marker=dom.styleSpan({position:'relative'});
            //dom.insertBefore(element.parentNode,marker,element);
            //dom.add(dom.document.body,marker);
            //element.scrollIntoView();

            logger.log('hilite',reference);

            var marker=dom.styleSpan({position:'relative',border:'none',margin:0,padding:0});
            dom.insertBefore(reference,marker,reference.firstChild);
            

            var positionMarker=dom.where(marker);
            var border=3;
            var padding=2;
            var width=position.width;
            var height=position.height;
            var left=position.left-positionMarker.left;
            var top=position.top-positionMarker.top;
            logger.log('Hilite',left,top,width,height);
            dom.add(marker,
                dom.styleDiv({position:'absolute',left:(left-border-padding)+'px',top:(top+height+padding+1)+'px',width:(width+(border+padding)*2)+'px',height:border+'px',zIndex:5000,backgroundColor:'#f00'}),
                dom.styleDiv({position:'absolute',left:(left-border-padding)+'px',top:(top-border-padding-1)+'px',width:(width+(border+padding)*2)+'px',height:border+'px',zIndex:5000,backgroundColor:'#f00'}),
                dom.styleDiv({position:'absolute',left:(left+width+padding+1)+'px',top:(top+-border-padding)+'px',width:border+'px',height:(height+(border+padding)*2)+'px',zIndex:5000,backgroundColor:'#f00'}),
                dom.styleDiv({position:'absolute',left:(left-border-padding-1)+'px',top:(top+-border-padding)+'px',width:border+'px',height:(height+(border+padding)*2)+'px',zIndex:5000,backgroundColor:'#f00'})
            );


            previousHilite=marker;
        }
    };

    function notify(message) {
        var time=new Date().getTime();
        dom.replaceContent(infoBox,Math.round((time-startTime)/1000)+'s '+message);
    }
    

    var currentWait;
    function waitfor(callback) {
        if (currentWait) currentWait.cancel();
        currentWait=dom.schedule(function(){currentWait=null;callback();},500);
    }

    var nextClick;

    function Section(number,source,next) {

        var section=this;
        var sectionDiv=dom.styleDiv({backgroundColor:'#000',color:'#fff'});
        
        dom.adoptChildren(source,sectionDiv);
        var audioLink=dom.find(sectionDiv,'a','audio');
        var scrollTo=new Anchor(dom.find(sectionDiv,'a','scroll'));
        var showLink=new Anchor(dom.find(sectionDiv,'a','show'));
        

        var userentry=dom.find(sectionDiv,'span','userentry');
        var value;
        if (userentry) value=dom.getInnerText(userentry);

        var amshown=false;        


        this.open=function() {
            
            //logger.log('start wait');
            if (showLink.find) {
                waitfor(wait);
            } else {
                waitfor(null);
                show();
            }
        }
        
//        function loaded() {
//
//            var targetDom=getTargetDom();
//            var anchorElement=findAnchor(targetDom,anchor);
//            if (anchorElement) show(targetDom,anchorElement);
//            else failed();
//        }
//
        var wait=this.wait=function wait() {
            //logger.log('wait');
            var targetDom=getTargetDom();
            //logger.log('w2');
            var anchorElement=showLink.find(targetDom);
            //logger.log('waiting',anchor,anchorElement);
            waitfor(wait);
            if (anchorElement) show(targetDom,anchorElement);
            else {
                notify(number+') Waiting for target');
            }
            
        }
        
        function failed() {
            
        }

        var playing=false;
        
        function show(targetDom,element) {
            if (!amshown) {
                section.show();
                if (scrollTo.find) {
                    scrollTo.find(targetDom).scrollIntoView()
                }
                
                
                amshown=true;
            }

            if (player && audioLink && !playing) {
                //playAudio(audioLink.href);

                player.load(audioLink.href);
                player.play();
                playing=true;
            }

            if (element) {
                hiliter.hiliteElement(targetDom,element);
                if (nextClick) nextClick.remove();
                nextClick=targetDom.onclick(element,function(){notify(number+') Finished ');next();});
            }

            if (autoPlay) {
                if (playing && !player.isEnded()) {
                    notify(number+') Waiting for audio to finish');
                } else {
                    targetDom.sendClick(element);
                    if (value) targetDom.setValue(element,value);
                    //player.stop();
                    notify(number+') Audio finished, triggering next action');
                }
            } else {
                notify(number+') Waiting for click');
            }

        }

        this.content=sectionDiv;
    }

	function enhanceSections(list) {

        var sections=[];
        var currentIndex=-1;
        var sectionContainer=dom.div();

        function open(index) {
            //logger.log('open',index);
            if (sections[index]) sections[index].open();
        }

        function makeSection(elt) {
            var index=sections.length;
            var section=new Section(index+1,elt,function(){open(index+1);});
            section.show=function() {
                if (sections[currentIndex]) sections[currentIndex].content.style.display='none';
                section.content.style.display='block';
                currentIndex=index;
            }
            section.content.style.display='none';
            dom.add(sectionContainer,section);
            sections.push(section);
            return section;
        }

        
        
        var sectionElements=dom.findAll(list,'li');
        //logger.log('Sections',sectionElements.length);
        for (var i=0;i<sectionElements.length;i++) {            
            makeSection(sectionElements[i]);
        }

        dom.replaceElement(list,sectionContainer);
        open(0);
	}

    function startup() {
        logger.log('startup',audioEnabled);
        if (audioEnabled) dom.add(dom.find(document.body,'div','player'),new audioPlayer.GUI(player,100,16));
        infoBox=dom.find(document.body,'div','infobox');
        progressive.enhance(document.body,'ul','sections',enhanceSections);
    }

    this.afterAllLoad(function(){
        openURL("index.html?u="+Math.random(),startup);
        startup();
        
    });

});