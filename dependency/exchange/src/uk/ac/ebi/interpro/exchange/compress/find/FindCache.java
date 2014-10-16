package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public abstract class FindCache implements Find {
    int last=0;
    int next=-1;

    public static Find cache(final Find underlying) {
        return new FindCache(){
            public int forwards(int at) throws IOException {
                return underlying==null?at:underlying.next(at);
            }

            public Find[] getChildren() {
                return underlying==null?null:underlying.getChildren();
            }

            public BitReader getBitReader() {
                return underlying==null?null:underlying.getBitReader();
            }

            public String toString() {
                return underlying==null?"All":"Cached: "+underlying;
            }
        };
    }

    public abstract int forwards(int at) throws IOException;

    public int next(int at) throws IOException {
        if (at<last) throw new IOException("Backwards seek");
        last=at;
        if (at>next) next=forwards(at);
        return next;
    }



}
