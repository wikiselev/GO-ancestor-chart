package uk.ac.ebi.interpro.jxbp2.render;

import org.w3c.dom.*;
import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.jxbp2.*;

import java.io.*;
import java.util.*;

public class Renderer {


    final public RuleSet ruleSet;


    public final FileCache files;
    public final DocumentCache documents;
    public final WidgetCache widgets;

    public Renderer(RuleSet ruleSet,File[] roots,String defaultStyle,String[] use,int limit, Map<String,String> redirects) {
        this.ruleSet = ruleSet;
        files=new FileCache(roots,limit,redirects);
        documents=new DocumentCache(files);
        widgets=new WidgetCache(documents,ruleSet);
        this.defaultStyle=defaultStyle;
        this.defaultWidgets=use;


    }

    public Renderer(File base,Element elt) {
        this(new RuleSet(new JavaNamespaceRule(Renderer.class.getClassLoader())),base,elt);
    }

    public Renderer(RuleSet ruleSet,File base,Element elt) {
        this(ruleSet,
                getRelativeFiles(base, elt.getAttribute("root").split(",")),
                elt.getAttribute("standard"),
                elt.getAttribute("use").split(","),
                StringUtils.parseInt(elt.getAttribute("limit"),1048576),
                getRedirectMap(elt));
    }

    private static File[] getRelativeFiles(File base, String[] names) {
        File[] files=new File[names.length];
        for (int i = 0; i < names.length; i++) files[i]=IOUtils.relativeFile(base, names[i]);
        return files;
    }

    private static Map<String, String> getRedirectMap(Element elt) {
        Map<String,String> map=new HashMap<String,String>();
        NodeList elts = elt.getElementsByTagName("redirect");
        for (int i=0;i<elts.getLength();i++) {
            Element e = (Element) elts.item(i);
            map.put(e.getAttribute("prefix"),e.getFirstChild().getNodeValue());
        }
        return map;
    }


    public CachedFileData getFile(String path) throws IOException {
        return files.getFile(path);
    }


    public String defaultStyle;
    public String[] defaultWidgets;


    public void flush() {
        documents.clear();
        widgets.clear();
        files.clear();

    }

    public Render getHTML() throws IOException, ProcessingException {
        return getRender().html();
    }



    public Render getXML() throws IOException, ProcessingException {
        return getRender().xml();
    }

    public Render getText(String mimetype) throws IOException, ProcessingException {
        return getRender().text(mimetype);
    }

    public Render getRender() throws IOException, ProcessingException {
        Render render=new Render(documents, widgets, ruleSet);

        for (String widgets : defaultWidgets) render.use(widgets);
        return render;
    }


}
