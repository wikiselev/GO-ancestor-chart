package uk.ac.ebi.quickgo.web.servlets;

import java.io.IOException;
import javax.servlet.*;

public class HealthFilter
    implements Filter
{

    public HealthFilter()
    {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        request.setAttribute("health", Boolean.valueOf(true));
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterconfig)
        throws ServletException
    {
    }

    public void destroy()
    {
    }
}