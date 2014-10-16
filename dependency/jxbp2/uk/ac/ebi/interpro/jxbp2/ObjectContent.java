package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public class ObjectContent implements Content {
    Object value;
    
    public ObjectContent(Object value) {
        this.value = value;
    }

    boolean testnull=false;
    public void is(String is) {isnull(is);}
    public void isnull(String is) {testnull="null".equals(is);}

    public void content(ObjectProcessor processor, Element element) throws ProcessingException {
        if (value!=null) processor.bind(value);
        processor.processContent(element);
    }

    public void element(ObjectProcessor processor, Element element) throws ProcessingException {
        if ((value==null) == testnull)
            content(processor,element);
    }

    public void text(ObjectProcessor processor) {
        if (value!=null) processor.outputText(value.toString());
    }
}
