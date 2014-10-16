JSLIB.depend('progressive',['dom.js'],function(dom) {

    var logger=this.logger;
    logger.log('Progressive enhancement');       
    
    this.attachLoad=function(doc) {
        if (!doc) doc=document;
        
    };
    this.enhanceAll=function enhance(node,tagname,actions) {
        if (!tagname) tagname='j';
        var all=node.getElementsByTagName(tagname);
        for (var i=0;i<all.length;i++) {
            var child=all[i];
            logger.log('Enhance '+child+' '+child.className);
            if (!child.className || !actions[child.className]) continue;
            try {
            actions[child.className](child.parentNode);
            } catch (e) {
                logger.log('Failure while enhancing. Ignoring and proceeding');
                logger.error(e);
            }
        }
    };

    this.enhance=function enhance(node,tagName,className,callback) {
        var all=dom.findAll(node,tagName,className);
        for (var i=0;i<all.length;i++) callback(all[i]);
    };

    this.enhanceSwitchJS=function() {
        return function(node) {
            var options=node.getElementsByTagName('a');
            var x=options[0].style.cssText;
            options[0].style.cssText=options[1].style.cssText;
            options[1].style.cssText=x;
        };
    };
});