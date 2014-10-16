package uk.ac.ebi.interpro.webutil.tools;

import javax.servlet.http.*;


public class WebUtils {
    /**
     * Utility to retrieve a named cookie's value.
     *
     * @param req  Request containing cookie
     * @param name
     * @return Cookie value - or null
     */

    public static String getCookieValue(HttpServletRequest req, String name) {
        Cookie[] ck = req.getCookies();
        if (ck != null)
            for (Cookie c : ck) if (c.getName().equals(name)) return c.getValue();
        return null;

    }


}
