package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.util.*;
import java.lang.reflect.*;

public abstract class SimpleAction implements BindingAction {
    Key key;
    public Node node;

    protected SimpleAction(Node node, Key key) {
        this.node = node;
        this.key = key;
    }

    public Key getKey() {
        return key;

    }



    public static void auto(ObjectProcessor processor, Object object,Node node) throws ProcessingException {



        int restore=processor.bookmark();

        Content content;

        if (object instanceof Boolean) content=new BooleanContent((Boolean) object);
        else if (object instanceof Object[]) content=new ListContent(Arrays.asList((Object[])object));
        else if (object instanceof Collection) content=new ListContent((Collection<?>) object);
        else if (object instanceof Map) content=new MapContent((Map<?,?>)object);
        else if (object instanceof Map.Entry) content=new MapEntryContent((Map.Entry<?, ?>) object);
        else if (object instanceof CharSequence) content=new TextContent(object.toString());
        else if (object instanceof Number) content=new TextContent(object.toString());
        else if (object instanceof Element) content=new ElementContent((Element)object);
        else content=new ObjectContent(object);

        processor.bind(content);

        if (node instanceof Element) {

            Element element = (Element) node;
            processor.processAttributes(element);
            
            content.element(processor, element);
        }
        else content.text(processor);

        processor.restore(restore);


    }

    public void auto(ObjectProcessor processor, Object object) throws ProcessingException {
        try {
            auto(processor, object,node);
        } catch (Throwable e) {
            if (e.getCause() instanceof ProcessingException) throw (ProcessingException) e.getCause();
            throw new ProcessingException("Unable to process object", e,node);

        }
    }


    
}
