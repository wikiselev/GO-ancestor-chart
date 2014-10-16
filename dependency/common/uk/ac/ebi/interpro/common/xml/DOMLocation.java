package uk.ac.ebi.interpro.common.xml;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;

public class DOMLocation {
    public String source;
    public static String key = DOMLocation.class.getCanonicalName();
    public int lineNumber;
    public int columnNumber;

    DOMLocation(Locator loc,String source) {
        this.source = source;
        lineNumber = loc.getLineNumber();
        columnNumber = loc.getColumnNumber();
    }

    public static DOMLocation getLocation(Node n) {        
        if (n==null) return null;
        return (DOMLocation) n.getUserData(key);
    }


    public String toString() {
        return lineNumber+":"+columnNumber+" "+source;
    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        DocumentFactory df = new DocumentFactory();
        Document d = df.load(new InputSource(new StringReader("<xml>hi</xml>")),"");
        Node child = d.getDocumentElement().getFirstChild();
        System.out.println(child.getClass().getCanonicalName());
        System.out.println(child.getParentNode());
    }
}
