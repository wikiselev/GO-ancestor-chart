JSLIB.depend('svg',['../jslib/dom.js'],
function(dom){

    var logger=this.logger;

    this.SVG=function(document,style) {
        var svg=this;


/*

        var wrapSVG=function(element) {
            if (element==null) return null;
            if (element.wrapped) return element.wrapped;
            var obj=wrapNode(element,wrap);
            if (obj==null) return null;
            obj.transformTo = function(target) {
                return obj.element.getTransformToElement(target.element);
            };
            obj.transform=function (tf) {
                obj.setAttribute("transform",tf);
                return this;
            };
            obj.getBBox=function() {
                return element.getBBox();
            };
            obj.getTransform=function() {
                var tf = element.transform.baseVal.consolidate();
                if (tf==null) element.transform.baseVal.appendItem(tf=svg.element.createSVGTransform());
                return tf;
            };
            obj.stroke=function (paint,width,opacity) {
                obj.setAttribute("stroke",paint);
                if (width) obj.setAttribute("stroke-width",width);
                if (opacity) obj.setAttribute("stroke-opacity",opacity);
                return this;
            };
            obj.fill=function (v) {
                obj.setAttribute("fill",v);
                return this;
            };
            var clientPoint=obj.clientPoint=function(x,y) {
                return svg.point(x,y).matrixTransform(element.getScreenCTM().inverse());
            };
            var eventPoint=obj.eventPoint=function(e) {
                return clientPoint(e.clientX,e.clientY);
            };
            return obj;
        };
*/

        function el(name) {
            return document.createElementNS("http://www.w3.org/2000/svg",name);
        };


        svg.path=function(d,style) {
            var path=setStyle(el("path"),style);
            path.setAttribute("stroke","#000");
            path.setAttribute("stroke-width","1");
            if (d) path.setAttribute("d",d);
            /*path.clear=function() {
                //logging.log("PESL"+path.element.pathSegList);
                path.element.pathSegList.clear();
                return path;
            };
            path.moveTo=function(x,y) {
                if (x.x && x.y) {y=x.y;x=x.x;}
                path.element.pathSegList.appendItem(path.element.createSVGPathSegMovetoAbs(x,y));
                return path;
            };
            path.lineTo=function(x,y) {
                if (x.x && x.y) {y=x.y;x=x.x;}
                path.element.pathSegList.appendItem(path.element.createSVGPathSegLinetoAbs(x,y));
                return path;
            };
            path.close=function() {
                path.element.pathSegList.appendItem(path.element.createSVGPathSegClosePath());
                return path;
            };*/
            return path;
        };
        svg.rect=function(x,y,width,height,style) {
            var r=setStyle(el("rect"),style);
            r.setAttribute("x",x);
            r.setAttribute("y",y);
            r.setAttribute("width",width);
            r.setAttribute("height",height);
            return r;
        };

        svg.text=function(x,y,text,style) {
            var r=setStyle(el("text"),style);
            r.setAttribute("x",x);
            r.setAttribute("y",y);
            r.appendChild(document.createTextNode(text));
            return r;
        };
        svg.tspan=function(x,y,text,style) {
            var r=setStyle(el("tspan"),style);
            r.setAttribute("x",x);
            r.setAttribute("y",y);
            r.appendChild(document.createTextNode(text));
            return r;
        };
        svg.setXY=function(e,x,y) {
            e.setAttribute("x",x);
            e.setAttribute("y",y);
            return e;
        };
        svg.circle=function(cx,cy,r) {
            return svg.el("circle").setAttribute("cx",cx).setAttribute("cy",cy).setAttribute("r",r);
        };
        function setStyle(e,style) {
            if (style) for (var x in style) e.setAttribute(x,style[x]);
            return e;
        }

        svg.g=function(style) {
            var e=setStyle(el("g"),style);

            dom.addList(e,arguments,1);
            return e;
        };
        svg.point=function(x,y) {
            var pt=svgElement.createSVGPoint();
            if (x) pt.x=x;
            if (y) pt.y=y;
            return pt;
        };
        svg.rotate=function(angle,cx,cy) {
            var tf=svgElement.createSVGTransform();
            tf.setRotate(angle,cx,cy);
            return tf;
        };




        svg.eventPoint=function(target,e) {
            //return svg.targetPoint(target,e.clientX,e.clientY);
            return svg.targetPoint(target,e.pageX,e.pageY);
        };

        svg.targetPoint=function(target,x,y) {
            return svg.point(x,y).matrixTransform(target.getScreenCTM().inverse());
        };

        svg.preTransform=function(target,xform) {
            var txf=target.transform.baseVal;
            //txf.insertItemBefore(xform,0);
            txf.appendItem(xform);
            //var m=xform.matrix;
            txf.consolidate();
            //logger.log(''+m.a+' '+m.b+' '+m.c+' '+m.d+' '+m.e+' '+m.f);
            return target;

        };

        svg.postTransform=function(target,xform) {
            var txf=target.transform.baseVal;
            //txf.insertItemBefore(xform,0);
            txf.insertItemBefore(xform,0);
            //var m=xform.matrix;
            txf.consolidate();
            //logger.log(''+m.a+' '+m.b+' '+m.c+' '+m.d+' '+m.e+' '+m.f);
            return target;

        };

        svg.setTransform=function(target,xform) {
            var txf=target.transform.baseVal;
            txf.clear();
            txf.appendItem(xform);            
            return target;

        };

        svg.scale=function(sx,sy) {
            var tf=svgElement.createSVGTransform();
            tf.setScale(sx,sy);
            return tf;
        };

        svg.translate=function(tx,ty) {
            var tf=svgElement.createSVGTransform();
            tf.setTranslate(tx,ty);
            return tf;
        };

        svg.zoom=function(zoom,cx,cy) {
            var mt=svgElement.createSVGMatrix();
            mt=mt.translate(cx,cy);
            mt=mt.scale(zoom);
            mt=mt.translate(-cx,-cy);
            //var m=mt;
            //logger.log('zoom '+zoom+' '+cx+','+cy+' '+m.a+' '+m.b+' '+m.c+' '+m.d+' '+m.e+' '+m.f);
            var tf=svgElement.createSVGTransform();
            tf.setMatrix(mt);


            return tf; 
        };

        svg.clearPath=function (path) {            
            path.pathSegList.clear();
        };
        svg.moveTo=function (path,x,y) {
            path.pathSegList.appendItem(path.createSVGPathSegMovetoAbs(x,y));
        };
        svg.quadTo=function (path,x,y,xc,yc) {
            path.pathSegList.appendItem(path.createSVGPathSegCurvetoQuadraticAbs(x,y,xc,yc));
        };
        svg.lineTo=function (path,x,y) {
            path.pathSegList.appendItem(path.createSVGPathSegLinetoAbs(x,y));
        };
        svg.cubicTo=function (path,x,y,xc1,yc1,xc2,yc2) {
            path.pathSegList.appendItem(path.createSVGPathSegCurvetoCubicAbs(x,y,xc1,yc1,xc2,yc2));
        };

        svg.PanAndZoom=function () {
            var canvas=svg.g();
            dom.addList(canvas,arguments,0);
            var background=svg.rect(0,0,2000,2000);            
            background.setAttribute('fill','#fff');

            var container=this.content=svg.g({},background,canvas);

            svg.enableDrag(this);

            var topLeft;
            var bottomRight;
            var width;
            var height;
            var determinant;


            function recalc() {
                var box=canvas.getBBox();
                var ctm=canvas.getTransformToElement(background);
                topLeft=svg.point(box.x,box.y).matrixTransform(ctm);
                bottomRight=svg.point(box.x+box.width,box.y+box.height).matrixTransform(ctm);
                width=svgElement.parentNode.offsetWidth;
                height=svgElement.parentNode.offsetHeight;
                determinant=ctm.a*ctm.d-ctm.b*ctm.c;
            }

            function zoomMinimum() {
                recalc();
                var zoomx=Math.min(1,width/(bottomRight.x-topLeft.x));
                var zoomy=Math.min(1,height/(bottomRight.y-topLeft.y));

                return Math.min(1/Math.sqrt(determinant),Math.min(zoomx,zoomy));
            }

            function zoomMaximum() {

                return 4/Math.sqrt(determinant);
            }


            function toZero(a,b) {
                if (a>0 && b>0) return Math.min(a,b);
                if (a<0 && b<0) return Math.max(a,b);
                return 0;
            }


            function check() {

                recalc();




                var dx=toZero(topLeft.x,bottomRight.x-width);
                var dy=toZero(topLeft.y,bottomRight.y-height);
                /*if (bottomRight.y>height) dy=Math.max(0,Math.min(topLeft.y,bottomRight.y-height));
                if (topLeft.y<0) dy=Math.min(0,Math.max(topLeft.y,bottomRight.y-height));*/
                /*dx+=Math.min(0,bottomRight.x-width);
                dy+=Math.min(0,bottomRight.y-height);
*/

                if (dx!=0 || dy!=0) svg.postTransform(canvas,svg.translate(-dx,-dy));



            }

            this.move=function(x,y) {
                var origin=this.getPoint();
                svg.postTransform(canvas,svg.translate(x-origin.x,y-origin.y));
                check();
            };

            this.getPoint=function() {
                
                return svg.point(0,0).matrixTransform(canvas.getTransformToElement(background));
                
            };

            


            dom.onmousescroll(container,function(e){
                var z=Math.pow(1.1,e.detail || (e.wheelDelta/40));

                z=Math.max(zoomMinimum(),z);
                z=Math.min(zoomMaximum(),z);

                var mouse=svg.eventPoint(canvas,e);
                //logger.log("zoom:"+mouse.x+","+mouse.y+","+z);
                svg.preTransform(canvas,svg.zoom(z,mouse.x,mouse.y));
                check();
                

                dom.stop(e);
            });




        };




        var offset;
        var dragging;
        var dragTarget;

        svg.enableDrag=function(target) {
            var element=target.content;
            dom.onmousedown(element,function(e) {

                offset=svg.eventPoint(element.parentNode,e);
                var origin=target.getPoint();
                offset.x=offset.x-origin.x;
                offset.y=offset.y-origin.y;
                dragging=target;
                dragTarget=element;
                dom.stop(e);
            });
            element.setAttribute('cursor','move');

        };


        dom.onmousemove(document,function(e) {
            if (!dragging) return;
            var mouse=svg.eventPoint(dragTarget.parentNode,e);
            dragging.move(mouse.x-offset.x,mouse.y-offset.y);
            dom.stop(e);
        });

        dom.onmouseup(document,function(e) {
            if (!dragging) return;
            if (dragging.finished) dragging.finished();
            dragging=null;
            
        });


        var svgElement=this.content=el('svg');
        dom.style(svgElement,style);
        svgElement.setAttribute('pointer-events','all');
        
        return svg;
    };


});