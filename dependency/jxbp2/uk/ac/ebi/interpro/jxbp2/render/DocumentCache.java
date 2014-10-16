package uk.ac.ebi.interpro.jxbp2.render;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.util.*;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.xml.*;
import uk.ac.ebi.interpro.common.performance.*;

public class DocumentCache {

    private static Location me=new Location();

    FileCache files;
    DocumentFactory df=new DocumentFactory();


    public DocumentCache( FileCache files) {    
        this.files = files;
        df.setWhiteSpaceCompress(true);
    }

    public Document loadDocument(String key) throws IOException {
        Action a=me.start("Parse "+key);
        CachedFileData file=files.getFile(key);
        if (!file.isFile) throw new IOException("File not readable "+key);
        ByteArrayInputStream is=new ByteArrayInputStream(file.content);
        //InputStream is = new FileInputStream(key);
        try {

            return df.load(new InputSource(is),key);
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException)e;
            IOException ioe = new IOException("Unable to parse file");
            ioe.initCause(e);
            throw ioe;
        } finally {
            is.close();
            me.stop(a);
        }

    }

    public void clear() {
        documentArchive.clear();
    }

    public class CachedDocument implements Closeable {
        public Document document;
        String key;

        public CachedDocument(Document document, String key) {
            this.document = document;
            this.key = key;
        }

        public void close() throws IOException {
            synchronized(documentArchive) {
                documentArchive.get(key).add(this);
            }
        }
    }


    public final Map<String, List<CachedDocument>> documentArchive = CollectionUtils.arrayListHashMap();

    public CachedDocument borrowDocument(String key) throws IOException {

        synchronized(documentArchive) {
            List<CachedDocument> available= documentArchive.get(key);
            if (!available.isEmpty()) return available.remove(0);
        }
        return new CachedDocument(loadDocument(key),key);
    }



}
