package uk.ac.ebi.interpro.common.http;

import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;

public class HTTPResponse extends HTTPMessage {

    private static Location me = new Location();

    public int statusCode;

    public String message;

    private static final Charset UTF8 = Charset.forName("UTF-8");


    public HTTPResponse(Throwable error) {
        this(500,"text/plain");
        PrintWriter wr = new PrintWriter(getWriter());
        wr.println("Failed");
        error.printStackTrace(wr);
        wr.flush();
    }

    public HTTPResponse(int statusCode) {
        this.statusCode = statusCode;
    }

    public HTTPResponse(String contentType) {
        this(200,contentType);
    }

    public HTTPResponse(int statusCode,String contentType) {
        this.statusCode = statusCode;
        this.contentType=contentType;
    }

    public HTTPResponse(int statusCode,String contentType,String body) {
        this.statusCode = statusCode;
        this.contentType=contentType;
        setBody(body);
    }

    public Writer getWriter() {

        return new OutputStreamWriter(getOutputStream(), UTF8);
    }

    public OutputStream getOutputStream() {
        if (body==null) body=new ByteArrayOutputStream();
        return body;
    }

    public HTTPResponse(HTTPResponse o) {
        super(o);
        statusCode = o.statusCode;
        message = o.message;
    }

    public String toString() {
        return "HTTP-reply:[" + statusCode + "," + contentType + "," + (body == null ? "-" : body.size()) + "]";
    }

    public void log(String name) {
        //if (!StatisticsContext.enabled()) return;
        me.note(name, toString());
        super.log(name);
    }

    public void setHeader(String name, String value) {
        headers.add(new HTTPHeader(name,value));
    }

    public void setBody(byte[] content) {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        b.write(content,0,content.length);
        this.body=b;
    }

    public void setBody(String text) {
        ByteBuffer buff=UTF8.encode(text);
        byte[] body=new byte[buff.limit()];
        buff.get(body);
        setBody(body);
    }

    public boolean hasBody() {
        return body!=null;
    }

    public int getSize() {
        return body.size();
    }

    
}
