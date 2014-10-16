JSLIB.depend('quickgoChartSVG',['jslib/dom.js','jslib/svg.js','jslib/graphlayout.js','jslib/remote.js','jslib/progressive.js','jslib/parameters.js','jslib/lightbox.js','jslib/tabs.js','quickgoSelection.js'],
function(dom,svg,graphLayout,remote,progressive,parameters,lightbox,tabs,quickgoSelection){

/**
 * This module is not used in QuickGO as of 25-OCT-2010
 *
 * This is an implementation of the term ancestor chart layout and rendering in SVG
 *
 * David Binns
 */


	var quickgoChartSVG=this;

    var logger=this.logger;
    logger.log("Term charts");

    var queue=new remote.JSONCache(new remote.JSONQueue('GTerm',{format:'json'}),'id');

    var relationColours={'=':'#000','?':'#fff',
                            'I':'#000','P':'#00f','R':'#fc0','PR':'#0f0','NR':'#f00','+':'#0f0','-':'#f00'};


    function makeExplorer(elt) {



        var height=window.innerHeight-20;

        logger.log('height:',height);

        var chart=new svg.SVG(document,{width:'100%',height:'100%'});

        var ancestry=new GOAncestry(chart);

        //var href = dom.find(holder,'a').getAttribute('href');
        var id=parameters.parseHREF(window.location.search)['id'];

        logger.log("initialising explorer",id);

        dom.add(chart,new chart.PanAndZoom(ancestry));



        var chartHolder=dom.styleDiv({border:'1px solid black',height:'100%',width:'100%'},chart);
        var layoutButton=dom.imgtextbutton(ancestry.reset,'image/ig/layout.png','Reset');
        var collapseButton=dom.imgtextbutton(collapse,'image/ig/collapse.png','Collapse');
        var expandButton=dom.imgtextbutton(expand,'image/ig/expand.png','Expand');
        var closeButton=dom.imgtextbutton(hideExplorer,'image/ig/close.png','Close');
        var controls=dom.floatDiv('right',dom.classDiv('toolbar',layoutButton,expandButton,collapseButton,closeButton,dom.styleDiv({clear:'both'})));


        var detailArea=dom.div(ancestry.infoHTML);
        var infoBox=dom.styleDiv({width:'300px',overflow:'auto',fontSize:'10px',position:'absolute',right:0,height:'100%',padding:'3px'},controls,detailArea,'hello!');
        //var infoBox=dom.styleDiv({},controls,ancestry.infoHTML);

        var holdAll=dom.styleDiv({zIndex:2000,left:0,right:0,top:0,bottom:0},infoBox,chartHolder);
        holdAll.className='fixed';

        function expand() {
            infoBox.style.height='100%';
            detailArea.style.display='block';
            collapseButton.style.display='inline-block';
            expandButton.style.display='none';
        }

        function collapse() {
            infoBox.style.height='60px';
            detailArea.style.display='none';
            expandButton.style.display='inline-block';
            collapseButton.style.display='none';
        }


        function hideExplorer() {
            dom.remove(holdAll);
        }

        function showExplorer(e) {
            dom.add(document.body,holdAll);
            ancestry.loadTerm(id);
            expand();
            dom.stop(e);
        }

        dom.onclick(elt,showExplorer);
    }





    function GOAncestry(chart) {

        var definition=dom.div();
        var childrenList=dom.div();
        var selectionList=dom.div();

        var helpArea=dom.styleDiv({background:'#eef5f5'},'Click on a term to explore child (more specific) terms');

        function termRow(id,name) {
            var childRow=dom.row(id,name);
            dom.onclick(childRow,function(){loadTerm(id);});
            return childRow;
        }

        function refreshSelection(list) {
            dom.empty(selectionList);
            for (var i=0;i<list.length;i++) {
                dom.add(selectionList,termRow(list[i].id,list[i].name));
            }
        }

        quickgoSelection.registerSelectionListener(refreshSelection);



        this.infoHTML=dom.styleDiv({background:'#eef5f5'},helpArea,
                dom.styleDiv({background:'#066',color:'#fff',fontWeight:'bold'},'Definition'),definition,
                dom.styleDiv({background:'#066',color:'#fff',fontWeight:'bold'},'Children'),childrenList,
                dom.styleDiv({background:'#066',color:'#fff',fontWeight:'bold'},'Term Basket'),selectionList);




        var g=this.content=chart.g();

        function unselect() {
            helpArea.style.display='block';
            dom.empty(definition);
            dom.empty(childrenList);

            for (var x in terms) terms[x].fade(false);
            for (var j=0;j<edges.length;j++) edges[j].fade(false);

        }

        dom.onclick(chart,unselect);

        var layout=new graphLayout.Layout(chart);

        var terms={};
        var edges=[];

        function Term(id,name) {

            var term=this;

            graphLayout.RectangularNode.call(this,chart,id,id,'',name);
            dom.add(g,term);


            this.fade=function(yes) {
                dom.empty(relations);
                relationsIndex={};
                this.setColour(yes?'#ccc':'#000');
            };


            var relations=chart.g();
            var relationsIndex={};


            dom.add(this.bottom,relations);

            this.hilight=function(relation) {
                if (!relationsIndex[relation] && relation!='=') {
                    var c=0;
                    for (var ri in relationsIndex) c++;
                    relationsIndex[relation]=chart.rect(c*12+1,-2,10,4,{fill:relationColours[relation],strokeWidth:'1',stroke:'#000','shape-rendering':"crispEdges"});
                    dom.add(relations,relationsIndex[relation]);
                }
                for (var i=0;i<this.parentRelations.length;i++) this.parentRelations[i].hilight(relation);

                this.setColour('#000');
            };


            this.ancestors=[this];
            this.ancestralEdges=[];
            this.parentRelations=[];

            dom.onclick(this,function(e){

                dom.stop(e);

                for (var x in terms) terms[x].fade(true);
                for (var j=0;j<edges.length;j++) edges[j].fade(true);

                term.hilight("=");


                //for (var i=0;i<term.ancestors.length;i++) term.ancestors[i].hilight();

                //for (var k=0;k<term.ancestralEdges.length;k++) term.ancestralEdges[k].hilight();

                dom.replace(definition,dom.img('image/ajax-loader-big.gif'));

                helpArea.style.display='none';

                dom.empty(childrenList);
                function loaded(termData) {
                    dom.replace(definition,termData['termInfo']['definition']);

                    var children=termData['termInfo']['children'];
                    for (var i=0;i<children.length;i++) {
                        var childId = children[i]['child']['id'];
                        var childName = children[i]['child']['name'];
                        dom.add(childrenList,termRow(childId,childName));

                    }
                }
                queue.get(id,loaded);
            });
        }

        function Relation(parent,child,relationType) {
            graphLayout.LineEdge.call(this,chart,parent,child,parent+'-'+relationType+'-'+child,{});

            logger.log(this);

            var colour=relationColours[relationType];

            this.fade=function(yes) {
                this.setColour(yes?'#ccc':'#000');
            };
            this.hilight=function(childRelation) {
                if (childRelation=='i' || childRelation=='=') parent.hilight(relationType);
                else if (relationType=='i' || relationType=='=') parent.hilight(childRelation);
                else if (childRelation=='p' && relationType=='i') parent.hilight('p');
                else if (childRelation=='i' && relationType=='p') parent.hilight('p');
                else if (childRelation.match(/.r/) && relationType=='p') parent.hilight('r');
                else if (childRelation=='h' && relationType=='i') parent.hilight('h');
                else if (childRelation=='i' && relationType=='h') parent.hilight('h');
                else parent.hilight('?');

                this.setColour(colour);
            };

            this.setColour(colour);
        }




        function addTerm(termData) {
            var ancestry=termData["ancestry"];
            var ancestryTerms=[];
            for (var i=0;i<ancestry.length;i++) {
                var ancestor=ancestry[i]["term"];
                var id=ancestor['id'];
                var term=terms[id];
                if (!term) {
                    term=terms[id]=new Term(id,ancestor["name"].replace('_',' '));

                    var relations=ancestry[i]["relations"];
                    logger.log(relations);
                    for (var j=0;j<relations.length;j++) {
                        /*if (relations[j]=='0' || relations[j]=='h' || relations[j]=='v' || relations[j]=='hv') continue;*/
	                    if (relations[j]=='0' || relations[j]=='v' || relations[j]=='hv') continue;
                        var edge=new Relation(ancestryTerms[j],term,relations[j].toUpperCase());
                        edges.push(edge);
                        term.parentRelations.push(edge);
                        //term.ancestors=term.ancestors.concat(ancestryTerms[j].ancestors);
                        //term.ancestralEdges=term.ancestralEdges.concat(edge,ancestryTerms[j].ancestralEdges);
                        layout.addEdge(edge);
                        dom.add(g,edge);
                    }
                }
                ancestryTerms[i]=term;

                chart.enableDrag(term);

            }

            layout.updateLayout();
        }

        this.reset=function() {layout.resetLayout();};

        var loadTerm=this.loadTerm=function (id) {
            logger.log("LoadTerm",id);
            queue.get(id,addTerm);
        };


    }



});