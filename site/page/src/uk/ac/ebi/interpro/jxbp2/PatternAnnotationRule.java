package uk.ac.ebi.interpro.jxbp2;

import java.util.*;
import java.lang.reflect.*;

import uk.ac.ebi.interpro.common.*;
import org.w3c.dom.*;

public class PatternAnnotationRule extends SimpleRule {

    List<Rule> targetRules = new ArrayList<Rule>();

    Key key;

    class Invocation extends SimpleRule {
        private AccessibleObject target;
        Seeker seeker;

        public Invocation(AccessibleObject target, Seeker seeker) {

            this.target = target;

            this.seeker = seeker;
        }


        public BindingAction test(Node node, String in, int[] find) {
            Seeker.Locator matcher = seeker.matcher(in);
            if (!matcher.find()) return null;

            find[0] = matcher.start();
            find[1] = matcher.end();
            int count = matcher.count();
            Object[] parameters = new Object[count];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = matcher.group(i + 1);
            }

            return createAction(node, key, target, parameters);

        }

    }


    public void extract(Object key, AccessibleObject target) {
        BindRegexp br = target.getAnnotation(BindRegexp.class);
        if (br != null)
            targetRules.add(new Invocation(target, new RegexSeeker(br.value())));


        BindStrings bs = target.getAnnotation(BindStrings.class);
        if (bs != null)
            targetRules.add(new Invocation(target, new StringSeeker(bs.value())));
    }


    public PatternAnnotationRule(Class<?> c) {
        key=new ClassKey(c);
        for (final Method method : c.getMethods()) extract(c, method);
        for (Field field : c.getFields()) extract(c, field);


    }


    public BindingAction test(Node node, String in, int[] find) {

        for (Rule targetRule : targetRules) {
            BindingAction a = targetRule.test(node,in,find);
            if (a!=null) return a;
        }
        return null;    
    }
}
