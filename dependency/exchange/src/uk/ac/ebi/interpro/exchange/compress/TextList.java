package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public interface TextList {
    int search(String value) throws IOException;

    int size();

    String read(int i) throws IOException;
}
