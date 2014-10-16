package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public interface Input<X> {
    X read() throws IOException;
}
