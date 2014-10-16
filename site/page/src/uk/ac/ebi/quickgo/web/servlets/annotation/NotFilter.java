package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;

public class NotFilter implements Filter {
    final Filter underlying;

    public NotFilter(Filter underlying) {
        this.underlying = underlying;
    }

    public String toString() {
        return "!("+underlying+")";
    }

    public Find open(DataFiles df, List<Closeable> connection) throws IOException {
        return new Not(df.annotations.rowCount,underlying.open(df, connection));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return underlying.equals(((NotFilter) o).underlying);

    }

    public int hashCode() {
        return underlying.hashCode();
    }
}
