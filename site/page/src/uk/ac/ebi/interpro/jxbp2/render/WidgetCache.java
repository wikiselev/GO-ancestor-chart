package uk.ac.ebi.interpro.jxbp2.render;

import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.*;

public class WidgetCache {

    private static Location me=new Location();

    DocumentCache documents;
    RuleSet ruleSet;


    public WidgetCache(DocumentCache documents, RuleSet ruleSet) {
        this.documents = documents;
        this.ruleSet = ruleSet;
    }
    public void clear() {
        widgetArchive.clear();
    }

    public class CachedWidgets implements Closeable {
        Widgets widgets;
        String key;

        public CachedWidgets(Widgets widgets, String key) {
            this.widgets = widgets;
            this.key = key;
        }

        public void close() throws IOException {
            synchronized(widgetArchive) {
                widgetArchive.get(key).add(this);
            }
        }
    }

    public final Map<String, List<CachedWidgets>> widgetArchive = CollectionUtils.arrayListHashMap();

    public CachedWidgets borrowWidgets(String page) throws IOException, ProcessingException {
        CachedWidgets d=null;
        synchronized(widgetArchive) {
            List<CachedWidgets> available= widgetArchive.get(page);
            if (!available.isEmpty()) d=available.remove(0);
        }
        if (d==null) d= new CachedWidgets(loadWidgets(page), page);
        return d;
    }

    private Widgets loadWidgets(String page) throws ProcessingException, IOException {

        Action a=me.start("Make widgets "+page);
        Widgets w=new Widgets();
        ObjectProcessor processor=new ObjectProcessor(ruleSet);
        processor.bind(w);
        processor.process(documents.loadDocument(page).getDocumentElement());
        me.stop(a);
        return w;
    }



}
