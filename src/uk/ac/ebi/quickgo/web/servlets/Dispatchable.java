package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.quickgo.web.*;

public interface Dispatchable {
    public void process(Request r) throws Exception;
}
