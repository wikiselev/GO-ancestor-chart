JSLIB.depend('cookies',['parameters.js'],function(parameters) {

    var logger=this.logger;
    logger.log('Cookie reading and writing');
    var cookies=this;
    
    var values=parameters.parseKV(document.cookie,';','=');

    var get=this.get=function(name) {return values[name];};

    this.getStructured=function(name) {return parameters.parseKV(get(name),'|','=');};

    this.set=function(name,value,days,path) {
        var expires='';
        if (days) {
            var date = new Date();
            date.setTime(date.getTime()+(days*24*60*60*1000));
            expires = "; expires="+date.toGMTString();
        }
        if (!path) path='/';
        document.cookie = encodeURIComponent(name)+"="+encodeURIComponent(value)+expires+"; path="+path;

    };

    this.remove=function(name,path) {
        cookies.set(name,'',-1,path);
    };


});
