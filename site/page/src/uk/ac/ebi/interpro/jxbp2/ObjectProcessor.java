package uk.ac.ebi.interpro.jxbp2;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

import java.util.*;

import uk.ac.ebi.interpro.common.xml.*;

public class ObjectProcessor {

    public final RuleSet ruleSet;

    public ContentHandler contentHandler;

    private List<Object> bindings = new ArrayList<Object>();
    private LexicalHandler lexicalHandler;

    public int bookmark() {return bindings.size();}
    public void restore(int previous) {while (bindings.size() > previous)
            bindings.remove(bindings.size() - 1);}
    public ObjectProcessor bind(Object object) {bindings.add(object);return this;}

    public ObjectProcessor(ContentHandler contentHandler,RuleSet ruleSet) {
        this.contentHandler = contentHandler;
        this.ruleSet = ruleSet;
    }

    public ObjectProcessor(RuleSet ruleSet) {this.ruleSet = ruleSet;}

    public void process(Node node) throws ProcessingException {
        RuleSet.NodeInfo ni=ruleSet.getNodeInfo(node);

        if (execute(ni.action,node)) return;

        output(node);
    }

    void processText(Node node) throws ProcessingException {
        RuleSet.NodeInfo ni=ruleSet.getNodeInfo(node);
        RuleSet.Shredded shred=ni.shredded;
        //System.out.println("Shred: "+node.getNodeValue()+" = "+shred);
        while (shred != null) {
            outputText(shred.prior);
            execute(shred.action,node);
            shred = shred.next;
        }        
    }

    private boolean execute(BindingAction action,Node node) throws ProcessingException {
        if (action == null) return false;


        Key key = action.getKey();
        Object o = get(key);
        try {
            action.execute(o, this);
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessingException("Processing exception", e,node);
        }
        return true;

    }

    public Object get(Key key) {
        Object o = null;
        if (key!=null) for (Object mapping : bindings) if (key.matches(mapping)) o = mapping;
        return o;
    }

    public void output(Node node) throws ProcessingException {
        if (node instanceof Element) outputElement((Element) node);
        if (node instanceof Text) processText(node);
        if (node instanceof Attr) outputAttribute((Attr) node);
    }

    StringBuffer accumulator = new StringBuffer();






    private void outputElement(Element element) throws ProcessingException {

        try {

            processAttributes(element);

            startElement(element);
            processContent(element);
            endElement(element);
        } catch (SAXException e) {throw new ProcessingException("Unable to write element", e);}
    }

    private void endElement(Element element) throws SAXException {

        endElement( element.getNamespaceURI(), element.getLocalName(), element.getNodeName());
    }

    public void endElement(String name) throws SAXException {
        endElement(null,name,name);
    }

    private void endElement(String namespaceURI, String localName, String nodeName) throws SAXException {
        flushText();
        if (contentHandler!=null)
        contentHandler.endElement(namespaceURI, localName, nodeName);
    }

    public void startElement(String name) throws SAXException {
        startElement(null,name,name);
    }

    private void startElement(Element element) throws SAXException {
        startElement(element.getNamespaceURI(),element.getLocalName(), element.getNodeName());
    }

    private void startElement(String namespaceURI, String localName, String nodeName) throws SAXException {
        flushText();
        if (contentHandler!=null) contentHandler.startElement(namespaceURI, localName, nodeName, attr);
        attr.clear();
    }

    public void outputAttribute(Attr a) throws ProcessingException {
        processText(a);
        outputAttribute(a.getNamespaceURI(), a.getNodeName(),a.getLocalName(),getText());
    }

    public void outputAttribute(String uri,String nodeName,String localName,String value) {
        attr.addAttribute(uri, localName, nodeName, "CDATA", value);
    }

    public void outputAttribute(String name,String value) {
        attr.addAttribute(null, name, name, "CDATA", value);
    }

    AttributesImpl attr = new AttributesImpl();
    public Element container;

    public void processContent(Element element) throws ProcessingException {
        for (Node n : XMLUtils.listize(element.getChildNodes())) {
            container=element;
            process(n);
        }
    }

    public void processAttributes(Element element) throws ProcessingException {
        try {
            flushText();
        } catch (SAXException e) {throw new ProcessingException("Unable to write text", e);}
        attr.clear();
        for (Node attr : XMLUtils.listize(element.getAttributes())) {container=element;process(attr);}
    }

    public String getAttributeValue(String name) {
        return attr.getValue(name);
    }

    public void outputText(String text) {accumulator.append(text);}
    private String getText() {
        String s = accumulator.toString();
        accumulator.setLength(0);
        return s;
    }

    private void flushText() throws SAXException {
        if (accumulator.length()>0) {
            String s = getText();
            if (contentHandler!=null) contentHandler.characters(s.toCharArray(),0,s.length());
        }
    }

    public void clearAttributes() {
        attr.clear();
    }

    public void outputComment(String text) throws SAXException {
        if (lexicalHandler!=null) lexicalHandler.comment(text.toCharArray(),0,text.length());
    }


    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        this.lexicalHandler = lexicalHandler;
    }
}
