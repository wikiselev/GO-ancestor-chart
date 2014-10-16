package uk.ac.ebi.interpro.common.collections;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.lang.ref.*;
import java.util.*;

public class WeakValueMap<K,V> extends AbstractReferenceValueMap<K,V> {


    public WeakValueMap(Map<K, Reference<V>> underlying, Interval interval) {
        super(underlying, interval);
    }
    public WeakValueMap(Map<K, Reference<V>> underlying) {
        super(underlying);
    }
    public WeakValueMap(Interval interval) {
        super(interval);
    }
    public WeakValueMap() {
    }
    public V put(K key, V value) {
        return putReference(key, new WeakReference<V>(value));
    }

    // test that weak maps work
    public static void main(String[] args) {

        WeakValueMap<String, RCO> m=new WeakValueMap<String, RCO>();
        m.put("x",new RCO());
        MutableInteger i= RCO.refCounts.get(RCO.class.getName());
        System.out.println(new HashMap<String,Object>(m).size()+" "+i);
        System.gc();
        System.out.println(m.keySet().size()+" "+i);
        m.removeOld();
        System.out.println(m.keySet().size()+" "+i);

    }

}
