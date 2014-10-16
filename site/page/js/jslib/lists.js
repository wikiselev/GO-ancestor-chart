JSLIB.depend('lists',['dom.js'],
        function lists(dom) {

    var imageBase=this.dirname(this.path);

    var logger=this.logger;
    logger.log('Expandable lists:'+imageBase);



    var ExpandList=this.ExpandList=function ExpandList(element) {
        var listEl=element||dom.el('ul');


        this.attachLi=function(itemEl,subList,collapsed) {
            itemEl.style.listStyle='none';
            itemEl.style.fontSize='12px';
            itemEl.style.margin='0px';
            itemEl.style.padding='0px';
            itemEl.style.paddingLeft='10px';
            itemEl.style.borderLeft='1px solid white';
            itemEl.style.position='relative';
            var ps=itemEl.previousSibling;
            while (ps && ps.nodeName.toUpperCase()!='LI') ps=ps.previousSibling;
            if (ps) {
                ps.style.borderLeft='1px solid black';
            }

            var link=dom.div();
            link.style.marginLeft='-11px';
            link.style.borderBottom='1px solid black';
            link.style.borderLeft='1px solid black';
            link.style.height='8px';
	    link.style.fontSize='8px';
            link.style.width='10px';
//            link.style.cssFloat='left';
            link.style.position='absolute';

            var expander=dom.img(imageBase+'bullet_black.png');
            expander.style.verticalAlign='bottom';
	    expander.style.marginRight='10px';

            if (subList) {
                subList.style.margin='0px';
                subList.style.borderLeft='1px solid white';
                subList.style.padding='0px';
                subList.style.paddingLeft='8px';


                function setCollapsed(newCollapsed) {
                    collapsed=newCollapsed;
                    subList.style.display=collapsed?'none':'block';
                    expander.src=imageBase+'bullet_toggle_'+(collapsed?'plus':'minus')+'.png';
                    //dom.replace(expander,collapsed?'+':'-');
                }
                setCollapsed(collapsed);
                dom.onclick(expander,function(e){setCollapsed(!collapsed);dom.stopPropagation(e);});
            }
            itemEl.insertBefore(expander,itemEl.firstChild);
//            itemEl.appendChild(link);
	    itemEl.insertBefore(link,itemEl.firstChild);

        };

        this.addItem=function(title,subList,collapsed) {
            var itemEl=dom.el('li');
            dom.add(listEl,dom.add(itemEl,title,subList));
            return itemEl;
        };
        var li=element.firstChild;

        while (li) {
            if (li.nodeName.toUpperCase()=='LI') {            
                var subList=dom.find(li,'ul');
                if (subList) {new ExpandList(subList);}
                var collapsed=li.className=='collapsed';
                this.attachLi(li,subList,collapsed);
            }
            li=li.nextSibling;
        }
        this.content=listEl;
    };

    this.enhanceLists=function() {
        return function enhanceList(node) {
            var list=dom.find(node,'ul');
            new ExpandList(list);
        };
    };
});
