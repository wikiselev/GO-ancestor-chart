package uk.ac.ebi.quickgo.web.configuration;

import org.w3c.dom.*;

import java.io.*;

import uk.ac.ebi.interpro.common.*;

public class Feedback {
    public File path;
    public String password;
    public Feedback(File base, Element elt) {
        path= IOUtils.relativeFile(base,elt.getAttribute("path"));
        password=elt.getAttribute("password");
    }
}
