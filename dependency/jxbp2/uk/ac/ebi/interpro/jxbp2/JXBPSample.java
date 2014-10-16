package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;
import org.xml.sax.*;
import uk.ac.ebi.interpro.common.xml.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;

public class JXBPSample {

    public static class Say {
        public String text;

        public Say(String text) {
            this.text = text;
        }
    }

    public static class Cow {
        public String name="Mr Cow";
        public Say[] says={new Say("moo")};
        @BindStrings({"{*","}"})
        public String whatif(String why) {
            return "Don't "+why;
        }
        @BindXPath({"ask","@what"})
        public String ask(String what) {
            return "I don't know about "+what;
        }
    }


    public static void main(String[] args) throws ProcessingException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        DocumentFactory f=new DocumentFactory();

        f.setWhiteSpaceCompress(true);

        String xml = "<xml xmlns:c='java:"+Cow.class.getCanonicalName()+"' \n" +
                "xmlns:s='java:"+Say.class.getCanonicalName()+"'> \n" +
                "name: {c:name} \n" +
                "says: <c:says>{s:text}</c:says>.\n" +
                "{*bite}. \n" +
                "<ask what='the moon'/></xml>\n";

        

        Document doc=f.load(new InputSource(new StringReader(xml)),"Sample");

        System.out.println("OXF\n"+
        XMLUtils.getOuterXML(false,false,doc.getDocumentElement()));

        System.out.println("OXT\n"+
        XMLUtils.getOuterXML(true,false,doc.getDocumentElement()));

        Writer wr=new OutputStreamWriter(System.out);

        
        RuleSet rs=new RuleSet(
                new XPathAnnotationRule(Cow.class),
                new PatternAnnotationRule(Cow.class),
                new JavaNamespaceRule(JXBPSample.class.getClassLoader()));
        ObjectProcessor processor = new ObjectProcessor(new SAXOutput(wr),rs);
        processor.bind(new Cow());
        processor.process(doc.getDocumentElement());
        wr.flush();
    }

}
