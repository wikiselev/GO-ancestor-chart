package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public interface Find {
    int next(int at) throws IOException;
    Find[] getChildren();
    BitReader getBitReader();
    String toString();
}
