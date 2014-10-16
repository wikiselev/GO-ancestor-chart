package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;

class NoFilter implements Filter {

    public Find open(DataFiles df, List<Closeable> connection) throws IOException {
        return new All(df.annotations.rowCount);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    public int hashCode() {
        return 0;
    }


    public String toString() {
        return "*";
    }
}
