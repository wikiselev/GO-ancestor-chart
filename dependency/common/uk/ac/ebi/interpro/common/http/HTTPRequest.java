package uk.ac.ebi.interpro.common.http;

import uk.ac.ebi.interpro.common.performance.*;

public class HTTPRequest extends HTTPMessage {

    private static Location me = new Location();

    public String method;
    public String url;

    public HTTPRequest() {
    }

    public HTTPRequest(HTTPRequest o) {
        super(o);

        method = o.method;
        url = o.url;

    }

    public String toString() {
        return "HTTP-request:[" + method + "," + url + "," + (body == null ? "-" : body.size()) + "]";
    }

    public void log(String name) {
        //if (!StatisticsContext.enabled()) return;
        me.note(name, toString());
        super.log(name);
    }


}
