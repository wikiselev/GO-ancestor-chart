JSLIB.depend('print',['dom.js'],function menu(dom) {

    var logger=this.logger;
    logger.log('JSLIB.print: printing');


    var printElement;
    var printHolder=dom.div();
    var printTitle=dom.input();

    printTitle.content.style.width='50%';
    printTitle.content.style.fontSize='200%';
    printTitle.content.style.border='none';
    var textSize=dom.input('100',2,function(v){printHolder.style.fontSize=v+'%';});
    var closeButton=dom.button(endPrint,'Close');
    var printButton=dom.button(nowPrint,'Print');
    var buttonHolder=dom.div(textSize,'%',closeButton,printButton);
    buttonHolder.style.cssFloat='right';

    var titleBar=dom.div(buttonHolder,printTitle);
    var printFrame=dom.div(titleBar,printHolder);

    printFrame.style.visibility='visible';
    printFrame.style.position='absolute';
    printFrame.style.x='0';
    printFrame.style.y='0';
    printFrame.style.width='277mm';
    printFrame.style.height='190mm';
    printFrame.style.overflow='hidden';
    printFrame.style.fontSize='100%';

    dom.onmouseover(titleBar,function(evt){
        buttonHolder.style.visibility='visible';
        printFrame.style.border='1px solid black';
        printFrame.style.margin='-1px';
        dom.stopPropagation(evt);
    });
    dom.onmouseover(printFrame,function(evt){
        buttonHolder.style.visibility='hidden';
        printFrame.style.border='none';
        printFrame.style.margin='0';
    });

    this.print=function(title,target) {
        document.body.style.visibility='hidden';
        buttonHolder.style.visibility='hidden';
        document.body.insertBefore(printFrame,document.body.firstChild);
        printElement=target;
        printHolder.appendChild(printElement);
        printTitle.value=title;


    };

    function nowPrint() {
        buttonHolder.style.visibility='hidden';
        window.setTimeout(window.print,10);
    }

    function endPrint() {
        printHolder.removeChild(printElement);
        document.body.removeChild(printFrame);
        document.body.style.visibility='visible';
    }
});