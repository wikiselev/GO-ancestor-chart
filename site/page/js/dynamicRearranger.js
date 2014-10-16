JSLIB.depend('dynamicRearranger', ['jslib/dom.js'], function(dom) {
	var logger = this.logger;
	logger.log('LOADING dynamicRearranger');

	this.DynamicRearranger = function DynamicRearranger() {
		var startDragElement;
		var startDragX, startDragY;

		var dragBegin = this.dragBegin = function(evt) {
			if (!evt) {
				evt = window.event;
			}
			startDragElement = this.parentNode;
			var parent = startDragElement.offsetParent;
			startDragX = startDragElement.offsetLeft - parent.offsetLeft - evt.clientX;
			startDragY = startDragElement.offsetTop - parent.offsetTop - evt.clientY;
			document.body.onmousemove = drag;
			document.body.onmouseup = dragEnd;
			
			return false;
		};

		function drag(evt) {
			if (!evt) {
				evt = window.event;
			}
			var newX = evt.clientX + startDragX;
			var newY = evt.clientY + startDragY;
			startDragElement.style.left = newX + "px";
			startDragElement.style.top = newY + "px";
			return false;
		}

		function dragEnd(evt) {
			dragMove(evt);
			document.body.onmousemove = null;
			document.body.onmouseup = null;
			return false;
		}

		var moveBegin = this.moveBegin = function(elt, evt) {
			startDragElement = elt;
			startDragElement.style.border = "1px dotted black";
			startDragElement.style.margin = "-1px";
			document.body.onmousemove = move;
			document.body.onmouseup = moveEnd;
			//logger.log("Dragging " + startDragElement.offsetLeft + "," + startDragElement.offsetTop);
			return false;
		};
		
		function move(evt) {
			var target;
			if (evt.target) {
				target = evt.target;
			}
			else {
				target = evt.srcElement;
			}

			while (target && target.parentNode!=startDragElement.parentNode) {
				target = target.parentNode;
			}

			if (!target || target == startDragElement) {
				return false;
			}

			if (target.offsetTop > startDragElement.offsetTop) {
				target = target.nextSibling;
			}

			if (target) {
				startDragElement.parentNode.insertBefore(startDragElement, target);
			}
			else {
				startDragElement.parentNode.appendChild(startDragElement);
			}

			return false;
		}
		
		function moveEnd(evt) {
			move(evt);
			startDragElement.style.border = "";
			startDragElement.style.margin = "";
			document.body.onmousemove = null;
			document.body.onmouseup = null;
			startDragElement.getElementsByTagName("input")[0].onchange();
			return false;
		}
	};
	
	this.afterDOMLoad(function(){});
});