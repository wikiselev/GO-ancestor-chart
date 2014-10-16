package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class None implements Find {
    public int next(int at) throws IOException {return Integer.MAX_VALUE;}

    public Find[] getChildren() {
        return null;
    }

    public BitReader getBitReader() {
        return null;
    }

    public String toString() {
        return "None";
    }
}
