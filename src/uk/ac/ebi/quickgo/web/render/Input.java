package uk.ac.ebi.quickgo.web.render;

import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;

import org.w3c.dom.*;
import org.xml.sax.*;

public class Input {
    private Map<String,String[]> values;


    public Input() {
        this(new HashMap<String, String[]>());
    }

    public Input(Map<String, String[]> values) {
        this.values = values;
    }

    public void check(ObjectProcessor processor, Element element) throws ProcessingException, SAXException {
        processor.processAttributes(element);
        String name=processor.getAttributeValue("name");
        String value=processor.getAttributeValue("value");
        String[] values = this.values.get(name);
        if (values!=null && Arrays.asList(values).contains(value)) processor.outputAttribute("checked","checked");
        processor.startElement("input");        
        processor.endElement("input");
    }

    public void text(ObjectProcessor processor, Element element) throws ProcessingException, SAXException {
        processor.processAttributes(element);
        String name=processor.getAttributeValue("name");

        processor.startElement("textarea");
        String[] v = this.values.get(name);
        if (v!=null) processor.outputText(CollectionUtils.concat(v,", "));
        processor.endElement("textarea");
    }

    public void checked(ObjectProcessor processor) {

        String name=processor.container.getAttribute("name");
        String value=processor.container.getAttribute("value");
        /*System.out.println("Input:selected "+name+" "+value+" "+CollectionUtils.dump(values));*/
        if (Arrays.asList(values.get(name)).contains(value)) processor.outputAttribute("checked","checked");
    }
}
