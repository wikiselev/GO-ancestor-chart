package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.jxbp2.render.*;
import uk.ac.ebi.quickgo.web.*;

import javax.servlet.http.*;

import org.w3c.dom.*;

import java.io.*;

public class Describe extends HttpServlet{


    public class NodeWalker {
        String page;
        Document document;
        RuleSet rs;
        private ClassKey key = new ClassKey(Node.class);
        public BindingAction action;
        public RuleSet.Shredded shredded;
        public boolean noAction() {return action==null;}

        public boolean element(ObjectProcessor op) {
            Node node= (Node) op.get(key);
            return node instanceof Element;
        }

        public boolean text(ObjectProcessor op) {
            Node node= (Node) op.get(key);
            return node instanceof Text;
        }

        public void children(ObjectProcessor op,Element e) throws ProcessingException {
            int bkm=op.bookmark();
            Node node= (Node) op.get(key);
            NodeList nnm=node.getChildNodes();
            if (nnm!=null) for (int i=0;i<nnm.getLength();i++) process(op, nnm.item(i), e);
            op.restore(bkm);
        }

        public void attributes(ObjectProcessor op,Element e) throws ProcessingException {
            int bkm=op.bookmark();
            Node node= (Node) op.get(key);
            NamedNodeMap nnm=node.getAttributes();
            if (nnm!=null) for (int i=0;i<nnm.getLength();i++) process(op, nnm.item(i), e);
            op.restore(bkm);
        }

        private void process(ObjectProcessor op, Node cn, Element e) throws ProcessingException {
            op.bind(cn);
            action=rs.getNodeInfo(cn).action;
            shredded=rs.getNodeInfo(cn).shredded;
            op.processContent(e);
        }

        public void document(ObjectProcessor op,Element e) throws ProcessingException {
            process(op,document.getDocumentElement(),e);
        }


        public NodeWalker(String page,Document document, RuleSet rs) {
            this.document = document;
            this.rs = rs;
        }
    }

    public void process(Request r) throws IOException, ProcessingException {

        /*String page=r.getParameter("page");
        String template=r.getParameter("template");
        Renderer renderer = r.getRenderer();
        CachedFileData file = page!=null?renderer.getPageFile(page):renderer.getTemplateFile(template);
        Renderer.CachedDocument document=renderer.borrowDocument(file); 
        r.write(r.outputHTML(true).setPage("Describe.xhtml").render(
                    new NodeWalker(
                            page,
                            document.document,
                            renderer.ruleSet
                    )
        ));
        document.close();*/

    }
}
