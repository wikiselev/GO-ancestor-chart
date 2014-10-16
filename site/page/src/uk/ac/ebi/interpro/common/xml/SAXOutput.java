package uk.ac.ebi.interpro.common.xml;

import org.xml.sax.*;
import org.xml.sax.ext.*;

import java.io.*;
import java.util.*;

public class SAXOutput implements ContentHandler, LexicalHandler {

    static List<String> htmlEmpty = Arrays.asList("area,base,basefont,br,col,frame,hr,img,input,isindex,link,meta,param".split(","));

    private List<String> empty;

    public void allowEmpty(List<String> empty) {
        this.empty = empty;
    }

    public void allowHTMLEmpty() {
        empty=htmlEmpty;
    }

    public void allowNoEmpty() {
        empty=new ArrayList<String>();
    }


    public SAXOutput(Writer target) {
        this.target = target;

    }

    Writer target;

    private boolean haveClosedTag = true;

    public void xmlEncoder(char data,boolean attr) throws SAXException {
        xmlEncoder(data, target,attr);
    }

    public void xmlEncoder(char data, Writer target,boolean attr) throws SAXException {
        try {
        if (data == '&') target.write("&amp;");
        else if (attr && data == '"') target.write("&quot;");
        else if (data == '<') target.write("&lt;");
        else target.write(data);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }


    public void xmlEncoder(String data,boolean attr) throws SAXException {
        xmlEncoder(data, target,attr);
    }

    public void xmlEncoder(String data, Writer target,boolean attr) throws SAXException {
        for (int i = 0; i < data.length(); i++) xmlEncoder(data.charAt(i), target,attr);
    }

    public void xmlEncoder(char[] ch, int start, int end,boolean attr) throws SAXException {
        for (int i = start; i < end; i++) xmlEncoder(ch[i],attr);
    }

    // ContentHandler

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    StringWriter attrs = new StringWriter();



    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        writeAttribute("xmlns:"+prefix,uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }



    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {

        checkClose();
        write("<");
        write(qName);
        for (int i = 0; i < attributes.getLength(); i++) writeAttribute(attributes.getQName(i),attributes.getValue(i));
        write(attrs.toString());
        attrs.getBuffer().setLength(0);
        haveClosedTag = false;
        if (empty!=null && !empty.contains(localName)) checkClose();
    }

    private void writeAttribute(String qName,String value) throws SAXException {

        attrs.write(" ");
        attrs.write(qName);
        attrs.write("=\"");
        xmlEncoder(value, attrs,true);
        attrs.write("\"");
    }

//    private void wrap(IOException e) throws SAXException {
//        throw new SAXException(e);
//    }


    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (!haveClosedTag) write(" />");
        else
            write("</" + qName + ">");

        haveClosedTag = true;

    }

    public void characters(char[] chars, int start, int end) throws SAXException {

        checkClose();
        xmlEncoder(chars, start, end,false);

    }

    private void checkClose() throws SAXException {
        if (!haveClosedTag) write(">");
        haveClosedTag = true;
    }

    public void ignorableWhitespace(char[] chars, int start, int end) throws SAXException {
        characters(chars, start, end);
    }

    public void processingInstruction(String name, String data) throws SAXException {

        checkClose();
        write("<?");
        write(name);
        write(" ");
        write(data);
        write("?>");

    }

    public void skippedEntity(String string) throws SAXException {
    }


    // Lexical handler
    public void startDTD(String name, String publicID, String systemID) throws SAXException {
        write("<!DOCTYPE " + name);
        if (publicID!=null) write(" PUBLIC \"" + publicID + "\"");             
        else if (systemID!=null) write(" SYSTEM ");
        if (systemID!=null)
        write("\"" + systemID + "\"");
    }


    public void endDTD() throws SAXException {
        write(">");
    }

    public void startEntity(String string) throws SAXException {
    }

    public void endEntity(String string) throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void endCDATA() throws SAXException {
    }

    public void comment(char[] chars, int start, int length) throws SAXException {
        checkClose();
        write("<!--");
        write(chars, start, length);
        write("-->");
    }


    // Methods used to write to underlying writer
    private void write(String text) throws SAXException {
        try {
            target.write(text);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    private void write(char[] chars, int start, int length) throws SAXException {
        try {
            target.write(chars, start, length);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

}
