package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;

public class Debug extends HttpServlet {

/*
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        QuickGO quickGO=QuickGOContextListener.get(getServletContext());

        response.setContentType("text/html");
        PrintWriter wr = response.getWriter();
        wr.println(
                "<html><head><title>Peek</title><style>" +
                "body {font-family:sans-serif} " +
                ".popup {position:absolute;visibility:hidden;background:#fff;border:1px solid #000}" +
                "</style>" +
                "<script>" +
                "function expand(elt,name) {" +
                "var d=elt.parentNode.getElementsByTagName(name)[0];" +
                "if (d.style.visibility!='visible') d.style.visibility='visible';" +
                "else d.style.visibility='hidden';" +
                "}" +
                "</script>" +
                "<title>Performance</title></head>");

        String id = request.getParameter("id");

        quickGO.monitor.performance.get(id).performance.printActionHTML(wr);
        wr.println("</body></html>");

    }*/
}
