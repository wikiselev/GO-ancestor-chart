package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;

public interface Filter {
    Find open(DataFiles df, List<Closeable> connection) throws IOException;
    String toString();
    int hashCode();
    boolean equals(Object o);
}
