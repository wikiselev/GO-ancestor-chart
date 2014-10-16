package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class All implements Find {
    private final int max;

    public All(int max) {this.max = max;}

    public int next(int at) throws IOException {return at>= max ?Integer.MAX_VALUE:at;}

    public Find[] getChildren() {
        return null;
    }

    public BitReader getBitReader() {
        return null;
    }

    @Override
    public String toString() {
        return "All " +
                "max=" + max;
    }
}
