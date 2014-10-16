package uk.ac.ebi.quickgo.web;

import javax.servlet.*;

public class QuickGOContextListener implements ServletContextListener {

    private static final String CONFIG_PARAMETER_NAME = "QuickGO-config-file";

    QuickGO quickGO;
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext ctx = servletContextEvent.getServletContext();
        quickGO = new QuickGO(ctx.getInitParameter(CONFIG_PARAMETER_NAME));
        ctx.setAttribute(QuickGO.class.getName(), quickGO);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        quickGO.close();
    }

    public synchronized static QuickGO get(ServletContext ctx) {
        return (QuickGO) ctx.getAttribute(QuickGO.class.getName());
    }
}
