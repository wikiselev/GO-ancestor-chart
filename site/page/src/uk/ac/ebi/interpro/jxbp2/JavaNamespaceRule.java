package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;
import org.xml.sax.helpers.*;
import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.xml.*;

import javax.xml.XMLConstants;
import java.lang.reflect.*;


public class JavaNamespaceRule extends SimpleRule {
    private ClassLoader loader;
    private Class clazz;

    public JavaNamespaceRule(ClassLoader loader) {
        this.loader = loader;
    }

    public JavaNamespaceRule(Class clazz) {
        this.clazz=clazz;
    }


    public BindingAction test(final Node node,String uri,String name,String[] parameters) {
        //if (node instanceof Attr) System.out.println("Attr "+uri+" "+name+" "+node.getParentNode());

        if (name==null) return null;

        if (node instanceof Attr && (NamespaceSupport.NSDECL.equals(uri)
                || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri))) {
            if (node.getNodeValue().startsWith("java:")) return new SkipAction();
            else return null;
        }

        final Class<?> clazz= getClassForURI(uri);

        //System.out.println("Load class "+clazz+" "+className);
        if (clazz==null) return null;
        final ClassKey classKey=new ClassKey(clazz);

        //System.out.println("Loaded "+clazz.getSimpleName());

        if (name.equals("this")) {
            return new BindingAction() {
                public Key getKey() {return classKey;}

                public void execute(Object object, ObjectProcessor processor) throws ProcessingException {
                    if (object!=null) processor.processContent((Element) node);
                }
            };
        }

        if (name.equals("null")) {
            return new BindingAction() {
                public Key getKey() {return classKey;}

                public void execute(Object object, ObjectProcessor processor) throws ProcessingException {
                    if (object==null) processor.processContent((Element) node);
                }
            };
        }

        AccessibleObject ao= null;

        for (Field field : clazz.getFields())
            if (field.getName().equals(name)) ao=field;

        for (Method method : clazz.getMethods())
            if (method.getName().equals(name)) ao = method;

        if (ao==null) return null;

        return createAction(node,new ClassKey(clazz),ao,parameters);
    }

    private Class<?> getClassForURI(String uri) {

        if (uri==null) return clazz;
        else if (!uri.startsWith("java:")) return null;
        else return loadClassSimpleName(uri.substring(5));
    }

    private Class<?> loadClassSimpleName(String className) {
        while (true) {
            try {
                return loader.loadClass(className);
            } catch (ClassNotFoundException e) {/* try swapping . for $ to access inner classes */}
            int i=className.lastIndexOf(".");
            if (i<0) return null;
            className=className.substring(0,i)+"$"+className.substring(i+1);
        }
    }

    public BindingAction test(Node node) {
        String nodeURI=node.getNamespaceURI();
        String nodeName = node.getLocalName();
        
        return test(node,nodeURI,nodeName,null);

    }

    StringSeeker seeker=new StringSeeker("{",":","}");

    public BindingAction test(Node node, String in, int[] find) {
        Seeker.Locator matcher = seeker.matcher(in);
        if (!matcher.find()) return null;

        find[0]=matcher.start();find[1]=matcher.end();

        String uri=node.lookupNamespaceURI(matcher.group(1));

        String[] split=matcher.group(2).split(":",2);
        String name=split[0];
        String[] args=split.length==1?null:split[1].split(":");
        return test(node,uri,name,args);

    }




    enum A {X,Y}

    public static void main(String[] args) throws Exception {

        System.out.println("NS:"+new DocumentFactory().parseString("<hi></hi>").getDocumentElement().getNamespaceURI());

        //System.out.println(JavaNamespaceRule.class.getMethod("woot"));
        System.out.println(CollectionUtils.dump(A.class.getFields()));
/*
        System.out.println(CollectionUtils.dump(JavaNamespaceRule.class.getFields()));
        System.out.println(CollectionUtils.dump(JavaNamespaceRule.class.getMethods()));*/
    }

}
