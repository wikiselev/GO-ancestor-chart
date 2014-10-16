JSLIB.depend('test',['dom.js','tabs.js','remote.js','progressive.js','popup.js','table.js','lists.js','lightbox.js'],
        function test(dom,tabs,remote,progressive,popup,table,lists,lightbox) {

    var logger=this.logger;
    logger.log('Testing library');

    progressive.enhanceAll(document.body,'i',{
        'dynamic':remote.enhanceDynamicPopup('images/loading.gif','#(.*)','?snippet=$1'),
        'tabs':tabs.enhanceTabs('h2'),
        'popup':popup.enhancePopup(),
        'suggest':remote.enhanceSuggest('images/loading.gif',{embed:''}),
        'dynamic-table':table.enhanceDynamic(),
        'list':lists.enhanceLists(),
        'lightbox':lightbox.enhanceLightboxes('normal')
    });


});

