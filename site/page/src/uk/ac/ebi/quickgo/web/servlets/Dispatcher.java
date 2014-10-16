package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.*;
import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.common.http.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.Calendar;
import java.util.Date;
import uk.ac.ebi.interpro.jxbp2.render.CachedFileData;

public class Dispatcher {
    QuickGO quickGO;

    public Dispatcher(QuickGO quickGO) {
        this.quickGO = quickGO;
        configure = new ConfigurationServlet(this.quickGO);
    }

    ConfigurationServlet configure;
    GAnnotationServlet annotation = new GAnnotationServlet();
    GSearchServlet search = new GSearchServlet(this);
    GTermServlet term = new GTermServlet();
    Selection selection = new Selection();
    ImageServlet image = new ImageServlet();
    PageServlet page = new PageServlet();
    GMultiTermServlet multiTerm = new GMultiTermServlet(this);
    GMultiProteinServlet multiProtein = new GMultiProteinServlet();
    GProteinServlet protein = new GProteinServlet();
	GProteinSetServlet proteinSet = new GProteinSetServlet();
	GHistoryServlet history = new GHistoryServlet();
	GValidationServlet validator = new GValidationServlet();
    Describe describe = new Describe();
    GFeedback feedback = new GFeedback();
    Fail fail = new Fail();
    CacheTest cacheTest = new CacheTest();

    class Fail implements Dispatchable {
        public void process(Request r) throws Exception {
            r.write(new HTTPResponse(Integer.parseInt(r.getParameter("code"))));
        }
    }

    class CacheTest implements Dispatchable {
        public void process(Request r) throws Exception {
            HTTPRequest rq=ServletIO.read(r.request);

            if (r.getParameter("dump") != null) {
                for (HTTPHeader header : rq.headers) {
                    System.out.println(header.name + ":" + header.value);
                }
            }

            String format = r.getParameter("format");
            String expiry = r.getParameter("expiry");
            String etag = r.getParameter("etag");
            String modified = r.getParameter("modified");
            String vary = r.getParameter("vary");
            String value = r.getParameter("cookie");
            String cookie = r.getCookie();
            if (value != null) {
	            r.setCookieValue("x", value);
            }

            if ("now".equals(etag)) {
	            etag = CachedFileData.RFC1123.format(new Date());
            }
            if ("now".equals(modified)) {
	            modified = CachedFileData.RFC1123.format(new Date());
            }
            if ("".equals(etag)) {
	            etag = "X1";
            }
            if ("".equals(modified)) {
	            Calendar cal = Calendar.getInstance();
	            cal.set(1977, 10, 28);
	            modified = CachedFileData.RFC1123.format(cal.getTime());
            }

            String content = "cookie:" + cookie + " date:" + new Date() + " input:" + r.getParameter("input") + " etag:" + etag + " modified:" + modified;
            if ("js".equals(format)) {
	            content = "document.write('" + content + "');\n";
            }
            else {
	            content = "<html><body>\n" + content + "\n</body></html>";
            }
            HTTPResponse response = new HTTPResponse(200, "js".equals(format) ? "text/javascript" : "text/html", content);
            if (expiry != null) {
                int expires = Integer.parseInt(expiry);
                long now = System.currentTimeMillis();
                response.setHeader("Cache-Control", "max-age=" + expires);
                response.setHeader("Expires", CachedFileData.RFC1123.format(new Date(now+expires*1000)));
                response.setHeader("Date", CachedFileData.RFC1123.format(new Date(System.currentTimeMillis())));
            }
            if (vary != null) {
                response.setHeader("Vary", vary);
                
            }
            if (etag != null) {
                response.setHeader("ETag", etag);
                if (!CachedFileData.isNoneMatch(rq, etag)) {
	                response = new HTTPResponse(304);
                }
            }
            if (modified != null) {
                response.setHeader("Last-Modified", modified);
                if (!CachedFileData.isModifiedSince(rq, modified)) {
	                response = new HTTPResponse(304);
                }
            }

            System.out.println("Request " + r.request.getQueryString() + " cookie:" + cookie + " if-none-match:" + r.getHeader("If-None-Match") + " if-modified-since:" + r.getHeader("If-Modified-Since") + " status:" + response.statusCode);

            r.write(response);
        }
    }


    public void dispatch(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (uri.equals("Configure")) {
            configure.process(request, response);
            return;
        }

        if (uri.equals("check")) {
            check(request, response);
            return;
        }

        if (uri.equals("wait")) {
            waitForData(request, response);
            return;
        }

        Configuration cfg = quickGO.getConfiguration();
        if (cfg.failed()) {
            response.setContentType("text/html");
            PrintWriter wr = response.getWriter();
            wr.println("<html><head><title>QuickGO is not configured</title></head>" +
                    "<body><a href='Configure'>Configure</a></body></html>");
            wr.close();
            return;
        }

        MemoryMonitor memoryMonitor = null;
        if (request.getParameter("memory") != null) {
	        memoryMonitor = new MemoryMonitor(true);
        }

        Request r = new Request(quickGO, request, response, cfg, uri);
        try {
            if (uri.equals("gohierarchy")) {
	            redirect(r, "GMultiTerm?format=image&id=" + r.getParameter("code"));
            }
            else if (uri.equals("DisplayGoTerm")) {
	            redirect(r, "GTerm?id=" + r.getParameter("id"));
            }
            else if (uri.equals("QuickGO")) {
	            redirect(r, "GTerm?id=" + r.getParameter("entry"));
            }
            else if (uri.equals("GAnnotation")) {
	            annotation.process(r);
            }
            else if (uri.equals("GSearch")) {
	            search.process(r);
            }
            else if (uri.equals("GTerm")) {
	            term.process(r);
            }
            else if (uri.equals("GProtein")) {
	            protein.process(r);
            }
            else if (uri.equals("GMultiProtein")) {
	            multiProtein.process(r);
            }
            else if (uri.equals("GMultiTerm")) {
	            multiTerm.process(r);
            }
            else if (uri.equals("GProteinSet")) {
	            proteinSet.process(r);
            }
            else if (uri.equals("Selection")) {
	            selection.process(r);
            }
            else if (uri.equals("GHistory")) {
	            history.process(r);
            }
            else if (uri.equals("GValidate")) {
	            validator.process(r);
            }
            else if (uri.equals("IS")) {
	            image.process(r);
            }
            else if (uri.equals("Describe")) {
	            describe.process(r);
            }
            else if (uri.equals("GFeedback")) {
	            feedback.process(r);
            }
            else if (uri.equals("GSearch")) {
	            search.process(r);
            }
            else if (uri.equals("fail")) {
	            fail.process(r);
            }
            else if (uri.equals("cachetest")) {
	            cacheTest.process(r);
            }
            else {
	            page.process(r);
            }
        }
        catch (Exception e) {
            r.error(e);
        }
        finally {
            r.close();
        }

        if (memoryMonitor != null) {
            System.out.println("Memory monitor: " + memoryMonitor.end());
            System.out.println("->" + memoryMonitor);
        }
    }

    void check(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter w = response.getWriter();
        DataFiles df = quickGO.dataManager.get();
        w.println((df == null || quickGO.down) ? "//started" : "//alive");
        w.close();
    }

    void redirect(Request r,String destination) throws IOException {
        HTTPResponse response = new HTTPResponse(302);
        response.setHeader("Location", destination);
        r.write(response);
    }

    void waitForData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        PrintWriter wr = response.getWriter();
        wr.print(quickGO.dataManager.waitForData() ? "good" : "bad");
    }
}
