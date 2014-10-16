package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public interface Output<X> {
    void write(X x) throws IOException;
    void close() throws Exception;
}
