package uk.ac.ebi.interpro.common.xml;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.util.*;
import java.io.*;

import uk.ac.ebi.interpro.common.*;


public class XMLUtils {
    public static List<Element> getChildElements(Element elt, String name) {
        List<Element> list = new ArrayList<Element>();

        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
                if (name.equals(child.getNodeName()))
                    list.add((Element) child);
        }
        return list;
    }

    public static Element getChildElement(Element elt, String name) {
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
                if (name.equals(node.getNodeName())) return (Element) node;
        }
        return null;
    }

    public static void splitName(String nodeName, String[] name) {
        int p = nodeName.indexOf(":");
        if (p < 0) {
            name[0] = null;
            name[1] = nodeName;
        } else {
            name[0] = nodeName.substring(0, p);
            name[1] = nodeName.substring(p + 1);
        }
    }

    public static String getOuterXML(boolean checkNS, boolean html, Element elt) throws SAXException {
        StringWriter sw = new StringWriter();
        ContentHandler ch = new SAXOutput(sw);
        if (checkNS) ch = new SAXCheckNS(ch);
        if (html) ch = new SAXProtectEmpty(ch);
        new XMLSerialiser(ch).write(elt);
        return sw.toString();
    }

    public static void getInnerText(StringBuilder sb,Element elt,String separator) {
        NodeList tns = elt.getChildNodes();
        for (int j = 0; j < tns.getLength(); j++) {
            Node tn = tns.item(j);
            if (tn.getNodeType() == Node.TEXT_NODE) {
                sb.append(tn.getNodeValue());
            } else if (tn.getNodeType() == Node.ELEMENT_NODE) {
                sb.append(separator);
                getInnerText(sb, (Element) tn,separator);
                sb.append(separator);
            }
        }
    }

    public static String getInnerText(Element elt) {

        StringBuilder sb = new StringBuilder();
        getInnerText(sb,elt," ");
        return sb.toString();
    }

    public static String setInnerText(Element node, String value) {
        StringBuilder sb = new StringBuilder();
        while (node.hasChildNodes()) {
            Node tn = node.getFirstChild();
            if (tn.getNodeType() == Node.TEXT_NODE) {
                sb.append(tn.getNodeValue());
            }
            node.removeChild(tn);
        }
        node.appendChild(node.getOwnerDocument().createTextNode(value));
        return sb.toString();
    }

    public static String setChildText(Element elt, String name, String value) {
        Element child = getChildElement(elt, name);

        if (child == null) {
            child = elt.getOwnerDocument().createElement(name);
            elt.appendChild(child);
        }
        return setInnerText(child, value);

    }

    public static String getChildText(Element elt, String name) {
        Element child = getChildElement(elt, name);
        return child == null ? null : getInnerText(child);
    }


    public static Map<String, String> getChildTextMap(Element elt) {
        Map<String, String> data = new HashMap<String, String>();
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) child;
                data.put(e.getNodeName(), getInnerText(e));
            }
        }
        return data;
    }

    public static List<Node> listize(final NodeList list) {
        return new AbstractList<Node>() {
            public Node get(int i) {
                return list.item(i);
            }

            public int size() {
                return list.getLength();
            }
        };
    }

    public static List<Node> listize(final NamedNodeMap nnm) {
        return new AbstractList<Node>() {
            public Node get(int i) {
                return nnm.item(i);
            }

            public int size() {
                return nnm.getLength();
            }


        };
    }


    public static class AttributeMap extends AbstractMap<String, String> {


        List<Node> n;


        public AttributeMap(NamedNodeMap nnm) {
            n=listize(nnm);
            
        }

        Set<Entry<String, String>> entries=new AbstractSet<Entry<String, String>>() {
            public Iterator<Entry<String, String>> iterator() {
                final Iterator<Node> it = n.iterator();

                final AttributeEntry e= new AttributeEntry();
                return new Iterator<Entry<String, String>>() {
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    public Entry<String, String> next() {
                        e.n=it.next();
                        return e;
                    }

                    public void remove() {
                        it.remove();
                    }
                };
            }

            public int size() {
                return n.size();
            }
        };

        public Set<Entry<String, String>> entrySet() {
            return entries;
        }

        private static class AttributeEntry implements Entry<String,String> {
            Node n;

            public String getKey() {
                return n.getNodeName();
            }

            public String getValue() {
                return n.getNodeValue();
            }

            public String setValue(String s) {
                throw new UnsupportedOperationException();
            }
        }
    }

    public static class ChildElementTextMap extends AbstractMap<String, String> {

        Element elt;

        public ChildElementTextMap(Element elt) {
            this.elt = elt;
        }

        Set<Entry<String, String>> entries = new AbstractSet<Entry<String, String>>() {

            public Iterator<Entry<String, String>> iterator() {
                return new ChildElementIterator(elt);
            }

            public int size() {
                int i = 0;
                for (Entry<String, String> entry : entries) i++;
                return i;
            }

        };

        public Set<Entry<String, String>> entrySet() {
            return entries;
        }


        public String put(String key, String value) {

            for (Entry<String, String> entry : entries) {
                if (entry.getKey().equals(key)) {
                    String s = entry.getValue();
                    entry.setValue(value);
                    return s;
                }
            }
            Element e = elt.getOwnerDocument().createElement(key);
            setInnerText(e, value);
            elt.appendChild(e);
            return null;
        }

        private static class ChildElementIterator implements Iterator<Entry<String, String>> {

            Element nextChild;
            Element currentChild;
            Entry<String, String> entry = new Entry<String, String>() {

                public String getKey() {
                    return currentChild.getNodeName();
                }

                public String getValue() {
                    return getInnerText(currentChild);
                }

                public String setValue(String v) {
                    String old = getInnerText(currentChild);
                    setInnerText(currentChild, v);
                    return old;
                }
            };

            private Node parent;


            public ChildElementIterator(Node parent) {
                this.parent = parent;
                //seek(parent.getParentNode());
                seek(parent.getFirstChild());
                next();
            }

            public boolean hasNext() {
                return nextChild != null;
            }

            public void seek(Node n) {

                while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
                    n = n.getNextSibling();
                }
                nextChild = (Element) n;
            }

            public Entry<String, String> next() {
                if (nextChild == null) throw new NoSuchElementException();
                currentChild = nextChild;
                seek(currentChild.getNextSibling());
                return entry;
            }

            public void remove() {
                parent.removeChild(currentChild);
            }
        }
    }

    public static Element write(Document d,Object o) {
        FieldIntrospectiveMap fields=new FieldIntrospectiveMap(o);
        Element e=d.createElement(o.getClass().getSimpleName());
        for (String key : fields.keySet()) {
            Object f=fields.get(key);
            if (f instanceof String) e.setAttribute(key,(String)f);
            else e.appendChild(write(d,f));
        }
        return e;
    }


    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

        String xml = "<xml><u>8</u><i>9</i></xml>";
        DocumentFactory f = new DocumentFactory();
        Document doc = f.parseString(xml);
            //System.out.println(XMLUtils.class.getSimpleName());
        System.out.println(getInnerText(doc.getDocumentElement()));
        /*




        ChildElementTextMap m = new ChildElementTextMap(doc.getDocumentElement());
        for (String key : m.keySet()) {
            System.out.println(key + ":" + m.get(key));
        }
        m.put("x", "1");
        System.out.println(getOuterXML(false, false, doc.getDocumentElement()));
        */
    }





}
