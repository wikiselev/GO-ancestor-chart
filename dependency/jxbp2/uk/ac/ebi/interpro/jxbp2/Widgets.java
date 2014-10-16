package uk.ac.ebi.interpro.jxbp2;

import uk.ac.ebi.interpro.common.performance.*;

import java.util.*;

import org.w3c.dom.*;

public class Widgets {

    private static Location me=new Location();

    Widgets parent;
    Element context;

    public Widgets(Widgets parent) {

        this.parent = parent;
        /*Action a=me.start("copy");*/
        definitions.putAll(parent.definitions);
        /*me.stop(a);*/
    }

    public Widgets(Widgets parent,Element context) {
        this(parent);
        this.context = context;
    }

    public Widgets() {
    }

    public void include(Widgets widgets) {
        definitions.putAll(widgets.definitions);
    }

    class Definition {

        Element element;
        private boolean includeElement;

        public Definition(Element element, boolean includeElement) {
            this.element = element;
            this.includeElement = includeElement;
        }
    }

    private Map<String, Definition> definitions=new HashMap<String, Definition>();

    public void define(ObjectProcessor binding,Node node) {
        Element element=null;
        String name=null;
        if (node instanceof Attr) {
            element=binding.container;
            name=((Attr)node).getNodeValue();
        }
        if (node instanceof Element) {
            element=(Element)node;
            name=element.getAttribute("name");
        }         
        definitions.put(name,new Definition(element,false));
        //System.out.println("Define: "+name+" "+element);
    }

    public String parameter(String name) {
        if (context.hasAttribute(name)) return context.getAttribute(name);
        if (parent!=null) return parent.parameter(name);
        return null;
    }

    public boolean test(Map<String,String> what) {
        boolean result=true;
        String value = what.get("value");
        String name = what.get("name");
        if (name!=null) result&=parameter(name)!=null;
        if (name!=null && value!=null) result&=value.equals(parameter(name));
        return result;
    }


    public void invoke(ObjectProcessor binding,Element element) throws ProcessingException {

        String name=element.getAttribute("name");
        if (name==null) return;


        /*Action a=me.log()?me.start("invoke:"+name+" "+ DOMLocation.getLocation(element)):null;*/

        execute(name,binding, element);
        /*me.stop(a);*/
    }

    public void execute(String name,ObjectProcessor binding,Element element) throws ProcessingException {
        Element container=binding.container;
        try {
            Widgets.Definition definition = definitions.get(name);
            if (definition==null) return;
            int reset=binding.bookmark();
            binding.bind(new Widgets(this,element));
            /*Action a2=me.log()?me.start("content:"+name+" "+ DOMLocation.getLocation(element)+" "+reset):null;*/
            if (definition.includeElement) binding.output(definition.element);
            else binding.processContent(definition.element);
            /*me.stop(a2);*/
            binding.restore(reset);
        } catch (ProcessingException e) {
            throw new ProcessingException("Failed while executing content",e,container);
        }
    }

    public void content(ObjectProcessor binding,Element element) throws ProcessingException {
        binding.bind(new Widgets(parent,parent.context));
        binding.processContent(context);
        binding.bind(this);
    }
}
