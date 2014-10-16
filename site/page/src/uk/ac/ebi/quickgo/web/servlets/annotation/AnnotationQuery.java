package uk.ac.ebi.quickgo.web.servlets.annotation;

import java.util.*;

public class AnnotationQuery {
    final String[] db;
    final String[] slimIDs;
    final String slimTypes;
	final String[] proteins;
    final Filter filter;

    public AnnotationQuery(Filter filter) {
        this(null, null, null, null, filter);
    }

    public AnnotationQuery(String[] db, String[] slimIDs, String slimTypes, String[] proteins, Filter filter) {
        this.db = db;
        this.slimIDs = slimIDs;
        this.slimTypes = slimTypes;
	    this.proteins = proteins;
        this.filter = filter;
    }

    public AnnotationQuery() {
        this(null, null, null, null, new NoFilter());

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationQuery that = (AnnotationQuery) o;

        if (!Arrays.equals(db, that.db)) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (!Arrays.equals(slimIDs, that.slimIDs)) return false;
        if (slimTypes != null ? !slimTypes.equals(that.slimTypes) : that.slimTypes != null) return false;
	    if (!Arrays.equals(proteins, that.proteins)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = db != null ? Arrays.hashCode(db) : 0;
        result = 31 * result + (slimIDs != null ? Arrays.hashCode(slimIDs) : 0);
        result = 31 * result + (slimTypes != null ? slimTypes.hashCode() : 0);
        result = 31 * result + (proteins != null ? Arrays.hashCode(proteins) : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }
}
