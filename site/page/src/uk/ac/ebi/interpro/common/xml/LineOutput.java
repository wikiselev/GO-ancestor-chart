package uk.ac.ebi.interpro.common.xml;

import org.xml.sax.*;

import java.io.*;

public class LineOutput implements ContentHandler {
    Writer wr;
    boolean output=false;

    public LineOutput(Writer wr) {
        this.wr = wr;
    }


    public void setDocumentLocator(Locator locator) {}
    public void startDocument() throws SAXException {}
    public void endDocument() throws SAXException {}
    public void startPrefixMapping(String string, String string1) throws SAXException {}
    public void endPrefixMapping(String string) throws SAXException {}
    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (localName.equals("l")) output=true;
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equals("l")) {
            output=false;
            try {
                wr.write("\n");
            } catch (IOException e) {
                throw new SAXException("IOError",e);
            }
        }
    }

    public void characters(char[] chars, int start, int end) throws SAXException {
        if (output) {
            try {
                wr.write(chars, start, end);
            } catch (IOException e) {
                throw new SAXException("IOError",e);
            }
        }
    }

    public void ignorableWhitespace(char[] chars, int start, int end) throws SAXException {
        characters(chars,start,end);
    }

    public void processingInstruction(String string, String string1) throws SAXException {}
    public void skippedEntity(String string) throws SAXException {}
}
