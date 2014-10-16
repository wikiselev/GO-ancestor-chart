package uk.ac.ebi.quickgo.web.configuration;

import org.w3c.dom.*;
import uk.ac.ebi.interpro.common.xml.XMLUtils;
import uk.ac.ebi.interpro.jxbp2.render.*;

import java.io.*;
import java.util.*;

public class Pages {

    public Map<String, Renderer> renderers=new HashMap<String, Renderer>();


    public Renderer defaultRenderer;

    public Pages(File base,Element definition) {
        List<Element> elts= XMLUtils.getChildElements(definition,"pages");
        for (Element elt : elts) renderers.put(elt.getAttribute("name"),new Renderer(base,elt));
        defaultRenderer=renderers.get("default");

    }

}
