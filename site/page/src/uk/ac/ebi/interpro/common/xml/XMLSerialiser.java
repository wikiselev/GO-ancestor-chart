package uk.ac.ebi.interpro.common.xml;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

public class XMLSerialiser {
    String[] name = new String[2];
    private ContentHandler ch;

    public XMLSerialiser(ContentHandler ch) {

        this.ch = ch;
    }


    public void write(Element elt) throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        attr.clear();
        NamedNodeMap nnm = elt.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr a = (Attr) nnm.item(i);
            XMLUtils.splitName(a.getNodeName(), name);
            attr.addAttribute(a.getNamespaceURI(), name[1], a.getNodeName(), "CDATA", a.getNodeValue());
        }
        XMLUtils.splitName(elt.getNodeName(), name);
        String localName=name[1];
        ch.startElement(elt.getNamespaceURI(), localName, elt.getNodeName(), attr);
        NodeList nl = elt.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                String s = n.getNodeValue();
                ch.characters(s.toCharArray(), 0, s.length());
            }
            if (n.getNodeType() == Node.ELEMENT_NODE) write((Element) n);
        }
        ch.endElement(elt.getNamespaceURI(),localName, elt.getNodeName());
    }
}
