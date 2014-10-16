package uk.ac.ebi.interpro.common.collections;

import java.util.*;

class IteratorEnumeration implements Enumeration {
    private final Iterator it;
    public IteratorEnumeration(Iterator it) {
        this.it = it;
    }
    public boolean hasMoreElements() {
        return it.hasNext();
    }
    public Object nextElement() {
        return it.next();
    }
}
