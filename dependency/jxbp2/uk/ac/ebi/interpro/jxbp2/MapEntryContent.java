package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.util.*;

public class MapEntryContent implements Content {
    Map.Entry<?,?> entry;



    public MapEntryContent(Map.Entry<?, ?> entry) {
        this.entry = entry;
    }

    public void element(ObjectProcessor processor, Element element) throws ProcessingException {
        int bookmark=processor.bookmark();

        processor.bind(entry.getKey());
        processor.bind(entry.getValue());
        processor.processContent(element);
        processor.restore(bookmark);
    }

    public void text(ObjectProcessor processor) {
        processor.outputText(entry.getKey().toString()+":"+entry.getValue().toString());
    }


}
