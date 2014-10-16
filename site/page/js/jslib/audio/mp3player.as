/*

Compile actionscript with the flex sdk from:

http://opensource.adobe.com/wiki/display/flexsdk/Flex+SDK

flex/installation/path/bin/mxmlc mp3player.as

David Binns, August 2010

*/

package {
    import flash.display.Sprite;
    import flash.media.Sound;
    import flash.net.URLRequest;
    import flash.media.ID3Info;
    import flash.text.TextField;
    import flash.text.TextFieldAutoSize;
    import flash.events.Event;
    import flash.media.SoundChannel;
    import flash.external.ExternalInterface;
    import flash.utils.Timer;
    import flash.events.TimerEvent;
    import flash.display.Loader;
    import flash.display.LoaderInfo;

    public class mp3player extends Sprite {
        private var snd:Sound;
        private var channel:SoundChannel;
        private var myTextField:TextField = new TextField();
        private var position:Number=0;
        private var myTimer:Timer;
        private var id3:ID3Info;
        private var version:String="Flash MP3 Player v0.81";
        

        private var onUpdateCallback:String;
        private var onEndedCallback:String;

        public function onUpdate(callback:String):void {
            onUpdateCallback=callback;
        }

        public function onEnded(callback:String):void {
            onEndedCallback=callback;
            
        }

        public function seek(p:Number):void {
            position=p;
            if (channel) play();
        }
        public function play():void {
            myTextField.appendText("stop?\n");
            if (channel) channel.stop();
            myTextField.appendText("Play\n");
            channel = snd.play(position);            
            channel.addEventListener(Event.SOUND_COMPLETE, soundCompleteHandler);
            updated();
            myTextField.appendText("Playing\n");
            if (myTimer) myTimer.start();
        }

        public function stop():void {
            channel.stop();
            position=0;
            channel=null;
            updated();            
            if (myTimer) myTimer.stop();
        }

        public function pause():Number {
            if (channel) {
                channel.stop();
                position=channel.position;
            }

            channel=null;
            updated();
            return position;
        }



        public function load(url:String):void {
            
            snd=new Sound();
            snd.addEventListener(Event.ID3, id3Handler);
            snd.load(new URLRequest(url));

            position=0;

            myTextField.appendText("Load "+url+"\n");
            if (myTimer) myTimer.start();

        }

        public function setInterval(millis:Number):void {
            if (myTimer) myTimer.stop();
            myTimer = new Timer(millis,0);
            myTimer.addEventListener(TimerEvent.TIMER,updateHandler);
            myTimer.start();
        }

        public function getInfo(key:String):String {
            if (key=='version') return version;
            if (id3) return id3[key];
            return '';
        }


        public function mp3player() {

           
            
            myTextField.x=0;
            myTextField.y=0;
            myTextField.width=500;
            myTextField.height=300;

            myTextField.appendText("Start "+version+"\n");

            this.addChild(myTextField);                        

                      

            var parameters:Object=LoaderInfo(this.root.loaderInfo).parameters;
            if (parameters['url']) load(String(parameters['url']));
            if (parameters['play']) play();

            if (ExternalInterface.available) {
                ExternalInterface.marshallExceptions=true;
                ExternalInterface.addCallback("getInfo", getInfo);
                ExternalInterface.addCallback("seekAudio", seek);
                ExternalInterface.addCallback("playAudio", play);
                ExternalInterface.addCallback("pauseAudio", pause);
                ExternalInterface.addCallback("stopAudio", stop);
                ExternalInterface.addCallback("onEnded", onEnded);
                ExternalInterface.addCallback("onUpdate", onUpdate);
                ExternalInterface.addCallback("loadAudio", load);
                ExternalInterface.addCallback("setInterval", setInterval);

            }


            myTextField.appendText("Started\n");

        }

        public function updateHandler(event:TimerEvent):void {
            updated();
        }

        private function updated():void {
            var p:Number=channel?channel.position:position;

            if (onUpdateCallback) ExternalInterface.call(onUpdateCallback,channel?true:false,p,snd.length,snd.bytesLoaded,snd.bytesTotal);
        }

        public function soundCompleteHandler(event:Event):void {
            myTextField.appendText("Finished\n");
            
            stop();
            if (onEndedCallback) ExternalInterface.call(onEndedCallback);
        
        }



        private function id3Handler(event:Event):void {
            id3 = snd.id3;
            myTextField.appendText('ID3 received \n');
           for (var propName:String in id3) {
                myTextField.appendText(propName + " = " + id3[propName] + "\n");
            }
        }



        
    }
}