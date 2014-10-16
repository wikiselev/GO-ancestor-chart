package uk.ac.ebi.interpro.jxbp2.render;

import uk.ac.ebi.interpro.common.*;

import java.io.*;
import java.util.*;
import java.net.*;

public class FileCache {

    long startTime=System.currentTimeMillis();

    File[] roots;
    int limit=-1;
    Map<String,String> urlRedirects=new HashMap<String,String>();

    public FileCache(File[] roots, int limit, Map<String, String> urlRedirects) {
        this.roots = roots;
        this.limit = limit;
        this.urlRedirects = urlRedirects;
    }

    public FileCache(File root, int limit, Map<String, String> urlRedirects) {
        this.roots = new File[]{root};
        this.limit = limit;
        this.urlRedirects = urlRedirects;
    }

    int size=0;

    public int size() {return size;}
    public int count() {return files.size();}


    public FileCache(File root) {
        this.roots = new File[]{root};
    }

    public final Map<String, CachedFileData> files=new HashMap<String, CachedFileData>();



    public CachedFileData getFile(String page) throws IOException {
        synchronized(files) {
            if (files.containsKey(page)) return files.get(page).touch();
            CachedFileData data = find(page);
            files.put(page,data);
            sizeLimit();
            return data;
        }
    }

    private CachedFileData find(String page) throws IOException {

        for (String pfx : urlRedirects.keySet()) {
            if (page.startsWith(pfx)) {
                return new CachedFileData(new URL(urlRedirects.get(pfx)+page.substring(pfx.length())),page,startTime);
            }
        }

        for (File root : roots) {
            File f=new File(root,page);
            if (f.exists()) return  new CachedFileData(f,page,startTime);
        }

        return new CachedFileData(page,startTime);

    }



    private void sizeLimit() {
        if (limit<0) return;
        List<CachedFileData> all=new ArrayList<CachedFileData>(files.values());
        Collections.sort(all,CachedFileData.lastUsedOrder);
        int total=0;
        for (CachedFileData cfd : all) {
            int s = cfd.size();
            if (total+ s >limit) files.remove(cfd.key);
            else total+=s;
        }
        size=total;
    }

    public void clear() {
        files.clear();
        startTime=System.currentTimeMillis();
    }
}
