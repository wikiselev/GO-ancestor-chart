package uk.ac.ebi.interpro.jxbp2.render;

import org.w3c.dom.*;

import javax.xml.xpath.*;

import uk.ac.ebi.interpro.jxbp2.*;

public class DataPage {

    public String id;
    public boolean debug;
    public String pageName;
    public String templateName;

    private Document document;

    XPath xp=XPathFactory.newInstance().newXPath();
    public String defaultXPath;

    public DataPage(String pageName,String templateName,Document document) {
        this.templateName = templateName;
        this.pageName = pageName;
        this.document = document;

    }

    public void test(Element elt, ObjectProcessor p) throws XPathExpressionException, ProcessingException {
        String xpath = elt.getAttribute("xpath");
        if (xpath==null || xpath.length()==0) xpath=defaultXPath;
        if (xpath==null || xpath.length()==0) xpath="/";
        XPathExpression xpe = xp.compile(xpath);
        Element element = (Element) xpe.evaluate(document.getDocumentElement(), XPathConstants.NODE);
        if (element!=null) p.processContent(elt);
    }

    public void use(Element elt,ObjectProcessor p) throws XPathExpressionException, ProcessingException {
        String xpath = elt.getAttribute("xpath");
        if (xpath==null || xpath.length()==0) xpath=defaultXPath;
        if (xpath==null || xpath.length()==0) xpath="/";
        XPathExpression xpe = xp.compile(xpath);
        Element element = (Element) xpe.evaluate(document.getDocumentElement(), XPathConstants.NODE);
        if (element!=null) p.processContent(element);
    }
}
