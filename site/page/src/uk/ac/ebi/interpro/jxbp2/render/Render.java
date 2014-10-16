package uk.ac.ebi.interpro.jxbp2.render;

import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.common.http.*;
import uk.ac.ebi.interpro.common.xml.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.common.*;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

public class Render {

    private static Location me=new Location();

    List<Closeable> resources =new ArrayList<Closeable>();

    ObjectProcessor processor;

    private HTTPResponse page;
    DataPage dataPage;


    Widgets widgets=new Widgets();
    private String stylePath;
    private Document styleDoc;
    private Document pageDoc;
    private Writer wr;
    private WidgetCache widgetCache;
    private DocumentCache documentCache;
    private RuleSet ruleSet;


    public Render(DocumentCache documents, WidgetCache widgets,RuleSet ruleSet) throws IOException, ProcessingException {
        this.widgetCache = widgets;
        this.documentCache = documents;


        this.ruleSet = ruleSet;
    }

    public Render html() throws IOException {
        page = new HTTPResponse("text/html");
        wr = page.getWriter();
        wr.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
        SAXOutput output = new SAXOutput(wr);
        processor=new ObjectProcessor(new SAXProtectEmpty(new SAXCheckNS(output)), ruleSet);
        processor.setLexicalHandler(output);
        processor.bind(widgets);
        processor.bind(new Use(documentCache));
        return this;
    }

    public Render xml() {
        page = new HTTPResponse("text/xml");
        wr = page.getWriter();
        processor=new ObjectProcessor(new SAXCheckNS(new SAXOutput(wr)), ruleSet);
        processor.bind(widgets);
        processor.bind(new Use(documentCache));
        return this;
    }

    public Render text(String mimetype) {
        page = new HTTPResponse(mimetype);
        wr = page.getWriter();
        processor=new ObjectProcessor(new LineOutput(wr), ruleSet);
        processor.bind(widgets);
        processor.bind(new Use(documentCache));
        return this;
    }

    public Render setStyle(String path) throws IOException {
        stylePath = path;
        DocumentCache.CachedDocument cached = documentCache.borrowDocument(path);
        resources.add(cached);
        styleDoc = cached.document;

        return this;
    }



    public Render setPage(String path) throws IOException {
        DocumentCache.CachedDocument cached = documentCache.borrowDocument(path);
        resources.add(cached);
        pageDoc = cached.document;
        dataPage=new DataPage(path, stylePath, pageDoc);

        return this;
    }




    public Render use(String path) throws IOException, ProcessingException {
        WidgetCache.CachedWidgets cached = widgetCache.borrowWidgets(path);
        resources.add(cached);
        widgets.include(cached.widgets);
        return this;
    }

    public Render bind(Object o) {
        processor.bind(o);
        return this;
    }

    public Render find(String xpath) {
        dataPage.defaultXPath=xpath;
        return this;
    }

    public HTTPResponse render(Object... bind) throws ProcessingException {
        Action a= me.start("Render");
        Element root=pageDoc.getDocumentElement();
        if (styleDoc !=null) root= styleDoc.getDocumentElement();
        for (Object o : bind) processor.bind(o);
        processor.bind(dataPage);

        processor.process(root);

        IOUtils.closeAll(resources);

        try {
            wr.flush();
        } catch (IOException e) {
            throw new ProcessingException("Error flushing",e);
        }

        me.stop(a);
        return page;
    }


    
}
