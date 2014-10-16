package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;

public class DispatcherServlet extends HttpServlet {

    QuickGO quickGO;

    private static final String CONFIG_PARAMETER_NAME = "QuickGO-config-file";

    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        quickGO=new QuickGO(ctx.getInitParameter(CONFIG_PARAMETER_NAME));

    }


    public void destroy() {
        quickGO.close();
    }

    


    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
       //System.out.println("Dispatch QuickGO: "+path+" "+request.getRequestURI());

        if (path ==null || path.length()==0) {
            response.sendRedirect(request.getContextPath()+request.getServletPath()+"/");
            return;
        }
        quickGO.dispatcher.dispatch(path.substring(1),request,response);
    }
}
