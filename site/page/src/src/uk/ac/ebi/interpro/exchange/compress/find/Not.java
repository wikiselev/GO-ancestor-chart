package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class Not implements Find {
    private final int max;
    private final Find underlying;

    public Not(int max, Find underlying) {
        this.max = max;
        this.underlying = underlying;
    }

    public int next(int at) throws IOException {
        if (at>= max) return Integer.MAX_VALUE;
        while (underlying.next(at)==at) at++;
        return at;
    }

    public Find[] getChildren() {
        return new Find[]{underlying};
    }

    public BitReader getBitReader() {
        return null;
    }
}
