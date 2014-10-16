package uk.ac.ebi.interpro.jxbp2;


import org.w3c.dom.*;

import java.lang.reflect.*;

class FieldOutputAction extends SimpleAction {
    Field field;



    public String toString() {
        return field==null?"this":field.getDeclaringClass().getCanonicalName()+"."+field.getName()+" "+(field.isEnumConstant()?"[enum]":"");
    }

    public FieldOutputAction(Field field, Node node, Key key) {
        super(node, key);
        this.field = field;
    }



    public void execute(Object object, ObjectProcessor processor) throws ProcessingException {
        
        if (object==null) return;
        Object o;
        try {
            if (field==null) auto(processor,object);
            o = field.get(object);
        } catch (IllegalAccessException e) {
            throw new ProcessingException("Field access error", e,node);
        }
        if (field.isEnumConstant()) {
            if (o==object) processor.processContent((Element) node);
            return;
        }

        auto(processor,o);

    }


}
