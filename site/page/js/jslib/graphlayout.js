JSLIB.depend('graphlayout',['dom.js'],
function(dom){

    var logger=this.logger;

    this.RectangularNode=RectangularNode;
    this.Edge=Edge;
    this.LineEdge=LineEdge;
    this.Layout=Layout;

    function RectangularNode(svg,name,left,right,text) {

        this.toString=function() {return name;};

        var width=100;
        var height=0;



        var rect=svg.rect(-width/2,-height/2,width,height);
        rect.setAttribute('fill','#fff');
        rect.setAttribute('pointer-events','visible');
        rect.setAttribute('stroke','#000');
        rect.setAttribute('stroke-width','1');
        //rect.setAttribute("shape-rendering","crispEdges");


        var titleRect=svg.rect(0,0,width,9,{fill:'#000',stroke:'none'});

        var rightText=svg.text(0,8,right,{"font-family":'sans-serif',"font-size":'9',fill:'#fff'});

        var textHolder=svg.g({},titleRect,svg.text(0,8,left,{"font-family":'sans-serif',"font-size":'9',fill:'#fff'}),rightText);

        var bottom=this.bottom=svg.g();


        function setHeight(newHeight) {

            height=newHeight;
            rect.setAttribute('y',-height/2);
            rect.setAttribute('height',height);
            svg.setTransform(textHolder,svg.translate(-width/2,-height/2));
            svg.setTransform(bottom,svg.translate(-width/2,height/2));
        }

        function wrapText() {



            if (height>0) return;
            //logger.log('Wrap text',text);

            rightText.setAttribute('x',width-rightText.getComputedTextLength()-1);

            var currentLine;
            var yt=10;
            var haveWord;
            var end=0;
            var start=0;

            function trimSubstring(upto) {
                return text.substring(start,upto).replace(/ +$/,'')+(haveWord || upto==text.length?'':'-');
            }

            function endLine() {
                dom.replace(currentLine,trimSubstring(end));
                var textWidth=currentLine.getComputedTextLength();                
                currentLine.setAttribute('x',width/2-textWidth/2);
            }

            function newLine() {
                yt+=10;
                currentLine=svg.text(0,yt,'',{"font-family":'sans-serif',"font-size":'10'});
                dom.add(textHolder,currentLine);
                haveWord=false;
                start=end;
            }

            newLine();


            for (var i=0;i<text.length;i++) {
                dom.replace(currentLine,trimSubstring(i+1));
                var length=currentLine.getComputedTextLength();
                if (length>width-10) {
                    endLine();
                    newLine();
                } else {
                    if (text[i]==' ') {haveWord=true;end=i+1;}
                    if (!haveWord) end=i+1;
                }
            }

            end=text.length;
            endLine();

            setHeight(yt+5);

        };




        var g=this.content=svg.g({},rect,textHolder,bottom);

        var drag;
        var x;
        var y;

        this.setXY=function(newX,newY,newDrag) {
            x=newX;
            y=newY;
            drag=newDrag;
            //logger.log('set ',x,y);
            svg.setTransform(g,svg.translate(x,y));
        };

        this.getWidth=function() {return width;};
        this.getHeight=function() {wrapText();return height;};

        this.getPoint=function() {return {x:x,y:y};};        
        this.move=function(x,y) {return drag.move(x,y);};
        this.finished=function() {return drag.finished();};
        this.setColour=function(colour) {
            rect.setAttribute('stroke',colour);
            textHolder.setAttribute('fill',colour);

        };
    };

    function Edge(parent,child) {

        this.parent=parent;
        this.child=child;




    };

    function LineHandle() {
    }

    function LineEdge(svg,parent,child,name,style) {

        Edge.call(this,parent,child);

        var path=this.content=svg.path(null,style);
        path.setAttribute('fill','none');
        path.setAttribute('pointer-events','none');
        path.setAttribute('stroke-width','2');

        this.getPath=function() {
            return path;
        };

        this.toString=function() {return name;};
        this.setColour=function(colour) {
            path.setAttribute('stroke',colour);            

        };
    };


    function Layout(svg) {

        var layout=this;

        function ProxyEdge(edge,parentProxy,childProxy) {
            Edge.call(this,parentProxy,childProxy);
            var locations=[];
            var routes=[];

            this.setRoute=function(levelNumber,route) {
                routes[levelNumber]=route;
            };

            this.getLevelLocation=function(levelNumber) {
                if (levelNumber==childProxy.getLevel().getNumber()) return childProxy.getLocation();
                if (levelNumber==parentProxy.getLevel().getNumber()) return parentProxy.getLocation();
                return locations[levelNumber];

            };

            this.store=function() {

                var childLevel=childProxy.getLevel();
                var parentLevel=parentProxy.getLevel();

                var path=edge.getPath();

                svg.clearPath(path);
                var parentY=parentProxy.getLevel().where()+parentProxy.getHeight()/2;
                svg.moveTo(path,parentProxy.getLocation().get(),parentY);

                //logger.log('Path '+locations.length);
                var parentX=parentProxy.getLocation().get();

                for (var i=parentLevel.getNumber();i<childLevel.getNumber();i++) {

                    var childY=findLevel(i+1).where();
                    if (i==childLevel.getNumber()-1) childY-=childProxy.getHeight()/2;

                    var childX=this.getLevelLocation(i+1).get();
                    var route=routes[i];


                    svg.lineTo(path,parentX,route-10);
                    svg.cubicTo(path,childX,route+10,parentX,route,childX,route);
                    svg.lineTo(path,childX,childY);

                    //var mid=(previousLevel.where()+level.where())/2;


                    //psl.appendItem(path.createSVGPathSegCurvetoCubicAbs(location.get(),level.where(),previousLocation.get(),mid,location.get(),mid));
                    //if (i!=positions.length-1) psl.appendItem(path.createSVGPathSegLinetoAbs(e.child.x,e.child.y+e.child.getHeight()/2));

                    parentX=childX;
                }

            };


            this.checkLevels=function() {
                var childLevel=childProxy.getLevel();
                var parentLevel=parentProxy.getLevel();
                childProxy.setLevelAfter(parentProxy.getLevel().getNumber()+1);
                for (var i=0;i<Math.max(locations.length,childLevel.getNumber());i++) {
                    var required=i>parentLevel.getNumber() && i<childLevel.getNumber();
                    if (!locations[i] && required) {
                        locations[i]=new Location(this,[this],20,20);
                        findLevel(i).add(locations[i]);
                    }
                    if (locations[i] && !required) {
                        findLevel(i).remove(locations[i]);
                        locations[i]=null;
                    }
                }

            };

            this.checkLevels();

            this.toString=function() {
                return 'Proxy:'+edge;
            };

            this.getUnderlying=function() {
                return edge;
            };

        }

        function Location(underlying,edges,width,height) {

            var position;

            this.get=function() {return position;};
            this.set=function(p) {position=p;};
            this.getWidth=function() {return width;};
            this.getHeight=function() {return height;};
            this.getLeft=function() {return position-width/2;};
            this.getRight=function() {return position+width/2;};
            this.moveRight=function(dx) {return position+=dx;};

            this.setToAverageEdgeLocation=function(comparedLevel) {
                var total=0;
                var count=0;

                for (var i=0;i<edges.length;i++) {
                    var compared=edges[i].getLevelLocation(comparedLevel);
                    if (compared) {
                        total+=compared.get();
                        count++;
                    }
                }
                //logger.log('average location',this,comparedLevel,count,total/count);
                if (count==0) return null;
                    
                position=total/count;
            };

            this.getEdges=function() {
                return edges;
            };

            this.toString=function() {return underlying.toString();};

        }

        function ProxyNode(node) {



            this.toString=function() {
                return 'Proxy:'+node;
            };

            var edges=[];

            var location=new Location(this,edges,node.getWidth()+20,node.getHeight());

            this.getLevel=function() {return level;};

            this.getLocation=function() {return location;};

            this.addEdge=function (proxyEdge) {
                edges.push(proxyEdge);
            };

            var level;
            function setLevel(number) {
                //logger.log('set level',node,number);
                if (level) level.remove(location);
                level=findLevel(number);
                level.add(location);
                for (var i=0;i<edges.length;i++) edges[i].checkLevels();
                //logger.log('finished set level',node,number,level.getNumber(),level);
            };

            function setLevelTo(number) {
                if (level.getNumber()!=number) {
                    setLevel(number);
                    update();
                }

            }

            this.setLevelAfter=function (number) {
                if (level.getNumber()<number) {
                    setLevel(number);
                }

            };

            this.move=function(x,y) {
                var closest=0;
                var miny=1000;
                for (var l=0;l<levels.length;l++) {
                    var dy=Math.abs(levels[l].where()-y);
                    if (dy<miny) {
                        closest=l;
                        miny=dy;
                    }
                }
                setLevelTo(closest);
                location.set(x);
                this.store();
                for (var i=0;i<edges.length;i++) {
                    edges[i].store();
                }
                //refresh();
            };

            this.finished=function() {
                refresh();
            };


            this.store=function() {

                node.setXY(location.get(),level.where(),this);
                location.locked=true;
            };

            this.getHeight=function() {return node.getHeight();};

            setLevel(0);

            location.locked=false;

            this.getUnderlying=function() {
                return node;
            };
        }



        function findLevel(n) {
            while (levels.length<=n) levels.push(new Level(levels.length));
            return levels[n];
        }

        function Level(n) {
            var locations=[];
            var position;
            this.getLocations=function() {return locations;};
            this.getNumber=function() {return n;};
            this.where=function() {return position;};
            this.getLeft=function() {
                return locations.length==0?0:locations[0].getLeft();
            };
            this.moveLeft=function(dx) {
                 for (var l=0;l<locations.length;l++) {
                    locations[l].set(locations[l].get()-dx);
                }
            };

            this.setPosition=function(p) {
                var height=getHeight();
                position=p+height/2;
                //logger.log('Height',height);
                return height;
            };
            this.add=function(n) {locations.push(n);};
            this.remove=function(n) {locations.remove(n);};
            this.toString=function() {return 'Level'+n+'('+locations.join(',')+')';};

            function getHeight() {
                var max=0;
                for (var c=0;c<locations.length;c++) {
                    max=Math.max(max,locations[c].getHeight());
                }
                return max;
            };

            this.reset=function() {
                var pos=0;
                for (var c=0;c<locations.length;c++) {
                    pos+=locations[c].getWidth()/2;
                    if (!locations[c].locked) locations[c].set(pos);
                    pos=locations[c].getRight();
                }
            };

            this.removeOverlaps=function() {

                locations.sort(function (l1,l2) {return l1.get()-l2.get();});

                function gap(i1,i2) {
                    return locations[i2].getLeft()-locations[i1].getRight();
                }

                function shiftLeft(dx,from) {
                    while (dx>0) {
                        locations[from].moveRight(-dx);
                        if (from==0) return;
                        var g=gap(from-1,from);
                        if (g>0) dx-=g;
                        from--;
                    }
                }

                function shiftRight(dx,from) {
                    while (dx>0) {
                        locations[from].moveRight(dx);
                        if (from==locations.length-1) return;
                        var g=gap(from,from+1);
                        if (g>0) dx-=g;
                        from++;
                    }
                }


                for (var r=1;r<locations.length;r++) {
                    var g=gap(r-1,r);
                    if (g<0) {
                        shiftLeft(-g/2+1,r-1);
                        shiftRight(-g/2+1,r);
                    }
                }

            };

            this.arrange=function(compare) {


                for (var l=0;l<locations.length;l++) {
                    //if (!locations[l].locked) 
                        locations[l].setToAverageEdgeLocation(compare);
                }



                this.removeOverlaps();

            };

            function Reservation(p1,p2) {
                this.start=Math.min(p1,p2);
                this.end=Math.max(p1,p2);
                this.overlaps=function (other) {
                    return Math.min(this.end,other.end)>Math.max(this.start,other.start);
                };
            }

            function Route() {
                var occupation=[];
                this.occupied=function(reservation) {
                    for (var i=0;i<occupation.length;i++) {
                        if (occupation[i].overlaps(reservation)) return true;
                    }
                    occupation.push(reservation);
                    return false;
                };
            }

            this.routeEdges=function(position) {
                var routes=[];
                if (n==levels.length-1) return 0;
                for (var r1=0;r1<locations.length;r1++) {
                    var edges=locations[r1].getEdges();
                    for (var e=0;e<edges.length;e++) {
                        var edge=edges[e];
                        var parent=edge.getLevelLocation(n);
                        var child=edge.getLevelLocation(n+1);
                        //logger.log('Route',edge,parent,child);
                        if (parent!=null && child!=null) {
                            var r=0;
                            var reservation=new Reservation(parent.get(),child.get());
                            while ((routes[r]=routes[r]||new Route()).occupied(reservation)) r++;
                            var pos=20*(r+1)+position;
                            edge.setRoute(n,pos);
                            //logger.log('TO',n,pos,parent.get(),child.get());
                        }
                    }
                }
                //logger.log('Routes',routes.length);
                return (routes.length+1)*20;
            };
        }

        var levels=[];
        var proxyEdges=[];
        var proxyNodes=[];


        var addNode=this.addNode=function(n) {
            if (n.proxy) return n.proxy;
            proxyNodes.push(n.proxy=new ProxyNode(n));
            return n.proxy;
        };

        this.addEdge=function(e) {
            var parentProxy=addNode(e.parent);
            var childProxy=addNode(e.child);
            var proxy=new ProxyEdge(e,parentProxy,childProxy);
            proxyEdges.push(proxy);
            childProxy.addEdge(proxy);
            parentProxy.addEdge(proxy);
        };


        function setLevelPositions() {
            var position=0;

            for (var i=0;i<levels.length;i++) {
                //logger.log('Route',levels[i]);
                position+=levels[i].setPosition(position);
                position+=levels[i].routeEdges(position);
            }
        }


        function removeOverlaps() {
            for (var d=1;d<levels.length;d++) {
                levels[d].removeOverlaps();
            }
        }

        function rearrangeLevels() {
            for (var r=0;r<levels.length;r++) {
                levels[r].reset();
            }

            for (var re=0;re<levels.length;re++) {
                for (var d=1;d<levels.length;d++) {
                    levels[d].arrange(d-1);
                }
                for (var u=levels.length-2;u>=0;u--) {
                    levels[u].arrange(u+1);
                }
            }

        }

        function leftAlign() {
            var minLeft=levels[0].getLeft();

            for (var i=0;i<levels.length;i++) {
                minLeft=Math.min(levels[i].getLeft(),minLeft);
            }

            for (var l=0;l<levels.length;l++) {
                levels[l].moveLeft(minLeft);
            }

        }

        function store() {
            for (var kn=0;kn<proxyNodes.length;kn++) {
                proxyNodes[kn].store();
            }

            for (var ke=0;ke<proxyEdges.length;ke++) {
                proxyEdges[ke].store();
            }
        }

        function refresh() {
            removeOverlaps();

            //rearrangeLevels();
            setLevelPositions();
            store();
        }

        function update() {
            rearrangeLevels();
            setLevelPositions();
            leftAlign();
            store();
        }




        this.updateLayout=function() {
            update();

        };

        function reset() {
            var oldEdges=proxyEdges;
            var oldNodes=proxyNodes;
            levels=[];
            proxyEdges=[];
            proxyNodes=[];

            for (var j=0;j<oldNodes.length;j++) {
                oldNodes[j].getUnderlying().proxy=null;
            }

            for (var i=0;i<oldEdges.length;i++) {
                var e=oldEdges[i].getUnderlying();
                e.proxy=null;
                layout.addEdge(e);
            }
        }

        this.resetLayout=function() {
            reset();
            update();
        };

    }
});