package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.util.*;

public class MapContent extends ListContent {
    Map<?,?> map;

    String key;

    public void get(String key) {
        this.key=key;
    }

    public MapContent(Map<?, ?> map) {
        super(map.entrySet());
        this.map = map;
    }

    public void content(ObjectProcessor processor, Element element) throws ProcessingException {
        if (key!=null) SimpleAction.auto(processor,map.get(key),element);
        else super.content(processor,element);
    }
}
