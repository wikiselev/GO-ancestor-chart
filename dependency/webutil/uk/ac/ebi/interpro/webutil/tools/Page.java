package uk.ac.ebi.interpro.webutil.tools;

import uk.ac.ebi.interpro.common.performance.*;

import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

@Deprecated
public class Page extends RCO implements Response {

    byte[] data;
    String contentType;
    OutputStreamWriter wr;
    ByteArrayOutputStream baos;
    int status= HttpServletResponse.SC_OK;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    Map<String,String> headers=new HashMap<String, String>();


    public Page setStatus(int status) {
        this.status=status;
        return this;
    }

    public Writer getWriter() {
        return wr = new OutputStreamWriter(getOutputStream(), UTF8);
    }

    public ByteArrayOutputStream getOutputStream() {return baos = new ByteArrayOutputStream();}

    public void finish() {
        if (wr != null) try {wr.flush();} catch (IOException e) {}
        if (baos != null) data = baos.toByteArray();
        baos=null;
        wr=null;
    }

    public void write(HttpServletResponse response) throws IOException {
        if (baos!=null) throw new IOException("Page rendering not finished");
        response.setStatus(status);
        for (String name : headers.keySet()) {
            response.setHeader(name,headers.get(name));
        }
        if (data!=null) {
            response.setContentType(contentType);
            response.getOutputStream().write(data);
        }

    }
    public int size() {
        return data==null?0:data.length;
    }

    public Page(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;

    }


    public Page(String contentType) {
        this.contentType = contentType;
    }

    public Page(String contentType, String content, int code) throws UnsupportedEncodingException {
        this.contentType = contentType;
        data=content.getBytes("UTF8");
        setStatus(code);
    }

    public Page(int code) {
        setStatus(code);
    }



    public Page(Exception e) {
        this.contentType = "text/plain";
        e.printStackTrace(new PrintWriter(getWriter()));
        finish();
    }

    public boolean completed() {
        return data!=null;
    }

    public void setHeader(String name, String value) {
        headers.put(name,value);
    }
}
