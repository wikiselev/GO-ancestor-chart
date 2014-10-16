package uk.ac.ebi.interpro.common;

import java.util.*;

public class URLGenerator {
    Map<String,String> parameters;

    String path;


    public URLGenerator(String path,Map<String, String> parameters) {
        this.parameters = parameters;

        this.path = path;
    }

    public URLGenerator setParameter(String name, String value) {
        if (name != null)
            parameters.put(name, value);
        return this;
    }

    public URLGenerator setPath(String path) {
        this.path=path;
        return this;
    }

    public URLGenerator remove(String name) {
        parameters.remove(name);
        return this;
    }

    public String toString() {
        return URLUtils.encodeURL(path,parameters);

    }
    public URLGenerator copy() {
        return new URLGenerator(path,new HashMap<String,String>(parameters));
    }

    public URLGenerator(String URI,String queryString) {
        this(URI,URLUtils.decodeURLLast(queryString));        

    }

    public URLGenerator(String URI) {
        this(URLUtils.queryPrefix(URI),URLUtils.queryString(URI));

    }
}
