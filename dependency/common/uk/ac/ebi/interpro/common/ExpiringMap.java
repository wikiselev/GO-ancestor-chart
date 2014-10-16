package uk.ac.ebi.interpro.common;

import java.util.*;

public class ExpiringMap implements Map {



    Map underlying;
    Set protect = Collections.synchronizedSet(new HashSet());

    public int hashCode() {return underlying.hashCode();}
    public int size() {return underlying.size();}
    public void clear() {underlying.clear();}
    public boolean isEmpty() {return underlying.isEmpty();}
    public boolean containsKey(Object key) {return underlying.containsKey(key);}
    public boolean containsValue(Object value) {return underlying.containsValue(value);}
    public boolean equals(Object o) {return underlying.equals(o);}
    public Collection values() {return underlying.values();}
    public void putAll(Map t) {
        protect.addAll(t.keySet());
        underlying.putAll(t);
    }
    public Set entrySet() {return underlying.entrySet();}
    public Set keySet() {return underlying.keySet();}
    public Object get(Object key) {return underlying.get(key);}
    public Object remove(Object key) {return underlying.remove(key);}
    public Object put(Object key, Object value) {
        protect.add(key);
        return underlying.put(key, value);
    }

    public void expire() {
        underlying.keySet().retainAll(protect);
        protect.clear();
    }

    public TimerTask autoExpire() {
        return new TimerTask() {
            public void run() {expire();}
        };
    }

    public ExpiringMap(Map underlying) {
        this.underlying = underlying;
    }

    public ExpiringMap() {
        this(Collections.synchronizedMap(new HashMap()));
    }

}
