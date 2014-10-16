package uk.ac.ebi.interpro.webutil.tools;

import uk.ac.ebi.interpro.common.http.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;

public class ServletIO {
    public static HTTPRequest read(HttpServletRequest req) throws IOException {
        HTTPRequest message = new HTTPRequest();
        Enumeration en = req.getHeaderNames();
        message.contentType = null;
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            Enumeration values = req.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                message.headers.add(new HTTPHeader(name, value));
            }

        }
        String qs = req.getQueryString();
        message.url = req.getRequestURI()+(qs==null?"":"?"+ qs);
        message.contentType = req.getContentType();
        message.method = req.getMethod();
        if (message.method.equals("POST")) message.setBody(req.getInputStream());
        return message;
    }

    public static void write(HttpServletResponse res, HTTPResponse message) throws IOException {
        if (message.message == null) res.setStatus(message.statusCode);
        else res.setStatus(message.statusCode, message.message);
        res.setContentType(message.contentType);
        if (message.hasBody()) res.setContentLength(message.getSize());
        for (HTTPHeader header : message.headers) res.addHeader(header.name, header.value);
        if (message.hasBody()) {
            ServletOutputStream os = res.getOutputStream();
            message.copyBody(os);
        }
    }
}
