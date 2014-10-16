package uk.ac.ebi.quickgo.web;

import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.interpro.common.http.*;


import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.mortbay.jetty.handler.*;

public class Forwarder extends AbstractHandler {
    private URI proxyTarget;
    List<String> permittedHeaders= Arrays.asList("Cookie", "Content-length", "Host", "If-Modified-Since", "If-None-Match", "User-Agent", "Referer", "accept-charset", "accept-language", "accept");
    List<String> returnableHeaders= Arrays.asList("Last-Modified", "ETag", "Set-Cookie", "Location", /*"Content-Length",*/ "Date", "Pragma", "Server", "Cache-Control", "Encoding", "Content-disposition");

    public Forwarder(String proxyTarget) throws URISyntaxException {
        this.proxyTarget = new URI(proxyTarget);
    }


    public void handle(String target,HttpServletRequest request, HttpServletResponse response,int dispatch) throws IOException {
        HTTPRequest rq = ServletIO.read(request);
        try {
            URI uri = new URI(rq.url);
            rq.url=new URI(proxyTarget.getScheme(),null,proxyTarget.getHost(),proxyTarget.getPort(),uri.getPath(),uri.getQuery(),null).toString();
        } catch (URISyntaxException e) {IOException e2 = new IOException("Bad URI");e2.initCause(e);throw e2;}
        URLAdapter urlAdapter = new URLAdapter(10000, 10000);
        HTTPResponse rs = forward(urlAdapter, rq);
        ServletIO.write(response, rs);
    }

    private HTTPResponse forward(URLAdapter urlAdapter, HTTPRequest rq) throws IOException {
        
        for (Iterator<HTTPHeader> it = rq.headers.iterator(); it.hasNext();) {
            if (!permittedHeaders.contains(it.next().name)) it.remove();
        }

        HTTPResponse rs = urlAdapter.send(rq);

        for (Iterator<HTTPHeader> it = rs.headers.iterator(); it.hasNext();) {
            if (!permittedHeaders.contains(it.next().name)) it.remove();
        }

        return rs;
    }



}
