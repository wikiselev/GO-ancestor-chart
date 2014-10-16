package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;

public class Expand implements Filter {
    final Filter underlying;

    public Expand(Filter underlying) {
        this.underlying = underlying;
    }

    public String toString() {
        return "~("+underlying+")";
    }

    public Find open(DataFiles df, List<Closeable> connection) throws IOException {
        return new RepeatedKeyFilter(
                df.proteinAnnotationCounts,null,
                underlying.open(df,connection),null
        );
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return underlying.equals(((Expand) o).underlying);

    }

    public int hashCode() {
        return underlying.hashCode();
    }
}
