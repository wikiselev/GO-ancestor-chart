package uk.ac.ebi.interpro.common.collections;

import java.util.*;

public class ProxyMap<K, V> implements Map<K, V> {
    protected Map<K, V> base;
    public ProxyMap(Map<K, V> base) {this.base = base;}
    public int size() {return base.size();}
    public V get(Object o) {return base.get(o);}
    public boolean isEmpty() {return base.isEmpty();}
    public boolean containsKey(Object o) {return base.containsKey(o);}
    public boolean containsValue(Object o) {return base.containsValue(o);}
    public V put(K k, V v) {return base.put(k, v);}
    public V remove(Object o) {return base.remove(o);}
    public void putAll(Map<? extends K, ? extends V> map) {base.putAll(map);}
    public void clear() {base.clear();}
    public Set<K> keySet() {return base.keySet();}
    public Collection<V> values() {return base.values();}
    public Set<Entry<K, V>> entrySet() {return base.entrySet();}
    public boolean equals(Object o) {return base.equals(o);}
    public int hashCode() {return base.hashCode();}
}
