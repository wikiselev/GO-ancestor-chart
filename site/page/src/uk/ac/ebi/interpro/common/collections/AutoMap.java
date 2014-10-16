package uk.ac.ebi.interpro.common.collections;

import uk.ac.ebi.interpro.common.*;

import java.util.*;

public class AutoMap<K,V> extends ProxyMap<K,V> {
    private Creator.KeyFactory<K,V> factory;


    public AutoMap() {
        super(new HashMap<K,V>());
    }

    public AutoMap(Map<K,V> base, Creator.KeyFactory<K,V> kf) {
        super(base);
        this.factory = kf;
    }

    public AutoMap(Map<K,V> base, final Creator.Factory<V> f) {
        this(base, new Creator.KeyFactory<K,V>() {
            public V make(Object key) {return f.make();}
        });
    }

    public V get(Object key) {
        if (!base.containsKey(key)) {base.put((K)key, make((K) key));}
        return base.get(key);
    }

    public V make(K key) {
        return factory.make((K)key);
    }

    public static <K extends Enum<K>,V> AutoMap<K,V> enumMap(Class<K> k,Class<V> v,Object... constructorParams) {
        return new AutoMap<K,V>(new EnumMap<K,V>(k),Creator.reflective(v,k,constructorParams));
    }

    public static <K,V> AutoMap<K,V> hashMap(Class<K> k,Class<V> v) {
        return new AutoMap<K,V>(new HashMap<K,V>(),Creator.reflective(v,k));
    }
}
