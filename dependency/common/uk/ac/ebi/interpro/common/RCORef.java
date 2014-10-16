package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.performance.*;


/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 28-Oct-2003
 * Time: 11:28:44
 * To change this template use Options | File Templates.
 */
public class RCORef extends RCO {
    private Object ref;

    public Object get() {
        return ref;
    }

    public RCORef(Object ref,String name) {
        super(name);
        this.ref = ref;
    }
}
