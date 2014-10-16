package uk.ac.ebi.quickgo.web.render;

import uk.ac.ebi.interpro.common.http.*;
import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.quickgo.web.data.*;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

public class JSON {

    List<Object> protection = new ArrayList<Object>() {
        public boolean contains(Object o) {
            for (Object i : this) if (i == o) return true;
            return false;
        }
    };

    private String callback;
    HTTPResponse p;
    Writer wr;

    public JSON(String callback) throws IOException {
        this.callback = callback;

        p = new HTTPResponse("text/javascript");

        wr = p.getWriter();
        if (callback != null) wr.write(callback + "(");


    }

    public HTTPResponse render(Object x) throws IOException {
        write(x);
        if (callback != null) wr.write(")");
        wr.flush();
        return p;
    }

    class CircularityException extends IOException {

        public CircularityException(String string) {
            super(string);
        }
    }

    void write(Object x) throws IOException {
        if (x == null) wr.write("null");
        else {

            if (protection.contains(x)) {
                StringBuilder sb = new StringBuilder();
                for (Object o : protection) sb.append(o.getClass().getCanonicalName()).append(" ");
                throw new CircularityException("Data circularity " + sb.toString());
            }
            protection.add(x);
            if (x instanceof JSONSerialise) writeInternal(((JSONSerialise) x).serialise());
            else writeInternal(x);
            protection.remove(protection.size() - 1);
        }
    }

    private void writeInternal(Object x) throws IOException {
        if (x==null) wr.write("null");
        else if (x.getClass().isArray() && !x.getClass().getComponentType().isPrimitive()) writeList(Arrays.asList((Object[]) x));
        else if (x instanceof String) writeString((String) x);
        else if (x instanceof Color) writeString("");
        else if (x instanceof Number) writeText(x.toString());
        else if (x instanceof Map) writeMap((Map<?,?>) x);
        else if (x instanceof Collection) writeList((Collection<?>) x);
		else if (x instanceof Boolean) writeText((Boolean)x?"true":"false");
        else writeIntrospect(x);
    }

    private void writeString(String x) throws IOException {
        wr.write("\"");
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            if (c == '\\') wr.write("\\\\");
            else if (c == '"') wr.write("\\\"");
            else wr.write(c);
        }
        wr.write("\"");
    }


    private void writeText(String x) throws IOException {
        wr.write(x);        
    }

    private void writeIntrospect(Object x) throws IOException {
        wr.write("{");
        int fc = 0;
        Field[] f = x.getClass().getDeclaredFields();
        for (Field field : f) {

            if (Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) continue;
            Object v;
            try {
                v = field.get(x);
            } catch (IllegalAccessException e) {

                continue;
            }
            if (fc++ != 0) wr.write(",");
            wr.write("\"" + field.getName() + "\" : ");

            write(v);
        }
        wr.write("}");
    }

    void writeMap(Map<?,?> map) throws IOException {
        wr.write("{");
        int fc = 0;
        for (Object key : map.keySet()) {
            Object v= map.get(key);

            if (fc++ != 0) wr.write(",");
            wr.write("\"" + key + "\" : ");

            write(v);
        }
        wr.write("}");
    }

    void writeList(Collection<?> l) throws IOException {
        wr.write("[");
        int i = 0;
        for (Object v : l) {
            if (i++ != 0) wr.write(",");
            write(v);
        }
        wr.write("]");
    }

    

}
