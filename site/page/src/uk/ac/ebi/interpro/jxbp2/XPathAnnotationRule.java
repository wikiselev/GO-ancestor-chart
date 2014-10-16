package uk.ac.ebi.interpro.jxbp2;


import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.lang.reflect.*;
import java.util.*;

import uk.ac.ebi.interpro.common.xml.*;
import uk.ac.ebi.interpro.common.performance.*;


/*
David notes a major performance problem using this with the built in Java XPath processor.
This problem hasn't been resolved, and so this class is not recommended for performance critical use
possible cause see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6344064
*/

public class XPathAnnotationRule extends SimpleRule {

    private static Location me=new Location();

    Key key;

    XPath xpath;

    List<Rule> targetRules = new ArrayList<Rule>();
    private List<XPathExpressionException> errors;

    class Invocation extends SimpleRule {
        AccessibleObject target;
        XPathExpression pattern;
        XPathExpression[] parameters;

        String xpathText;

        public Invocation(AccessibleObject target, String[] test) throws XPathExpressionException {

            xpathText=test[0];
            pattern = xpath.compile(test[0]);
            parameters = new XPathExpression[test.length - 1];

            for (int i = 1; i < test.length; i++) {
                parameters[i - 1] = xpath.compile(test[i]);
            }

            this.target = target;
        }

        public BindingAction test(org.w3c.dom.Node node) {
            try {

                if (!(node instanceof Element || node instanceof Attr)) return null;
                Node context = node;
                while (context != null) {
                    /*System.out.println("Test @ "+node+" "+xpathText);*/
                    if (XMLUtils.listize(((NodeList) pattern.evaluate(context, XPathConstants.NODESET))).contains(node))
                        break;
                    context = context.getParentNode();
                }
                if (context == null) return null;
                /*System.out.println("Match");*/
                Object[] parameterValues = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    XPathExpression parameter = parameters[i];
                    if (parameter == null) continue;
                    //System.out.println("Param: "+parameters[i]+" {"+parameter.evaluate(node)+"}");
                    parameterValues[i] = parameter.evaluate(node);
                }

                return createAction(node, key, target, parameterValues);
            }
            catch (XPathExpressionException e) {
                return null;
            }
        }

    }


    public XPathAnnotationRule(Class<?> c) {
        xpath=XPathFactory.newInstance().newXPath();
        key = new ClassKey(c);
        for (Method method : c.getMethods()) extract(method);
        for (Field field : c.getFields()) extract(field);


    }

    public void extract(AccessibleObject target)  {
        BindXPath bxp = target.getAnnotation(BindXPath.class);
        try {
        if (bxp != null)
            targetRules.add(new Invocation(target, bxp.value()));
        } catch (XPathExpressionException e) {
            errors.add(e);
        }

    }


    public BindingAction test(Node node) {
        for (Rule targetRule : targetRules) {
            BindingAction a = targetRule.test(node);
            if (a!=null) return a;
        }
        return null;
    }
}
