package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public class TextContent extends ObjectContent {
    public final String value;
    private String test;

    public TextContent(String value) {
        super(value);
        this.value = value;
    }

    public void is(String test) {
        this.test = test;
    }


    public void content(ObjectProcessor processor, Element element) throws ProcessingException {
        if ((test==null) || test.equals(value))
            processor.processContent(element);
    }

    public void text(ObjectProcessor processor) {
        processor.outputText(value);
    }
}
