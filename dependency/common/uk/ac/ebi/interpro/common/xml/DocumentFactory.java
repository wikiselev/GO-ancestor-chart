package uk.ac.ebi.interpro.common.xml;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.parsers.*;
import javax.xml.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class DocumentFactory {
    private boolean whiteSpaceCompress;



    public void setWhiteSpaceCompress(boolean whiteSpaceCompress) {
        this.whiteSpaceCompress = whiteSpaceCompress;
    }

    public Document parseString(String value) throws ParserConfigurationException, SAXException, IOException {
        return load(new InputSource(new StringReader(value)),"text");
    }

    public Document load(File f) throws ParserConfigurationException, SAXException, IOException {
        FileInputStream fis = new FileInputStream(f);
        Document d;
        try {
            d = load(new InputSource(fis), f.getName());
        } finally {
            fis.close();
        }
        return d;

    }

    public Document load(InputSource is,String name) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document d = dbf.newDocumentBuilder().newDocument();
        SAXParserFactory sxpf = SAXParserFactory.newInstance();
        sxpf.setNamespaceAware(true);
        SAXParser sp = sxpf.newSAXParser();
        CustomDefaultHandler handler = new CustomDefaultHandler(d,name);
        try {
            sp.parse(is, handler);
        } catch (Exception e) {
            IOException e2;
            if (handler.locator==null)
                e2 = new IOException("Failure parsing "+ name);
            else
                e2 = new IOException("Document parse failure at " + name + " " + handler.locator.getLineNumber() + ":" + handler.locator.getColumnNumber());
            e2.initCause(e);
            throw e2;
        }
        return d;
    }

    private class CustomDefaultHandler extends DefaultHandler {
        Document document;
        Node current;
        private String name;

        private Locator locator;


        public CustomDefaultHandler(Document document,String name) {
            this.document = document;
            current = document;
            this.name = name;
        }



        List<String[]> namespaces=new ArrayList<String[]>();

        public void addAttribute(Element e,String uri,String qname,String value) {
            /*e.setAttributeNS(uri,qname,value);*/
            Attr a=document.createAttributeNS(uri,qname);
            a.setValue(value);
            attach(a);
            e.setAttributeNodeNS(a);
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            //System.out.println("Elt: "+uri+" "+qName);
            Element e = document.createElementNS(uri, qName);
            attach(e);
            for (int i = 0; i < atts.getLength(); i++) {
                //System.out.println("Att: "+atts.getURI(i)+" "+atts.getQName(i)+"="+atts.getValue(i));
                addAttribute(e,atts.getURI(i), atts.getQName(i), atts.getValue(i));
            }
            for (String[] namespace : namespaces) {
                //String xmlns= XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                //System.out.println("NS: xmlns:"+namespace[0]+"="+namespace[1]+" "+xmlns+" "+NamespaceSupport.NSDECL);
                /*Attr attr = document.createAttributeNS(xmlns, "xmlns:" + namespace[0]);
                attr.setValue(namespace[1]);
                e.setAttributeNodeNS(attr);*/
                addAttribute(e,XMLConstants.XMLNS_ATTRIBUTE_NS_URI,"xmlns:"+namespace[0], namespace[1]);
                //e.setAttribute("xmlns:"+namespace[0], namespace[1]);
                
            }
            namespaces.clear();
            current.appendChild(e);
            current = e;

        }

        private Node attach(Node e) {
            e.setUserData(DOMLocation.key, new DOMLocation(locator,name), null);
            return e;
        }


        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            namespaces.add(new String[]{prefix, uri});
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (!(current instanceof Element)) throw new SAXException("End tag outside of element");

            String currentName = this.current.getNodeName();
            if (!currentName.equals(qName)) throw new SAXException("Element " + currentName + " not ");
            this.current = this.current.getParentNode();
            if (this.current == null) throw new SAXException("Fell off end of document");

        }


        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        Pattern tabspace=Pattern.compile("[ \t]+");

        public void characters(char[] chars, int start, int end) throws SAXException {
            String text = new String(chars, start, end);
            if (whiteSpaceCompress) text=tabspace.matcher(text).replaceAll(" ");
            Node c = ((Element) current).getLastChild();
            Text t;
            if (c!=null && c instanceof Text) t= (Text) c;
            else t= document.createTextNode("");
            t.appendData(text);
            attach(t);
            current.appendChild(t);
        }
    }

    public static void main(String[] args) throws Throwable {
        DocumentFactory df=new DocumentFactory();
        String doc = "<html xmlns:page='java:uk.ac.ebi.quickgo.web.render.Renderer.DataPage'>\n" +
                "<head with='me'>\n" +
                "  <title><page:use name='title'/></title>\n" +
                "  <page:use xpath='head'/>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1><page:use name='title'/></h1>\n" +
                "    <page:use xpath='body'/>\n" +
                "</body>\n" +
                "</html>";
        Document d;
        NamedNodeMap nnm;

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        DocumentBuilder b = f.newDocumentBuilder();
        d = b.parse(new InputSource(new StringReader(doc)));

        System.out.println(XMLUtils.getOuterXML(true,true,d.getDocumentElement()));

        nnm = d.getDocumentElement().getAttributes();
        
        System.out.println("URI:"+d.getDocumentElement().lookupNamespaceURI("page"));

        for (int i=0;i<nnm.getLength();i++) {
            Node a = nnm.item(i);
            System.out.println(a.getNamespaceURI()+" "+a.getLocalName()+" "+a.getNodeName()+" "+a.getNodeValue());
        }

        d=df.load(new InputSource(new StringReader(doc)),"");

        System.out.println(XMLUtils.getOuterXML(true,true,d.getDocumentElement()));
        nnm = d.getDocumentElement().getAttributes();

        System.out.println("URI:"+d.getDocumentElement().lookupNamespaceURI("page"));

        for (int i=0;i<nnm.getLength();i++) {
            Node a = nnm.item(i);
            System.out.println(a.getNamespaceURI()+" "+a.getLocalName()+" "+a.getNodeName()+" "+a.getNodeValue()+" ["+a.getParentNode()+"]");
        }

    }
}
