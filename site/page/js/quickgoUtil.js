JSLIB.depend('quickgoUtil', ['jslib/dom.js'], function(dom) {
    var logger = this.logger;
	logger.log('LOADING quickgoUtil');

	var makeForm = this.makeForm = function(url, params, inputparams) {
		var form = dom.el('form');
		form.setAttribute('method', 'POST');
		form.setAttribute('action', url);

		// Params
		for (var p in params) {
			var field = dom.el('input');
			field.setAttribute('name', p);
			field.setAttribute('value', params[p] + '');
			field.setAttribute('type', 'hidden');
			dom.add(form, field);
		}

		// Extra Params
		if(inputparams){
			for (p in inputparams) {
				field = dom.el('input');
				field.setAttribute('name', inputparams[p].value);
				field.setAttribute('value', inputparams[p].checked + '');
				field.setAttribute('type', 'hidden');
				dom.add(form, field);
			}
		}
		
		return form;
	};

	var postRequest = this.postRequest = function(url, params, inputparams) {
		var form = makeForm(url, params, inputparams);
		dom.add(document.body, form);
		form.submit();
		dom.remove(form);
	};
	
	var superSplit = this.superSplit = function(s) {
	    s = s.replace(/^[, \t\n]+|[, \t\n]+$/g, '');
	    return (s == '') ? new Array() : s.split(/[, \t\n]+/);
	};
});