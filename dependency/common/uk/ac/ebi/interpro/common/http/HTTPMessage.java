package uk.ac.ebi.interpro.common.http;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.util.*;
import java.io.*;

public abstract class HTTPMessage {

    private static Location me = new Location();

    public String contentType;
    public List<HTTPHeader> headers = new ArrayList<HTTPHeader>();
    protected ByteArrayOutputStream body;

    public void setBody(InputStream is) throws IOException {
        body = new ByteArrayOutputStream();
        IOUtils.copy(is, body);
    }

    public void copyBody(OutputStream os) throws IOException {
        body.writeTo(os);
    }

    protected HTTPMessage() {

    }

    protected HTTPMessage(HTTPMessage o) {
        contentType = o.contentType;
        headers = o.headers;
        body = o.body;
    }


    public void log(String name) {
        for (HTTPHeader header : headers) {
            me.note("header " + header.name + ": " + header.value);
        }

    }

    public String getHeader(String name) {
        for (HTTPHeader header : headers) {
            if (header.name.equalsIgnoreCase(name)) return header.value;
        }
        return null;
    }


}
