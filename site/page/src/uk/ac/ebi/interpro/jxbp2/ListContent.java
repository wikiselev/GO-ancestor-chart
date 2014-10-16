package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.util.*;

import uk.ac.ebi.interpro.common.collections.*;

public class ListContent extends ObjectContent {
    Collection<?> list;


    public ListContent(Collection<?> list) {
        super(list);
        this.list = list;
        this.size=list.size();
    }

    public boolean first() {
        return index==0;
    }

    public boolean more() {
        return index<size-1;
    }

    public int index;
    public int size;
    public boolean next=true;
    Body body= Body.all;
    public void stop(){next=false;}

    enum Body {all,none,any,once}

    public void body(String body) {
        this.body= Body.valueOf(body);
    }


    public void content(ObjectProcessor processor, Element element) throws ProcessingException {

        switch (body) {

        case all:
            index=0;
            for (Object o : list) {
                SimpleAction.auto(processor, o,element);
                index++;
                if (!next) break;
            }
            break;
        case any:if (!list.isEmpty()) processor.processContent(element);break;
        case none:if (list.isEmpty()) processor.processContent(element);break;
        case once:processor.processContent(element);break;
        }

    }

    public void text(ObjectProcessor processor) {
        processor.outputText(CollectionUtils.concat(list,","));
    }
}
