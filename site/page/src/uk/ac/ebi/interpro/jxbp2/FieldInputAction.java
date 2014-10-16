package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.lang.reflect.*;

class FieldInputAction extends SimpleAction {
    Field field;

    public FieldInputAction(Field field, Node node, Key key) {
        super(node, key);
        this.field = field;
    }


    public void execute(Object object, ObjectProcessor processor) throws ProcessingException {
        if (object==null) return;

        try {
            if (node instanceof Element)
                set(object, node.getFirstChild().getNodeValue());
            if (node instanceof Attr)
                set(object, ((Attr) node).getNodeValue());
        } catch (IllegalAccessException e) {
            throw new ProcessingException("Field access error", e,node);
        }
        return;
    }



    private void set(Object object, String s) throws IllegalAccessException {
        field.set(object, s);
    }



}
