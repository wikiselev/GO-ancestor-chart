JSLIB.depend('dom',['doc.js'],function dom(doc) {

    var logger=this.logger;
    logger.log('DOM default document');
    doc.DOM.apply(this,[document]);

    var dom=this;

    dom.afterDOMLoad(function() {
            var sampleInput=dom.input();
            sampleInput.content.style.width='50%';

            var marker=dom.div();
            marker.style.border='1px solid red';
            marker.style.display='none';
            marker.style.position='absolute';

            function compute(x){
                if (x!=null) {
                    if (x.parentNode) {
                        var pos=dom.where(x,document.body);
                        logger.log(x,pos);
                        dom.move(marker,pos.left,pos.bottom);
                        marker.style.width=pos.width+'px';
                        marker.style.height='0px';
                        marker.style.display='block';

                    } else marker.style.display='none';
                    logger.log(x);
                }

            }
            function computeInput(){
                compute(eval(sampleInput.value));
            }
            function clear() {
                dom.empty(logger.content);
            }
            dom.oncompletion(sampleInput,computeInput);
            var controlPanel=dom.div(dom.add(dom.button(clear),'Clear'),sampleInput);

            var holder=dom.div(controlPanel,logger);
            holder.style.padding='10px';
            holder.style.border='1px solid black';
            holder.style.backgroundColor='#cfc';
            holder.style.color='#000';

            var terminal=dom.div(holder);
            terminal.style.display=window.location.search.indexOf('debug=show')>0?'block':'none';


            terminal.style.position='absolute';
            terminal.style.left='0px';
            terminal.style.top='0px';
            terminal.style.right='0px';
            terminal.style.bottom='0px';
            terminal.style.zIndex='1000';
            //terminal.style.width='100%';
            //terminal.style.height='100%';
            //terminal.style.overflow='scroll';

            var container=dom.div(terminal);
            //container.style.position='relative';
            //container.style.border='1px solid green';

            // This seems to help. Not sure why:
            document.body.insertBefore(document.createTextNode(' '),document.body.firstChild);
            document.body.insertBefore(container,document.body.firstChild);
            document.body.insertBefore(marker,document.body.firstChild);



            var isopen=false;

            dom.onkeydown(document,function(ev){

                if (ev.keyCode==dom.keycode.F2) {
                    isopen=!isopen;
                    terminal.style.display=isopen?"block":"none";
                }

            });



            logger.log('loaded');

        });
});

