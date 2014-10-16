JSLIB.depend('table',['dom.js'],
        function(dom) {

    var table=this;

    var logger=this.logger;
    logger.log('Dynamic tables');



    this.DynamicTable=function DynamicTable(table) {

		//var showStyle=table.currentStyle?'block':'table-cell';
        var showStyle=window.getComputedStyle?'table-cell':'block';


        this.content=table;

        table.cellBorder='0';
        table.cellPadding='0';

        var headerRow=dom.find(table,'tr');

        var auto=true;

        function layout(setAuto) {
            if (auto==setAuto) return;
            auto=setAuto;

            //fixBox.checked=auto;

            var widths=[];
            var headerCells=dom.findAll(headerRow,'td');
            for (var i=0;i<headerCells.length;i++) {
                widths[i]=headerCells[i].offsetWidth;
                //logger.log(headerCells[i].name,widths[i]);
            }

            table.style.width = auto ? null : '0px';
            table.style.tableLayout = auto ? 'auto' : 'fixed';
            for (var j=0;j<headerCells.length;j++) {
                headerCells[j].style.width=auto? null:widths[j]+'px';
                
            }

        }

        
        var infoBox=dom.div();
        var columnInfo=dom.div();
        this.columnControl=dom.div(columnInfo,infoBox,dom.button(function(){layout(true);},'Automatic column widths'));

        var dragColumn;
        var targetColumn;


		var columns=[];


        function Column(headerCell) {

            var column=this;

            if (!headerCell.name) headerCell.name=headerCell.className;

            //logger.log('Column ',headerCell.name,headerCell.className);

            this.name=headerCell.name;

			this.show=true;


            function styleCells(name,value) {
				headerCell.style[name]=value;
            }

            
        
            var showHide=this.showHide=function(show) {
                if (setShow(show)) rearrange();
            }

            var setShow=this.setShow=function(show) {
				if (column.show==show) return false;
                //logger.log('Column',column.name,show);
                layout(true);
				dom.setCheckbox(checkbox,show);
				column.show=show;
                return true;
            }

            function select(selected) {
                styleCells("background",selected?'#ffc':null);
				styleCells("color",selected?'#000':null);
            }

            function moveColumn(other,auto) {
				if (other==column) return;
				var otherIndex=columns.indexOf(other);
				var index=columns.indexOf(column);
				//if (!auto || index>otherIndex) index++;

				//columns[otherIndex]=column;
				columns.splice(otherIndex,1);
				columns.splice(index,0,other);
				
				rearrange();
            }




            var checkbox = dom.checkbox(true);
            //showHide(checkbox.checked);
            dom.oncheckboxchange(checkbox,showHide);
            var colShow = dom.div(checkbox,dom.getInnerText(headerCell));
            //colShow.style.paddingRight='10px';
            columnInfo.appendChild(colShow);

			

            var anchor=dom.styleDiv({width:'20px',height:'1.5em',left:'-20px',position:'absolute',cursor:'col-resize'});

            var floating=dom.style(dom.floatDiv('right',anchor),{width:'0px',height:'0px',position:'relative',overflow:'visible'});
            
            //var content=dom.styleDiv({overflow:'hidden',cursor:'move',padding:'0 10px'});
            //dom.adoptChildren(headerCell,content);
            dom.insertBefore(headerCell,floating,headerCell.firstChild);
			//dom.add(headerCell,content);
			//headerCell.style.padding='0';
            

	

            function mousehandler(mover,finished) {
                select(true);
                var mup=dom.onmouseup(document.body,function(event){
                    mup.remove();
                    if (mover) mover.remove();
                    dom.stopDefault(event);
                    select(false);
                    dragColumn=null;
                    if (finished) finished();
                });
            }

            dom.onmouseover(headerCell,function(event){
                if (targetColumn==column) return;
                targetColumn=column;

                if (dragColumn) moveColumn(dragColumn,true);

                dom.stopPropagation(event);
            });

            dom.onmouseover(anchor,function(event){
				anchor.style.background='#ccc';
                //if (dragColumn) moveColumn(dragColumn,false);
                dom.stopPropagation(event);
            });

			dom.onmouseout(anchor,function(event){
                anchor.style.background='none';
                dom.stopPropagation(event);
            });

            dom.onmousemove(anchor,dom.stopDefault);

            dom.onmousedown(anchor,function(event){

                var width=headerCell.offsetWidth;

                var x=event.clientX-width;

                layout(false);
                //fixBox.checked=false;
                //fixBox.defaultChecked=false;
                mousehandler(dom.onmousemove(document.body,function(event){                    
                    
                    width=(event.clientX-x);
                    if (width<0) width=0;
                    headerCell.style.width=width+'px';
                    //logger.log('width '+width);
                    dom.stopDefault(event);
                }),function(event) {
                    if (width<20) {
                        showHide(false);
                        //checkbox.checked=false;
                        //checkbox.defaultChecked=false;
                        headerCell.style.width='20px';
						
                    }
                });
                dom.stopPropagation(event);
                dom.stop(event);
            });


			


            dom.onmousedown(headerCell,function(event){
                dragColumn=column;
                mousehandler(null,null);
                dom.stop(event);
            });

            

            

        }


        var headerCells=dom.findAll(headerRow,'td');
        for (var i=0;i<headerCells.length;i++) {
            columns.push(new Column(headerCells[i]))
        }

		rearrange();

		this.showHideColumns=function(columnInfo) {
            var needRearrange=false;
			for (var i=0;i<columns.length;i++) needRearrange|=columns[i].setShow(columnInfo[columns[i].name]);
            if (needRearrange) rearrange();
		};



        this.importRows=function(sourceTbody,targetTbody) {
			dom.adoptChildren(sourceTbody,targetTbody);

            rearrange();
        };

        this.importRow=function(sourceRow,targetTbody) {            
            this.importRows(sourceRow,[sourceRow],targetTbody);
        };

        function rearrange() {
            var rows=dom.findAll(table,'tr');

            for (var i=0;i<rows.length;i++) {
				//logger.log("Row:"+i);
                var cells=dom.findAll(rows[i],'td');
				var cellIndex={};
				for (var j=0;j<cells.length;j++) {
					cellIndex[cells[j].className]=cells[j];
					cells[j].style.display='none';
					//logger.log("cell:"+j+" "+cells[j]+" "+cells[j].getAttribute('class'));
				}

				//logger.log('sort');

                var colSpan=0;
                var prevCell;
                for (var j=0;j<columns.length;j++) {
					var cell=cellIndex[columns[j].name];
					//logger.log("cell:"+j+" "+cell+" "+columns[j].name);
                    if (cell) {
						if (columns[j].show) {
							cell.style.display=showStyle;
						}
						rows[i].appendChild(cell);
                        prevCell=cell;
                        colSpan=1;						
                    } else if (columns[j].show) {
						if (!prevCell) rows[i].appendChild(prevCell=dom.td());
						colSpan++;
						prevCell.colSpan=colSpan;
					}
                }

            }
        }
        
    };


    this.enhanceDynamic=function() {
        return function(node) {            
            new table.DynamicTable(dom.find(node,'table','dynamic'));
        };
    };
});

