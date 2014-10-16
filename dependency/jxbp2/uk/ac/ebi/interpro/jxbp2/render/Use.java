package uk.ac.ebi.interpro.jxbp2.render;

import uk.ac.ebi.interpro.jxbp2.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.util.*;
import java.io.*;

public class Use {

    DocumentCache documentCache;

    private Element container;
    private Use parent;

    public Use(DocumentCache documentCache,Use parent,Element container) {

        this.documentCache = documentCache;

        this.parent = parent;
        this.container = container;
    }

    public Use(DocumentCache documentCache) {
        this.documentCache = documentCache;

    }




    public void define() {
        // we don't process definitions when they occur
    }

    public String parameter(String name) {
        if (container.hasAttribute(name)) return container.getAttribute(name);
        if (parent!=null) return parent.parameter(name);
        return null;
    }

    public boolean test(Map<String,String> what) {
        boolean result=true;
        String value = what.get("value");
        String name = what.get("name");
        if (name!=null) result=parameter(name)!=null;
        if (name!=null && value!=null) result&=value.equals(parameter(name));
        return result;
    }


    public void use(ObjectProcessor binding,Element element) throws ProcessingException {



        Element container=element;
        try {

            Document doc=container.getOwnerDocument();
            DocumentCache.CachedDocument borrowed=null;

            String file=element.getAttribute("file");
            if (file!=null && file.length()>0) {
                borrowed=documentCache.borrowDocument(file);
                doc=borrowed.document;
            }

            try {

                String name=element.getAttribute("name");

                Element definition=null;
                NodeList definitions=doc.getElementsByTagNameNS("java:uk.ac.ebi.interpro.jxbp2.render.Use","define");
                //binding.outputComment("define:"+definitions.getLength());
                for (int i=0;i<definitions.getLength();i++) {
                    Element test= (Element) definitions.item(i);
                    //binding.outputComment("<use:define name='"+name+"'/>");
                    if (test.getAttribute("name").equals(name)) definition=test;
                }

                //binding.outputComment("<use:use "+(file!=null?"file='"+file+"' ":"")+"name='"+name+"'>");

                if (definition==null) {
                    binding.outputComment("Not found "+(file!=null?"file="+file:"")+" name="+name);
                    return;
                }

                binding.bind(new Use(documentCache,this,container));
                binding.processContent(definition);

                //binding.outputComment("</use:use "+(file!=null?"file='"+file+"' ":"")+"name='"+name+"'/>");

            } finally {

            if (borrowed!=null) borrowed.close();
            }

        } catch (ProcessingException e) {
            throw new ProcessingException("Failed while executing content",e,container);
        } catch (IOException e) {
            throw new ProcessingException("Failed while executing content",e,container);
        } catch (SAXException e) {
            throw new ProcessingException("Failed while writing comment",e,container);
        }
    }




    public void content(ObjectProcessor binding) throws ProcessingException {
        binding.bind(parent);
        binding.processContent(container);
    }
}
