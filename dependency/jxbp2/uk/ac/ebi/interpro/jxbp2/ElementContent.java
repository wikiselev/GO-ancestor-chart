package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public class ElementContent extends ObjectContent {
    Element element;

    public ElementContent(Element element) {
        super(element);
        this.element = element;
    }

    public void element(ObjectProcessor processor, Element element) throws ProcessingException {
        processor.processContent(this.element);
    }

    public void text(ObjectProcessor processor) {
        processor.outputText(element.getNodeName());
    }
}
