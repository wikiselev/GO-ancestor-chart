package uk.ac.ebi.interpro.common.collections;

import uk.ac.ebi.interpro.common.*;

import java.lang.ref.*;
import java.util.*;

/**
 * SoftValueMap, a map that stores its values in soft references.
 *
 * Note that null values will not be stored in this map.
 *
 * Note also that key/value pairs may disappear from this map without warning!
 */



public class SoftValueMap<K,V> extends AbstractReferenceValueMap<K,V> {


    public SoftValueMap(Map<K,Reference<V>> underlying, Interval interval) {
        super(underlying, interval);
    }
    public SoftValueMap(Map<K,Reference<V>> underlying) {
        super(underlying);
    }
    public SoftValueMap(Interval interval) {
        super(interval);
    }
    public SoftValueMap() {
    }
    public V put(K key, V value) {
        return putReference(key, new SoftReference<V>(value));
    }
}
