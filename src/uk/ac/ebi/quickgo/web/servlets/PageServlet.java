package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.http.*;
import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.jxbp2.render.*;
import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.quickgo.web.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;

public class PageServlet implements Dispatchable {


    private static final String XHTML404 = "page/404.xhtml";



    public void process(Request r) throws IOException, ProcessingException {
        //r.write(respond(r,r.getPathInfo()));
        r.write(respond(r,"page/"+r.getPathInfo()));
    }



    private HTTPResponse respond(Request r,String page) throws IOException, ProcessingException {

        Renderer renderer = r.getRenderer();
        CachedFileData data= renderer.getFile(page);

        if (data.isFile) return data.checkNewResponse(ServletIO.read(r.request))? data.respond(): data.unmodified();

        CachedFileData xhtml=null;

        if (data.isDirectory && !page.endsWith("/")) {
            HTTPResponse response = new HTTPResponse(302);
            response.setHeader("Location",r.request.getRequestURI()+"/");
            return response;
        }

        if (page.endsWith("/") || page.length()==0) xhtml = renderer.getFile(page + "index.xhtml");

        if (page.endsWith(".html")) xhtml = renderer.getFile(page.substring(0, page.length() - 5) + ".xhtml");

        if (xhtml!=null && xhtml.isFile) return renderXHTML(r, xhtml);

        xhtml=renderer.getFile(XHTML404);

        HTTPResponse response;

        if (xhtml.isFile) response=renderXHTML(r, xhtml);
        else {
            response = new HTTPResponse("text/plain");
            response.setBody("404");
        }
        response.statusCode=404;
        return response;
    }

    /*private boolean is(Renderer renderer, String fileName) throws IOException {
        return renderer.getFile(renderer.getPageFile(fileName)).exists;
    }*/

    public class Snippet {
        public String id;

        public Snippet(String id) {
            this.id = id;
        }
    }

    private HTTPResponse renderXHTML(Request r, CachedFileData data) throws ProcessingException, IOException {
        if (!data.checkNewResponse(ServletIO.read(r.request))) {
	        return data.unmodified();
        }
		else {
	        HTTPResponse p;
	        String section = r.request.getParameter("section");
	        if (section != null) {
	            p = r.outputHTML(false).setPage(data.key).setStyle("style/snippet.xhtml").find("//div[@id='"+section+"']").render(new Snippet(section));
	        }
	        else {
		        p = r.outputHTML(!("raw".equals(r.request.getParameter("format")))).setPage(data.key).render();
	        }
	        data.setCacheControl(p);
	        return p;
        }
    }

}
