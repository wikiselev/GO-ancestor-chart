package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.lang.reflect.*;

public abstract class SimpleRule implements Rule {


    protected BindingAction createAction(Node node, Key key, AccessibleObject target, Object[] parameterValues) {

        if (target instanceof Method) return new MethodAction((Method) target, node, key, parameterValues);
        if (target instanceof Field) return new FieldOutputAction((Field) target, node, key);
        return null;
    }

    public BindingAction test(Node node) {return null;}

    public BindingAction test(Node node, String in, int[] find) {return null;}

}
