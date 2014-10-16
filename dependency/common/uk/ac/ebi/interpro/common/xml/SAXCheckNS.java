package uk.ac.ebi.interpro.common.xml;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
public class SAXCheckNS implements ContentHandler {


    public SAXCheckNS(ContentHandler proxy) {
        this.proxy = proxy;
    }

    ContentHandler proxy;
    AttributesImpl attr=new AttributesImpl();
    boolean pushed=false;
    NamespaceSupport namespaceSupport=new NamespaceSupport();
    String[] name=new String[2];

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        mayPushContext();
        for (int i = 0; i < attributes.getLength(); i++) {
            XMLUtils.splitName(attributes.getQName(i),name);
            String value = attributes.getValue(i);
            if ("xmlns".equals(name[0])) startPrefixMapping(name[1], value);
            else attr.addAttribute(attributes.getURI(i),attributes.getLocalName(i),attributes.getQName(i),attributes.getType(i),attributes.getValue(i));
        }
        for (int i = 0; i < attr.getLength(); i++) checkQName(attr.getQName(i),attr.getURI(i));
        checkQName(qName,namespaceURI);
        proxy.startElement(namespaceURI, localName,qName, attr);
        pushed=false;
        attr.clear();
    }

    private void checkQName(String qName, String namespaceURI) throws SAXException {
        XMLUtils.splitName(qName,name);
        String prefix=name[0];
        if (prefix==null || namespaceURI==null) return;
        if (!namespaceURI.equals(namespaceSupport.getURI(prefix))) startPrefixMapping(prefix, namespaceURI);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        proxy.endElement(namespaceURI, localName, qName);
        namespaceSupport.popContext();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        mayPushContext();
        proxy.startPrefixMapping(prefix, uri);
        namespaceSupport.declarePrefix(prefix,uri);
    }

    private void mayPushContext() {
        if (pushed) return;
        namespaceSupport.pushContext();
        pushed=true;
    }

    public void setDocumentLocator(Locator locator) {proxy.setDocumentLocator(locator);}
    public void startDocument() throws SAXException {proxy.startDocument();}
    public void endDocument() throws SAXException {proxy.endDocument();}
    public void endPrefixMapping(String s) throws SAXException {proxy.endPrefixMapping(s);}
    public void characters(char[] chars, int i, int i1) throws SAXException {proxy.characters(chars, i, i1);}
    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {proxy.ignorableWhitespace(chars, i, i1);}
    public void processingInstruction(String s, String s1) throws SAXException {proxy.processingInstruction(s, s1);}
    public void skippedEntity(String s) throws SAXException {proxy.skippedEntity(s);}
}
