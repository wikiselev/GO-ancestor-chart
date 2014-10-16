package uk.ac.ebi.interpro.common.xml;
import org.xml.sax.*;
import java.util.*;
public class SAXProtectEmpty implements ContentHandler {

    public SAXProtectEmpty(ContentHandler proxy) {
        this.proxy = proxy;
    }

    List<String> empty = Arrays.asList("area,base,basefont,br,col,frame,hr,img,input,isindex,link,meta,param".split(","));
    ContentHandler proxy;
    char[] noText ={};

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        proxy.startElement(namespaceURI, localName,qName, attributes);
        if (!empty.contains(localName)) proxy.characters(noText,0,0); 
    }

    public void setDocumentLocator(Locator locator) {proxy.setDocumentLocator(locator);}
    public void startDocument() throws SAXException {proxy.startDocument();}
    public void endDocument() throws SAXException {proxy.endDocument();}
    public void startPrefixMapping(String s, String s1) throws SAXException {proxy.startPrefixMapping(s, s1);}
    public void endPrefixMapping(String s) throws SAXException {proxy.endPrefixMapping(s);}

    public void endElement(String s, String s1, String s2) throws SAXException {proxy.endElement(s, s1, s2);}
    public void characters(char[] chars, int i, int i1) throws SAXException {proxy.characters(chars, i, i1);}
    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {proxy.ignorableWhitespace(chars, i, i1);}
    public void processingInstruction(String s, String s1) throws SAXException {proxy.processingInstruction(s, s1);}
    public void skippedEntity(String s) throws SAXException {proxy.skippedEntity(s);}
}
