package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public interface Content {
    void element(ObjectProcessor processor, Element element) throws ProcessingException;
    void text(ObjectProcessor processor);
}
