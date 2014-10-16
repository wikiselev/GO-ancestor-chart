package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.*;

import java.util.*;
import java.lang.ref.*;
import java.io.*;

/**
 * Reference Counted Object
 */
public class RCO {
    public static Map<String, MutableInteger> refCounts = new HashMap<String, MutableInteger>();
//    public static Map<String, Set<WeakReference>> references =
//            new AutoMap<String, Set<WeakReference>>(
//                    new HashMap<String, Set<WeakReference>>(),
//                    Creator.<Set<WeakReference>>reflective((Class<Set<WeakReference>>) (Class) HashSet.class)
//            );

    public static Map<String, Set<WeakReference>> references =null;

    private MutableInteger mi;
    private WeakReference self;
    private String refName;

    public RCO() {
        set(this.getClass(), null);
    }

    public RCO(String name) {
        set(this.getClass(), name);
    }

    public RCO(Class<? extends Object> clazz) {
        set(clazz, null);
    }

    public RCO(Class<? extends Object> clazz, String name) {
        set(clazz, name);

    }

    private void set(Class clazz, String name) {
        if (name==null) refName=clazz.getName();
        else
            refName = clazz.getName() +"+" + name;
        synchronized (refCounts) {
            mi = refCounts.get(refName);
            if (mi == null) refCounts.put(refName, mi = new MutableInteger());
            if (references!=null) references.get(refName).add(self = new WeakReference(this));
            mi.i++;
        }
    }


    protected void finalize() throws Throwable {
        synchronized (refCounts) {
            mi.i--;
            if (references!=null) references.get(refName).remove(self);
        }
        super.finalize();
    }


}
