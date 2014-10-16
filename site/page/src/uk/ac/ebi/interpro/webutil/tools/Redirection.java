package uk.ac.ebi.interpro.webutil.tools;

import uk.ac.ebi.interpro.common.performance.*;

import javax.servlet.http.*;
import java.io.*;

@Deprecated
public class Redirection extends RCO implements Response {
    public String target;


    public void write(HttpServletResponse response) throws IOException {
        response.sendRedirect(target);
    }

    public int size() {
        return 0;
    }

    public boolean completed() {
        return true;
    }

    public Redirection(String target) {
        this.target = target;
    }
}
