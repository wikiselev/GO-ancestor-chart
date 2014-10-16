package uk.ac.ebi.interpro.common.collections;
import java.util.*;

public class MapProperties extends Properties {
    protected Map<String, String> base;
    public Enumeration propertyNames() {return new IteratorEnumeration(keySet().iterator());}
    public String getProperty(String key, String alternative) {return containsKey(key)?get(key):alternative;}
    public String getProperty(String key) {return get(key);}
    public Object setProperty(String key, String value) {return put(key,value);}
    public Enumeration keys() {return new IteratorEnumeration(keySet().iterator());}
    public Enumeration elements() {return new IteratorEnumeration(entrySet().iterator());}
    public boolean contains(Object o) {return containsValue(o);}
    public MapProperties(Map<String, String> base) {this.base = base;}
    public int size() {return base.size();}
    public String get(Object o) {return base.get(o);}
    public boolean isEmpty() {return base.isEmpty();}
    public boolean containsKey(Object key) {return base.containsKey(key);}
    public boolean containsValue(Object value) {return base.containsValue(value);}
    public Object put(Object key, Object value) {return base.put((String) key, (String) value);}
    public String remove(Object key) {return base.remove(key);}
    public void putAll(Map t) {base.putAll(t);}
    public void clear() {base.clear();}
    public Set keySet() {return base.keySet();}
    public Collection values() {return base.values();}
    public Set entrySet() {return base.entrySet();}
    public boolean equals(Object o) {return base.equals(o);}
    public int hashCode() {return base.hashCode();}
}
