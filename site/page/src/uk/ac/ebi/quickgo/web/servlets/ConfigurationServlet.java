package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.webutil.tools.WebUtils;
import uk.ac.ebi.interpro.jxbp2.render.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class ConfigurationServlet {
    enum ConfigurationAction {
        Home, Reload, Flush, MonitorInfo, RendererInfo, Yoyo, UpdateSuspend, UpdateResume, UpdateNow, Quit
    }

    QuickGO quickGO;

    DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm:ss");

    public ConfigurationServlet(QuickGO quickGO) {
        this.quickGO = quickGO;
    }

    protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ConfigurationAction action = CollectionUtils.enumFind(request.getParameter("action"), ConfigurationAction.Home);

        Configuration cfg = quickGO.active;

        response.setContentType("text/html");
        PrintWriter wr = response.getWriter();
        printHeader(wr, quickGO.hostName);
        wr.println("<h1>QuickGO Configuration (" + quickGO.hostName + ")</h3>");

        String password = request.getParameter("password");

        if (password != null) {
            Cookie cookie = new Cookie("quickgo_admin", request.getParameter("password"));
            cookie.setMaxAge((int) (365 * Interval.DAY_NS/Interval.SECOND_NS));
            response.addCookie(cookie);
        }
        else {
	        password = WebUtils.getCookieValue(request, "quickgo_admin");
        }
        String requiredPassword = cfg.password;
        if (requiredPassword != null && !requiredPassword.equals(password)) {
            wr.println("<form method='POST'><input type='password' name='password'/></form>");
        }
        else {
            authorized(wr, request, action, cfg);
        }

        wr.println("</body></html>");
    }

    private void authorized(PrintWriter wr, HttpServletRequest request, ConfigurationAction action, Configuration cfg) {
        switch (action) {
        case Yoyo:
	        yoyo(request.getParameter("what"));
	        break;
        case Flush:
	        flush(wr, cfg);
	        break;
        case Home:
	        home(wr, cfg);
	        break;
        case MonitorInfo:
	        monitorInfo(wr, quickGO.monitor, request.getParameter("id"));
	        break;
        case UpdateSuspend:
	        quickGO.updateSchedule.suspend();
	        ok(wr, "Suspend Update");
	        break;
        case UpdateResume:
	        quickGO.updateSchedule.resume();
	        ok(wr, "Resume Update");
	        break;
        case UpdateNow:
	        quickGO.updateSchedule.now();
	        ok(wr, "Update running shortly");
	        break;
        case Reload:
	        reload(wr);
	        break;
        case RendererInfo:
	        rendererInfo(wr, cfg.pages, request.getParameter("name"));
	        break;
        case Quit:
	        quickGO.quit();
	        ok(wr, "That's all, folks!");
	        break;
        }
    }

    public void ok(PrintWriter wr, String message) {
        wr.println(message);
    }

    private void yoyo(String what) {
        quickGO.down = what.equals("down");
    }

    private void home(PrintWriter wr, Configuration cfg) {
        wr.println("<div>" +
                "<a class='button' href='Configure?action=" + ConfigurationAction.Reload + "'>Reload configuration</a> | " +
                "<a href='Configure?action=" + ConfigurationAction.Quit + "'>quit</a>" +
                "</div>");

        wr.println("<div>QuickGO <a href='check'>status flag</a>: " + (quickGO.down ? "down":"up") +
                " <a href='Configure?action=Yoyo&what=up'>up</a> | <a href='Configure?action=Yoyo&what=down'>down</a>");

        wr.println("<div>" + quickGO.hostName + "</div>");

        monitorStatus(wr, quickGO.monitor);

        updateConfig(wr, quickGO.updateSchedule);
        dataConfig(wr, quickGO.dataManager);

        if (cfg == null) {
            wr.println("<div>No configuration available</div>");
        }
        else {
            configInfo(wr, cfg);

			if (cfg.pages != null) {
				pagesConfig(wr, cfg.pages);
			}

			if (cfg.features != null) {
				featuresConfig(wr, cfg.features);
			}

            if (cfg.failure != null) {
                wr.println("<h1>Configuration Failed</h1>");
                wr.println("<pre>");
                cfg.failure.printStackTrace(wr);
                wr.println("</pre>");
            }
        }
    }

    private void reload(PrintWriter wr) {
        wr.println("<h2>Reloading</h2>");
        Configuration alt = quickGO.reload();
        if (alt.failed()) {
            wr.println("<div>Failed</div><pre>");
            alt.failure.printStackTrace(wr);
            wr.println("</pre>");
        }
	    else {
	        wr.println("<div>Succeeded</div>");
        }
    }

    private static void printHeader(PrintWriter wr, String hostName) {
        wr.println("<html><head><title>QuickGO Configuration - " + hostName + "</title>");
        wr.println("<style>\n" +
                "body {font-family:sans-serif}\n" +
                Action.actionStyle +
                "</style>\n" +
                "<script>\n" +
                Action.actionScript +
                "</script></head><body>");
    }

    private void configInfo(PrintWriter wr, Configuration cfg) {
        wr.println("<h1>Configuration</h1>");
       wr.println("<div>Configuration loaded: " + df.format(new Date(cfg.loaded)) + " file: " + df.format(new Date(cfg.lastModified)) + "</div>");
    }

    private void monitorInfo(PrintWriter wr,QuickGOMonitor monitor,String requestID) {
        wr.println("<ul>");
        monitor.performanceLog.archive.get(requestID).root.printActionHTML(wr);
        wr.println("</ul>");
    }

    public static final NumberFormat nfbig = new DecimalFormat("###,###,###");

    private void updateConfig(PrintWriter wr, UpdateSchedule schedule) {
        wr.println("<h1>Update</h1>");
        wr.println("<table>");
        wr.println("<tr><td>Update check:</td><td>");
        if (schedule.autoUpdateConfigured()) {
            wr.println(schedule.updateEnabled() ?
                    " <a href='Configure?action=" + ConfigurationAction.UpdateSuspend + "'>Suspend</a> ":
                    " <a href='Configure?action=" + ConfigurationAction.UpdateResume + "'>Resume</a> ");
        }
        wr.println(" <a href='Configure?action=" + ConfigurationAction.UpdateNow + "'>Now</a> " +
                "</td></tr>");
        wr.println("<tr><td>Interval:</td><td>" + schedule.getUpdateInterval() + "</td></tr>");
        wr.println("</table>");
    }

    private void dataConfig(PrintWriter wr, DataManager data) {
        wr.println("<h1>Data</h1>");
        wr.println("<div>Local installation base: " + data.getBase() + "</div>");
        wr.println("<div>Remote update status: " + data.getUpdate() + "</div>");
        wr.println("<div>Local Status: " + data.getLocalStatus() + "</div>");
        wr.println("<div>Remote Status: " + data.getRemoteStatus() + "</div>");
        wr.println("<div>Memory used: " + data.maxUsedMemory + "</div>");
        wr.println("<div>Last update disk space required: " + data.diskSpaceRequired + " free: " + data.diskSpaceAvailable + "</div>");
        wr.println("<div>Last checked: " + data.since() + "</div>");
        wr.println("<div><a href='wait'>Wait for data</a></a>");
        DataFiles files = data.get();
        if (files != null) {
	        wr.println("<div>Current data stamp: "+ files.stamp + "</div>");
        }
        if (data.failure != null) {
            wr.println("<pre>");
            data.failure.printStackTrace(wr);
            wr.println("</pre>");
        }
        wr.println("<h3>Latest check</h3>");
        performanceList(wr, Collections.singletonList(data.recentCheck));
        wr.println("<h3>Recent updates</h3>");
        performanceList(wr, data.updateLog);
    }

    private void monitorStatus(PrintWriter wr, QuickGOMonitor monitor) {
        wr.println("<h1>Monitor</h1>");

        synchronized(monitor) {
            wr.println("<table>");
            wr.println("<tr><td>Free memory</td><td>" + nfbig.format(monitor.freeMemory) + "</td></tr>");
            wr.println("<tr><td>Total memory</td><td>" + nfbig.format(monitor.totalMemory) + "</td></tr>");
            wr.println("<tr><td>Garbage collect time</td><td>" + monitor.gcTime + "ms</td></tr>");
            wr.println("<tr><td>Garbage collect memory</td><td>" + nfbig.format(monitor.memoryDelta) + "</td></tr>");
            wr.println("</table>");
        }

        wr.println("<div>"+"</div>");
        wr.println("<table>");
        wr.println("<tr><td>Still running</td><td>Failed</td><td>Partial</td><td>Good</td></tr>");
        wr.println("<tr><td>" + monitor.getStillRunning() + "</td><td>" + monitor.getStatusCount(QuickGOMonitor.Status.FAILED) + "</td><td>" + monitor.getStatusCount(QuickGOMonitor.Status.PARTIAL) + "</td><td>" + monitor.getStatusCount(QuickGOMonitor.Status.GOOD) + "</td></tr>");
        wr.println("</table>");
        wr.println("<h3>Current requests</h3>");
        long now = System.currentTimeMillis();
        wr.println("<table>");
        wr.println("<tr><td>Duration (s)</td><td>URL</td></tr>");
        for (Request req : monitor.inprogress) {
            wr.println("<tr><td>" + (now - req.started) / 1000 + "</td><td>" + req.url + "</td></tr>");
        }
        wr.println("</table>");

        wr.println("<h3>Recently recorded</h3>");

        performanceList(wr, monitor.requestLog);
    }

    private void performanceList(PrintWriter wr, List<PerformanceMonitor> list) {
        wr.println("<table>");
        wr.println("<tr><td>When</td><td>Action</td></tr>");

        for (PerformanceMonitor performance : list) {
            wr.println("<tr><td><a href='Configure?action=" + ConfigurationAction.MonitorInfo + "&amp;id=" + performance.id + "'>" +
                    df.format(new Date(performance.root.when)) + "</a></td><td>" + performance.name + "</td></tr>");
        }
        wr.println("</table>");
    }

    private void flush(PrintWriter wr, Configuration cfg) {
        for (Renderer r : cfg.pages.renderers.values()) {
            r.flush();
        }
        wr.println("Flushed @ " + df.format(new Date()));
    }

    private void pagesConfig(PrintWriter wr,Pages pages) {
        wr.println("<h1>Pages</h1>");

        wr.println("<table>");

        wr.println("<tr><td>Name</td><td>Pages</td><td>Templates</td><td>Default</td></tr>");
        for (String name : pages.renderers.keySet()) {            
            Renderer r = pages.renderers.get(name);
            wr.println("<tr><td><a href='?action=" + ConfigurationAction.RendererInfo + "&amp;name=" + name + "'>" + name + "</a></td>"+/*"<td>" + r.pageRoot+"</td><td>" + r.templateRoot + "</td><td>" + r.defaultStyle + "</td>" + */"</tr>");
        }

        wr.println("</table>");
        wr.println("<a class='button' href='Configure?action=" + ConfigurationAction.Flush + "'>Flush page cache</a>");
    }

    public void rendererInfo(PrintWriter wr, Pages pages, String name) {
        wr.println("<h3>Files</h3>");
        Map<String, CachedFileData> fileArchive = pages.renderers.get(name).files.files;
        wr.println("<table>");
        synchronized(fileArchive) {
            List<CachedFileData> all = new ArrayList<CachedFileData>(fileArchive.values());
            Collections.sort(all, CachedFileData.lastUsedOrder);
            for (String file : fileArchive.keySet()) {
                CachedFileData cfd = fileArchive.get(file);
                wr.println("<tr><td>" + file + "</td><td>" + cfd.size() + "</td><td>" + cfd.getType() + "</td><td>" + cfd.sinceLastUse() + "</td></tr>");
            }
        }
        wr.println("</table>");
        wr.println("<h3>Documents</h3>");
        wr.println("<table>");

        Map<String, List<DocumentCache.CachedDocument>> documentArchive = pages.renderers.get(name).documents.documentArchive;
        synchronized(documentArchive) {
            for (String file : documentArchive.keySet()) {
                wr.println("<tr><td>" + documentArchive.get(file).size() + "</td><td>" + file + "</td></tr>");
            }
        }
        wr.println("</table>");
        wr.println("<h3>Widgets</h3>");
        wr.println("<table>");

        Map<String, List<WidgetCache.CachedWidgets>> widgetArchive = pages.renderers.get(name).widgets.widgetArchive;
        synchronized(widgetArchive) {
            for (String file : widgetArchive.keySet()) {
                wr.println("<tr><td>" + widgetArchive.get(file).size() + "</td><td>" + file + "</td></tr>");
            }
        }
        wr.println("</table>");
    }

    void featuresConfig(PrintWriter wr, Features features) {
        wr.println("<h1>Features</h1>");
        wr.println("<table>");
        for (Features.Name name : Features.Name.values()) {
            wr.println("<tr><td>" + name + "</td><td>" + features.values.get(name) + "</td></tr>");
        }
        wr.println("<table>");
    }
}
