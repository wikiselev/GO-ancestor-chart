JSLIB.depend('hash',['parameters.js'],function(parameters) {

    var logger=this.logger;
    logger.log('Hash parameters');

    var hash = window.location.hash||'#';
    hash=(''+hash).substring(1);

    var values=parameters.parseKV(hash,'&','=');

    function regenerate() {
        hash='';
        for (var x in values) hash+='&'+encodeURIComponent(x)+'='+encodeURIComponent(values[x]);
        window.location.hash=hash.replace('&','#');
    }

    this.get=function(name) {return values[name];};
    this.set=function(name,value) {
        values[name]=value;
        regenerate();
    };
    this.remove=function(name) {
        delete values[name];
        regenerate();
    };

});
