package uk.ac.ebi.interpro.common.http;

import uk.ac.ebi.interpro.common.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class URLAdapter implements HTTPAgent {
    private long connectTimeout;
    private long readTimeout;


    public URLAdapter(long connect, long read) {
        this.connectTimeout = connect;

        this.readTimeout = read;
    }

    public HTTPResponse send(HTTPRequest source) throws IOException {

        URL url = new URL(source.url);

        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setConnectTimeout((int) connectTimeout);
        urlc.setReadTimeout((int) readTimeout);
        urlc.setInstanceFollowRedirects(false);
        urlc.setRequestMethod(source.method);

        for (HTTPHeader header : source.headers) urlc.addRequestProperty(header.name, header.value);


        if (source.body != null) {
            urlc.setDoOutput(true);
            if (source.contentType == null)
                urlc.setRequestProperty("Content-type", "text/plain");
            OutputStream os = urlc.getOutputStream();
            source.body.writeTo(os);
            os.close();
        }

        InputStream is;

        try {
            is = urlc.getInputStream();
        } catch (IOException e) {
            is = urlc.getErrorStream();
        }


        HTTPResponse reply = new HTTPResponse(urlc.getResponseCode(),urlc.getContentType());

        reply.setBody(is);

        IOUtils.copy(is, reply.body);        
        reply.message = urlc.getResponseMessage();
        Map<String, List<String>> headerMap = urlc.getHeaderFields();
        for (String key : headerMap.keySet()) {
            if (key == null) continue;
            List<String> values = headerMap.get(key);
            for (String value : values) {
                reply.headers.add(new HTTPHeader(key, value));
            }
        }
        return reply;
    }

    public String toString() {
        return "URL timeouts - connect: " + Time.getTextFromMillis(connectTimeout) + " read: " + Time.getTextFromMillis(readTimeout);
    }
}
