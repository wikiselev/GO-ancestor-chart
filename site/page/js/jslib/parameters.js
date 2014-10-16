JSLIB.depend('parameters',[],function() {
    var logger = this.logger;
    logger.log('Query parameters');

    function trimDecode(input) {
        return decodeURIComponent(input.replace(/^ *| *$/g,'').replace('+',' '));
    }

    var parseKV = this.parseKV = function(input, separator, equals) {
        var results = {};

        function Value() {
            var values = [];
            var last;
            this.add = function (v) {
                values.push(v);
                last = v;
            };

            this.toString = function() {
	            return last;
            };

            this.all = function() {
                return values;
            };

            this.concat = function(sep) {
                return values.join(sep||',');
            };
        }

        var ca = input.split(separator);
        for (var i = 0, n = ca.length; i < n; i++) {
            var c = ca[i];
            var p = c.indexOf(equals);
            if (p < 0) {
	            p = c.length;
            }
            var name = trimDecode(c.substring(0, p));
            var value = trimDecode((p < c.length) ? c.substring(p + 1) : '');
            if (name.length == 0) {
	            continue;
            }

            results[name] = results[name]||new Value();
            results[name].add(value);
        }

        return results;
    };

    this.parseParameters = function(v) {
        return parseKV(v, '&', '=');
    };

    this.parseHREF = function(href) {
        var s = href.match(/([^?]*)(\?(.*))?/);
        var kv = parseKV(s[3]||'', '&', '=');
        kv[''] = s[1];
        return kv;
    };

    var search = (window.location.search||'?').substring(1);

    var values = this.parseParameters(search);

    this.get = function(name) {
	    return values[name];
    };

	// helper functions for base64 conversion of (positive) integers
    var indexTable = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz[]';
	var sentinel = '64$';

	function convert10to64(decimalInt) {
		var s = '';

		while (decimalInt > 0) {
			s = indexTable.charAt(decimalInt % 64) + s;
			decimalInt = Math.floor(decimalInt / 64);
		}
		return s;
	}

	function convert64to10(num) {
		var ret = 0;

		for (var factor = 1; num.length > 0; factor *= 64) {
			ret += indexTable.indexOf(num.charAt(num.length - 1)) * factor;
			num = num.substr(0, num.length - 1);
		}

		return ret;
	}

	// Base 64 compress the GO ids, left padded to 4 characters and concatenated together, and with a sentinel string at the front
    var compressTerms = this.compressTerms = function(ids) {
        var compressed = '';
        for (var i = 0; i < ids.length; i++) {
            //logger.log('compressTerms: ids[' + i + '] = ' + ids[i]);
	        compressed += convert10to64(ids[i].substring(3) >> 0).leftPad(4, '0');
        }
        return compressed.length > 0 ? sentinel + compressed : '';
    };

	var decompressTerms = this.decompressTerms = function(compressed) {
		var ids = [];
		var ix = 0;

		if (compressed && compressed.length > 0) {
			if (compressed.substr(0, sentinel.length) == sentinel) {
				// base64 encoding
				for (ix = sentinel.length; ix < compressed.length; ix += 4) {
					ids.push(convert64to10(compressed.substring(ix, ix + 4)));
				}
			}
			else {
				// assume old-style base36
				for (ix = 0; ix < compressed.length; ix += 4) {
					ids.push(parseInt(compressed.substring(ix, ix + 4), 36));
				}
			}
		}
		return ids;
	};

	var compressedTermCount = this.compressedTermCount = function(compressed) {
		if (compressed && compressed.length > 0) {
			return (compressed.substr(0, sentinel.length) == sentinel) ? (compressed.length - sentinel.length) / 4 : compressed.length / 4;
		}
		else {
			return 0;
		}
	};
});