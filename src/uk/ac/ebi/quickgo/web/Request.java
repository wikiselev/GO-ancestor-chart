package uk.ac.ebi.quickgo.web;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.interpro.common.http.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.jxbp2.render.*;
import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.interpro.webutil.tools.WebUtils;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.render.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

public class Request {
    private static Location me = new Location();

    private static final String QUICKGO_COOKIE_NAME = "quickgo";    
    private static final String PAGES_COOKIE_KEY = "p";

    public PerformanceMonitor monitor;
    public String id;
    public Map<String, String[]> parameterValues;

    public boolean debug() {
	    return monitor != null;
    }
    public long started;
    public String url;
    public String remoteHost;
    public String referrer;
    public String userAgent;
    public Features features;

    public String path;
    public String age() {
		return (System.currentTimeMillis() - started) + "ms";
    }

    public QuickGOMonitor.Status status = null;

    public QuickGO quickGO;

    private HttpServletResponse response;
    public HttpServletRequest request;
    public Configuration configuration;
    public DataFiles dataFiles;
    public List<Closeable> connection=new ArrayList<Closeable>();

    public boolean nodata() {
        return dataFiles == null;
    }

    public DataFiles getDataFiles() throws IOException, ProcessingException {
        if (dataFiles == null) {
	        write(outputHTML(true, "page/NoData.xhtml").render());
        }
        return dataFiles;
    }

    public Request(QuickGO quickGO, HttpServletRequest request, HttpServletResponse response, Configuration configuration, String path) {
        this.request = request;
        id = quickGO.unique();

        String qs = request.getQueryString();
        url = request.getRequestURI() + "?" + StringUtils.nvl(qs);
        started = System.currentTimeMillis();
        referrer = request.getHeader("Referer");
        userAgent = request.getHeader("User-Agent");
        String forwardedFor = request.getHeader("X-Forwarded-For");
        remoteHost = (forwardedFor != null ? forwardedFor + ", " : "") + request.getRemoteAddr();
        this.path = path;

        quickGO.monitor.start(this);

        //noinspection unchecked
        parameterValues = request.getParameterMap();

        if (request.getParameter("monitor") != null) {
            monitor = quickGO.monitor.performanceLog.start(id, "Request: " + url);
            quickGO.monitor.requestLog.add(monitor);
        }

        this.quickGO = quickGO;
        this.response = response;
        this.configuration = configuration;
        this.dataFiles = quickGO.dataManager.get();

        response.setHeader("X-QuickGO-request-id",id);

        cookie = WebUtils.getCookieValue(request, QUICKGO_COOKIE_NAME);
        if (cookie != null) {
	        //System.out.println("Request: cookie = " + cookie);
            for (String nameValue : cookie.split("_")) {
                String[] nv = nameValue.split("-");
                if (nv.length == 2) {
	                cookieInfo.put(nv[0], nv[1]);
                }
            }
        }
        
        String pages = getParameter("pages");
        if (pages != null) {
	        setCookieValue(PAGES_COOKIE_KEY, pages);
        }

        features = new Features(configuration.features);
    }

    public OutputStream outputData(String contentType,String filename) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-disposition", "attachment; filename="+filename);
        return response.getOutputStream();
    }

    public JSON outputJSON() throws IOException {
        return new JSON(getParameter("callback"));
    }

    public String getParameter(String name) {
        String[] vs = parameterValues.get(name);
        return (vs == null || vs.length < 1) ? null : vs[vs.length - 1];
    }

    public Map<String,String> getParameterMap() {
        return CollectionUtils.getFromParameterMap(parameterValues);
    }

    public String[] getParameterValues(String name) {
        return parameterValues.get(name);
    }

    public String getPathInfo() {
        return path;   
    }

    String cookie;
    Map<String, String> cookieInfo = new HashMap<String, String>();
    boolean sendCookie = false;

    public String getCookieValue(String part) {
        return cookieInfo.get(part);
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookieValue(String part, String value) {
        cookieInfo.put(part, value);
        sendCookie = true;
    }

    public String getHeader(String name) {
        return request.getHeader(name);
    }

    public Render outputHTML(boolean template, String name) throws IOException, ProcessingException {
        Render h = ("xml".equals(getParameter("embed"))) ? outputXML(name) : outputHTML(template);
        h.setPage(name);
        return h;
    }

    public Render outputHTML(boolean template) throws IOException, ProcessingException {
        Render h = getRenderer().getHTML().bind(this);
        if (template) {
	        h.setStyle(getRenderer().defaultStyle);
        }
        return h;
    }

    public Render outputXML(String name) throws IOException, ProcessingException {
        return getRenderer().getXML().bind(this).setPage(name);
    }

    public Render outputText(String mimeType, String name) throws IOException, ProcessingException {
        return getRenderer().getText(mimeType).bind(this).setPage(name);
    }

    public Renderer getRenderer() {
        String renderName = cookieInfo.get(PAGES_COOKIE_KEY);
        return (renderName != null && configuration.pages.renderers.containsKey(renderName)) ? configuration.pages.renderers.get(renderName) : configuration.pages.defaultRenderer;
    }

    public void close() {
        IOUtils.closeAll(connection);

        if (monitor != null) {
            quickGO.monitor.performanceLog.stop(monitor);
        }

        if (status == null) {
	        status = QuickGOMonitor.Status.GOOD;
        }
        
        quickGO.monitor.stop(this);
    }

    public void error(Exception e) {
        status = QuickGOMonitor.Status.FAILED;
        try {
            me.note("Failed", new ExceptionRecord(e));
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            PrintWriter wr = response.getWriter();
            ProcessingException.stackTrace(e, wr);
            //e.printStackTrace(wr);
            wr.flush();
        }
        catch (Exception e2) {
            System.out.println("Failed to write error " + e);
        }
    }

    public void plainTextError(String message) {
        status = QuickGOMonitor.Status.BADREQ;
        try {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain");
            Writer w = response.getWriter();
            w.write(message);
            w.flush();
        }
        catch (Exception e2) {
            //nevermind
        }
    }

    public void write(HTTPResponse page) throws IOException {
        Action a = me.start("Write client");
        if (sendCookie) {
            StringBuilder sb = new StringBuilder();
            for (String key : cookieInfo.keySet()) {
                if (sb.length() > 0) {
	                sb.append("_");
                }
                sb.append(key).append("-").append(cookieInfo.get(key));
            }
            Cookie cookie = new Cookie(QUICKGO_COOKIE_NAME, sb.toString());
            cookie.setMaxAge((int)(365 * Interval.DAY_NS/Interval.SECOND_NS));
            response.addCookie(cookie);     
        }

        try {
            ServletIO.write(response, page);            
        }
        catch (IOException e) {
            status = QuickGOMonitor.Status.CLIENTIO;
        }
        me.stop(a);
    }

	@Override
	public String toString() {
		String s = "";
		for (String k : parameterValues.keySet()) {
			String[] va = parameterValues.get(k);
			s = s + k + " = ";
			for (String v : va) {
				 s = s + v + " ";
			}
			s = s + "\n";
		}
		return "Request{" + s + '}';
	}
}
