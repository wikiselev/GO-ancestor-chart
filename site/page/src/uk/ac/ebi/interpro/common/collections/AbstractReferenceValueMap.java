/*
 * Created by IntelliJ IDEA.
 * User: david
 * Date: 02-Sep-2002
 * Time: 12:27:56
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package uk.ac.ebi.interpro.common.collections;

import uk.ac.ebi.interpro.common.*;

import java.util.*;
import java.lang.ref.*;

/**
 * AbstractReferenceValueMap, a map that stores its values in references.
 * This implementation does not implement put, and so does not actually create references.
 * Concrete implementations are present in WeakValueMap and SoftValueMap.
 *
 * Note that null values may not be stored in this map.
 */

public abstract class  AbstractReferenceValueMap<K,V> extends AbstractMap<K,V> {

    AbstractReferenceValueMap(Map<K,Reference<V>> underlying, Interval interval) {
        if (underlying==null) {underlying=new HashMap<K, Reference<V>>();}
        this.underlying=underlying;
        if (interval!=null) this.interval=interval.getMillis();
    }

    AbstractReferenceValueMap(Map<K,Reference<V>> underlying) {
        this(underlying,null);
    }

    AbstractReferenceValueMap(Interval interval) {
        this(null,interval);
    }

    AbstractReferenceValueMap() {
        this(null,null);
    }
    
    protected Map<K,Reference<V>> underlying;


    // All the methods not connected with values are implemented
    // directly via the underlying map
    // Others are implemented by the super methods in AbstractMap which
    // (deeply inefficiently) calls the entrySet method.


    public int size() {
        return underlying.size();
    }

    public boolean isEmpty() {
        return underlying.isEmpty();
    }

    public V remove(Object key) {
        return underlying.remove(key).get();
    }


    public void clear() {
        underlying.clear();
    }

    public Set<K> keySet() {
        return underlying.keySet();
    }

    public boolean containsKey(Object key) {
        return get(key)!=null;
    }



    class EntrySet<K,V> extends AbstractSet<Entry<K,V>> {

        public Iterator<Entry<K,V>> iterator() {

            final Iterator<Entry<K, Reference<V>>> underlyingIterator = (Iterator<Entry<K, Reference<V>>>)(Iterator)underlying.entrySet().iterator();

            return new Iterator<Entry<K,V>>() {

                public boolean hasNext() {
                    return underlyingIterator.hasNext();
                }

                public Entry<K, V> next() {
                    Entry<K,Reference<V>> e = underlyingIterator.next();
                    final K k=e.getKey();
                    final V v=e.getValue().get();
                    return new Entry<K,V>() {
                        public K getKey() {return k;}
                        public V getValue() {return v;}
                        public V setValue(V o) {throw new UnsupportedOperationException("indirect modification");}
                    };
                }
                public void remove() {
                    underlyingIterator.remove();
                }
            };
        }

        public int size() {
            return AbstractReferenceValueMap.this.size();
        }
    }

    EntrySet<K,V> entrySet=new EntrySet<K,V>();

    public Set<Entry<K,V>> entrySet() {
        return entrySet;
    }

    public V get(Object key) {
        if (!underlying.containsKey(key)) return null;
        V o = underlying.get(key).get();
        if (o==null) {remove(key);return null;}
        return o;
    }

    protected V putReference(K key,Reference<V> ref) {
        V previous=get(key);
        underlying.put(key,ref);
        return previous;
    }

    public void removeOld() {
        for (Iterator<Entry<K, V>> it = entrySet.iterator(); it.hasNext();) {
            Entry<K, V> o = it.next();
            if (o.getValue()==null) it.remove();
        }
    }


    private long interval=-1;
    private long previous;
    public void vacuum() {
        long now = System.currentTimeMillis();
        if (now - previous > interval) {
            previous = now;
            removeOld();
        }
    }

    public Map<K, V> materialize() {
        Map<K, V> map=new HashMap<K, V>();
        for (Iterator<Entry<K, V>> it = entrySet.iterator(); it.hasNext();) {
            Entry<K, V> o = it.next();
            K k = o.getKey();
            V v = o.getValue();
            if (v ==null) it.remove();
            else map.put(k,v);
        }
        return map;
    }



}
