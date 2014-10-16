package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

import java.lang.reflect.*;
import java.util.*;

import uk.ac.ebi.interpro.common.xml.*;

class MethodAction extends SimpleAction {
    Method method;
    private Object[] parameterTemplates;

    public MethodAction(Method method, Node node, Key key,Object[] parameters) {
        super(node, key);
        this.method = method;
        this.parameterTemplates = parameters;
    }


    public void execute(Object object, ObjectProcessor processor) throws ProcessingException {

        Object returns;
        try {
            
            Class<?>[] parameterTypes=method.getParameterTypes();
            Object[] parameters=new Object[parameterTypes.length];
            if (parameterTemplates!=null) 
                System.arraycopy(parameterTemplates,0,parameters,0,Math.min(parameterTemplates.length,parameters.length));
            Map<String,String> attributes= new XMLUtils.AttributeMap(node.getAttributes());
            String value=node.getNodeValue();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i]!=null) continue;
                isOfType(parameterTypes, i, node, parameters);
                isOfType(parameterTypes, i, attributes, parameters);
                isOfType(parameterTypes, i, processor, parameters);
                isOfType(parameterTypes, i, value, parameters);
            }

            if (!Modifier.isStatic(method.getModifiers()) && object==null) throw new NullPointerException("No object found for instance method "+method.getName()+" "+key);
            
            returns = method.invoke(object, parameters);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ProcessingException) throw (ProcessingException) e.getCause();
            throw new ProcessingException("Unable to execute "+method+" on "+object, e.getCause(),node);

        } catch (Exception e) {
            throw new ProcessingException("Unable to execute "+method+" on "+object, e,node);
        }
        auto(processor, returns);

    }

    private void isOfType(Class<?>[] parameterTypes, int i, Object object, Object[] parameters) {
        if (parameterTypes[i].isInstance(object)) parameters[i]= object;
    }


    public String toString() {
        return method.getDeclaringClass().getCanonicalName()+"."+method.getName()+"()";
    }


}
