JSLIB.depend('audioplayer',['dom.js'], function(dom, progressive) {

    var audioplayer=this;
    var logger = this.logger;

    var resourceBase=this.dirname(this.path);

    logger.log('Audio Player - HTML5 & Flash');


    function Listeners() {
        var listeners={};
        
        function get(name) {
            return (listeners[name]=listeners[name]||[]);
        }
        this.completed=function (name,event) {
            dispatch(name,event);
            get(name).push=function(listener) {
                listener(event);
            };
        };
        this.add=function add(name,listener) {
            get(name).push(listener);
        };
        var dispatch=this.dispatch=function (name,event) {
            var all=get(name);            
            for (var i=0;i<all.length;i++) {
                all[i](event);
            }
        };
    }

    function callAll(listeners) {
        for (var i=0;i<listeners.length;i++) listeners[i]();
    }

    this.GUI=function(player,width,height) {
        height=height||16;
        var progressWidth=width-height;
        var centre=height/2;
        var loadedBar=dom.styleSpan({position:'absolute',display:'block',top:(centre-1)+'px',left:'0',height:'2px',fontSize:'2px',backgroundColor:'#77f'});
        var progressBar=dom.styleSpan({position:'absolute',display:'block',top:'0px',left:'0',height:height+'px',fontSize:'2px',backgroundColor:'#77f'});

        var playButton=dom.img(resourceBase+"images/control_play_blue.png");
        playButton.width=height;
        playButton.height=height;
        var pauseButton=dom.img(resourceBase+"images/control_pause_blue.png");
        pauseButton.width=height;
        pauseButton.height=height;
        var loadingImage=dom.img(resourceBase+"images/ajax-loader.gif");
        loadingImage.width=height;
        loadingImage.height=height;

        function readyToPlay() {
            playButton.style.display='inline';
            pauseButton.style.display='none';
            loadingImage.style.display='none';
        }

        function playing() {
            playButton.style.display='none';
            pauseButton.style.display='inline';
            loadingImage.style.display='none';
        }

        playButton.style.display='none';
        pauseButton.style.display='none';
        loadingImage.style.display='inline';



        function seekClick(event) {
            
            var p=event.offsetX;
            var time=player.getEstimatedLength()*p/progressWidth;
            player.seek(time);
            
        }

        var title=dom.styleSpan({overflow:'hidden',position:'absolute',fontSize:height+'px',left:0,top:0});

        var timeline=dom.styleSpan({display:'inline-block',position:'relative',width:progressWidth+'px',height:height+'px',backgroundColor:'#ccf'},loadedBar,progressBar,title);

        dom.onclick(timeline,seekClick);

        this.content=dom.span(dom.span(loadingImage,playButton,pauseButton),timeline,player.content);

        function onUpdate() {
            if (player.isPlaying()) playing(); else readyToPlay();
            dom.replaceContent(title,player.getSongName());

            loadedBar.style.width=((player.getFractionLoaded()*progressWidth)>>0)+'px';
            progressBar.style.width=((player.getFractionPlayed()*progressWidth)>>0)+'px';

        }

        function onEnded() {
            readyToPlay();
        }

        function onReady() {
            readyToPlay();
        }




        dom.onclick(playButton,player.play);
        dom.onclick(pauseButton,player.pause);

        player.addEventListener('update',onUpdate);
        player.addEventListener('ended',onEnded);
        player.addEventListener('ready',onReady);

    }

    this.FlashPlayer=function (url,autoPlay) {
        var iframe=dom.el('iframe');
        iframe.src=resourceBase+'audio/flashaudioframe.html';
        iframe.style.width='1px';
        iframe.style.height='1px';
        iframe.style.border='none';

        var playerObject;


        var position;
        var length;
        var bytesLoaded;
        var bytesTotal;
        var estimatedLength=0;
        var fractionLoaded;
        var fractionPlayed;
        var isPlaying;
        var isEnded;
        var songName;

        var listeners=new Listeners();
        this.addEventListener=listeners.add;

        this.getFractionLoaded=function() {
            return fractionLoaded;
        };

        this.getFractionPlayed=function() {
            return fractionPlayed;
        };

        this.getPosition=function() {
            return position;
        };

        this.getEstimatedLength=function() {
            return estimatedLength;
        };

        this.getLength=function() {
            return length;
        };

        this.getBytesLoaded=function() {
            return bytesLoaded;
        };

        this.getBytesTotal=function() {
            return bytesTotal;
        };

        this.isPlaying=function() {
            return isPlaying;
        };

        this.isEnded=function() {
            return isEnded;
        };

        this.getSongName=function() {
            return '';
            //return songName;
        };


        function onUpdate(_isPlaying,_position,_length,_bytesLoaded,_bytesTotal) {
            logger.run(function() {
            isPlaying=_isPlaying;
            position=_position;
            length=_length;
            bytesLoaded=_bytesLoaded;
            bytesTotal=_bytesTotal;
            fractionLoaded=bytesLoaded/bytesTotal;
            estimatedLength=length/bytesLoaded*bytesTotal;
            fractionPlayed=position/estimatedLength;
            songName=playerObject.getInfo('songName');
            logger.log('Update '+bytesLoaded+' '+bytesTotal+' '+fractionLoaded+' '+fractionPlayed+' '+isPlaying+' '+songName);
            listeners.dispatch('update');
            });
        }

        function onEnded() {
            isEnded=true;
            listeners.dispatch('ended');
        }

        function onReady() {
            listeners.dispatch('ready');
        }



        




        function attach() {

            if (playerObject) return true;
            if (iframe.readyState && iframe.readyState!='complete') return false;

            var contentWindow=iframe.contentWindow;

            if (!contentWindow || !contentWindow.document) return false;

            var flashObject=contentWindow.document.getElementById('flash');

            if (!flashObject || !flashObject.getInfo) return false;

            logger.log('Attaching:'+flashObject.getInfo('version'));

            playerObject=flashObject;
            contentWindow.onFlashUpdate=onUpdate;
            contentWindow.onFlashEnded=onEnded;
            playerObject.setInterval(250);
            playerObject.onUpdate('onFlashUpdate');
            playerObject.onEnded('onFlashEnded');
            onReady();
            logger.log('start',playerObject+' '+url);
            if (url) load(url);
            if (autoPlay) play();
            logger.log('Attached');
            return true;

        }
        this.isReady=function() {
            return attach();
        }
        var load=this.load=function load(audio) {
            attach();
            isEnded=false;
            playerObject.loadAudio(audio);
        };
        var play=this.play=function play() {
            logger.log('play',playerObject);
            attach();
            logger.log('playAudio');
            isEnded=false;
            playerObject.playAudio();
            logger.log('playAudio-OK');            
        };
        var pause=this.pause=function pause() {
            attach();
            playerObject.pauseAudio();            
        };

        function delaystartup() {
            dom.schedule(startup,1000);
        }

        function startup() {
            if (!attach()) delaystartup();
            else {
                
            }
        }

        delaystartup();

        this.content=iframe;

        

    };


    function getFlashVersion(){
        // ie
        try {
            try {
                // avoid fp6 minor version lookup issues
                // see: http://blog.deconcept.com/2006/01/11/getvariable-setvariable-crash-internet-explorer-flash-6/
                var axo = new ActiveXObject('ShockwaveFlash.ShockwaveFlash.6');
                try { axo.AllowScriptAccess = 'always'; }
                catch(e) { return '6,0,0'; }
            } catch(e) {}
            return new ActiveXObject('ShockwaveFlash.ShockwaveFlash').GetVariable('$version').replace(/\D+/g, ',').match(/^,?(.+),?$/)[1];
        // other browsers
        } catch(e) {
            try {
                if(navigator.mimeTypes["application/x-shockwave-flash"].enabledPlugin){
                return (navigator.plugins["Shockwave Flash 2.0"] || navigator.plugins["Shockwave Flash"]).description.replace(/\D+/g, ",").match(/^,?(.+),?$/)[1];
                }
            } catch(e) {}
        }
        return '0,0,0';
    }

    function getFlashError() {
        var version=getFlashVersion();
        var major=version.split(',')[0]>>0;

        if (major==0) return 'Flash required but not present.';
        if (major<9) return 'Flash version '+major+' too old - version 9 or above required';
        return false;
    }



    this.HTML5Player=function (url,autoPlay) {

        var listeners=new Listeners();
        this.addEventListener=listeners.add;


        var audioElement=dom.el('audio');
        audioElement.style.width='1px';
        audioElement.style.height='1px';


        this.content=audioElement;
        this.play=function() {
            audioElement.play();
        };

        
        var bytesTotal;
        var bytesLoaded;

        function progress(event) {
            bytesLoaded=event.loaded;
            bytesTotal=event.total;
            logger.log('Progress',bytesLoaded,bytesTotal);
            listeners.dispatch('update');

        }

        function timeupdate() {
            logger.log('Update',audioElement.currentTime,audioElement.duration);
            listeners.dispatch('update');
        }

        function ended() {
            isEnded=true;
            listeners.dispatch('ended');
            listeners.dispatch('update');
        }

        audioElement.addEventListener('progress',progress);

        audioElement.addEventListener('timeupdate',timeupdate);

        audioElement.addEventListener('ended',ended);

        this.pause=function() {
            audioElement.pause();
        };
        this.seek=function(position) {
            audioElement.currentTime=position;
        };
        this.play=function() {            
            audioElement.play();
        };
        this.load=function(url) {            
            audioElement.src=url;
            audioElement.load();
        };
        this.getFractionLoaded=function() {
            return bytesTotal?bytesLoaded/bytesTotal:1;
        };

        this.getEstimatedLength=function() {
            if (!bytesLoaded) return audioElement.duration;
            return audioElement.duration*bytesLoaded/bytesTotal;
        };



        this.getFractionPlayed=function() {
            return audioElement.currentTime/this.getEstimatedLength();
        };

        this.isPlaying=function() {
            return !audioElement.paused;
        };


        this.isEnded=function() {
            return audioElement.ended;
        };

        this.getSongName=function() {
            return '';
        };

        listeners.completed('ready');
        if (url) this.load(url);
        if (autoPlay) this.play();


    }

    this.AudioPlayer=function(urlBase,autoPlay) {
        if (!getFlashError()) audioplayer.FlashPlayer.apply(this,[urlBase+'.mp3',autoPlay]);
        else audioplayer.HTML5Player.apply(this,[urlBase+'.ogg',autoPlay]);
    };

});