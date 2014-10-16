package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public class BooleanContent implements Content {
    Boolean value;
    boolean compare=true;

    public void is(String test) {
        this.compare = Boolean.valueOf(test);
    }

    public BooleanContent(Boolean value) {
        this.value = value;
    }

    public void element(ObjectProcessor processor, Element element) throws ProcessingException {
        if (compare==value) processor.processContent(element);
    }

    public void text(ObjectProcessor processor) {
        processor.outputText(value?"true":"false");
    }
}
