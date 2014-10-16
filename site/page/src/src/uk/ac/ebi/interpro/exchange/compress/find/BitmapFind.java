package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class BitmapFind implements Find {
    BitmaskFilter filter;

    public int next(int at) throws IOException {
        while (at<filter.count && !filter.test(at)) {
            at++;
        }
        if (at>=filter.count) return Integer.MAX_VALUE;
        return at;
    }

    public BitmapFind(BitmaskFilter filter) {
        this.filter = filter;
    }

    public Find[] getChildren() {
        return null;
    }

    public BitReader getBitReader() {
        return null;
    }

    @Override
    public String toString() {
        return "BitmapFind";
    }
}
