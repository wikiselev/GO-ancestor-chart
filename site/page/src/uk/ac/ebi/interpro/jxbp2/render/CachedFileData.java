package uk.ac.ebi.interpro.jxbp2.render;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.http.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

/**
 * Cache of file contents and meta data, for HTTP delivery.
 *
 *
 * Browser behaviour:
 * Chrome5 ignores expires header if etag sent.
 * Chrome5, Firefox3.6 & IE6 will send if-none-match if an etag has been sent
 * Chrome5 & Firefox3.6 will send if-modified since if a last-modified has been sent
 * Firefox3.6 will ignore Expires if Vary: Cookie is sent
 * IE6 will ignore Expires and etag if Vary: Cookie is sent
 * F5/refresh causes a if-none-match/if-modified since on all browsers
 * Shift-F5 cause a fresh request on Chrome & Firefox
 * Ctrl-F5 does the same on IE6
 */

public class CachedFileData  {

    public static final DateFormat RFC1123=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");


    public final String key;
    public final boolean exists;
    public final boolean isFile;
    public final boolean isDirectory;
    public final long lastModified;
    public final String lastModifiedDate;
    public final byte[] content;
    public final String etag;
    public static Map<String,String> mimeTypes= CollectionUtils.arrayToMap(new String[][]{
                {"css","text/css"},
                {"gif","image/gif"},
                {"html","text/html"},
                {"htm","text/html"},            
                {"jpeg","image/jpeg"},
                {"js","text/javascript"},
                {"pdf","application/pdf"},
                {"png","image/png"},
                {"txt","text/plain"},
                {"xml","application/xml"},
                {"mp3","audio/mpeg3"},
                {"ogg","audio/ogg"}
    });

    CachedFileData(String key,long startTime) throws IOException {
        this.key = key;
        lastModified = startTime;
        content =null;
        etag="\""+Long.toHexString(lastModified+key.hashCode())+"\"";
        lastModifiedDate= RFC1123.format(new Date(lastModified));
        exists=false;
        isFile=false;
        isDirectory=false;
        touch();
    }


    CachedFileData(File f,String key,long startTime) throws IOException {

        this.key = key;
        exists=f.exists();
        isFile=f.isFile();
        isDirectory=f.isDirectory();
        lastModified=(f.lastModified()+startTime)/2;
        if (isFile) {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            InputStream is = new FileInputStream(f);
            IOUtils.copy(is, baos);
            is.close();
            content =baos.toByteArray();
        } else content =null;
        etag="\""+Long.toHexString(lastModified+f.getPath().hashCode())+"\"";
        lastModifiedDate= RFC1123.format(new Date(lastModified));
        touch();
    }

    CachedFileData(URL u,String key,long startTime) throws IOException {

        this.key = key;
        content = readIf(u);
        exists=content!=null;
        isFile=exists;
        isDirectory=false;
        lastModified=startTime;

        etag="\""+Long.toHexString(lastModified+key.hashCode())+"\"";
        lastModifiedDate= RFC1123.format(new Date(lastModified));
        touch();
    }



    private byte[] readIf(URL u) {
        try {
            return IOUtils.readBytes(u);
        } catch (IOException e) {
            return null;
        }
    }


    public String toString() {
        return key;
    }

    public String getType() {
        if (!exists) return "missing";
        if (isFile) return "file";
        if (isDirectory) return "directory";
        return "?";
    }



    public HTTPResponse respond() throws IOException {
        if (!exists) return new HTTPResponse(404,"text/html","<html><body><h1>404 Not found</h1></body></html>");
        
        String fileName = key;
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        String contentType = mimeTypes.get(extension);
        if (contentType==null) contentType="text/plain";
        HTTPResponse rp = new HTTPResponse(contentType);
        rp.setBody(content);
        setCacheControl(rp,600);

        return rp;
    }

    public void setCacheControl(HTTPResponse page) {
        long now=System.currentTimeMillis();
        page.setHeader("Last-Modified", lastModifiedDate);
        page.setHeader("Date",RFC1123.format(new Date(now)));
        page.setHeader("ETag", etag);
        page.setHeader("Vary","Cookie");        
    }

    public void setCacheControl(HTTPResponse page,int expires) {
        long now=System.currentTimeMillis();
        page.setHeader("Cache-Control","max-age="+expires);
        page.setHeader("Expires",RFC1123.format(new Date(now+expires*1000)));
        setCacheControl(page);        
    }


    public static boolean isNoneMatch(HTTPRequest r,String etag) {
        String ifNoneMatch = r.getHeader("If-None-Match");
        if (ifNoneMatch != null) {
            String[] etags = ifNoneMatch.split(",");
            for (String suppliedEtag : etags) {
                if (suppliedEtag.trim().equals(etag)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isModifiedSince(HTTPRequest r,String lastModifiedDate) {
        String ifModifiedSince = r.getHeader("If-Modified-Since");
        return ifModifiedSince == null || !lastModifiedDate.equals(ifModifiedSince);
    }

    public boolean checkNewResponse(HTTPRequest r) {
        return isNoneMatch(r,etag) && isModifiedSince(r,lastModifiedDate);
    }

    public HTTPResponse unmodified() {
        HTTPResponse p=new HTTPResponse(304);
        setCacheControl(p);
        return p;
    }

    long lastUsed;

    public CachedFileData touch() {
        lastUsed=System.currentTimeMillis();
        return this;
    }

    public long sinceLastUse() {
        return System.currentTimeMillis()-lastUsed;
    }

    public int size() {
        //1k approximate memory footprint for file caching overhead
        return 1024+(content==null?0:content.length);
    }

    public static Comparator<CachedFileData> lastUsedOrder=new Comparator<CachedFileData>() {
        public int compare(CachedFileData cfd1, CachedFileData cfd2) {
            return Long.signum(cfd1.sinceLastUse() - cfd2.sinceLastUse());
        }
    };


    public HTTPResponse checkRespond(HTTPRequest httpRequest) throws IOException {
        return checkNewResponse(httpRequest)?respond():unmodified();
    }
}