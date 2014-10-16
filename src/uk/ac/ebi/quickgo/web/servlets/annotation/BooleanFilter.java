package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.util.*;
import java.io.*;

public class BooleanFilter implements Filter {
    enum BooleanCombination {or("|"),and("&"),like("~");
        String op;
        BooleanCombination(String op) {
            this.op=op;
        }
    }
    final BooleanCombination combination;
    final Filter[] options;


    public BooleanFilter(BooleanCombination combination, Filter... options) {
        this.combination = combination;
        this.options = options;
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        for (Filter option : options) {
            if (sb.length()>0) sb.append(" ").append(combination.op).append(" ");
            sb.append("(").append(option.toString()).append(")");
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanFilter that = (BooleanFilter) o;
        return combination == that.combination && Arrays.equals(options, that.options);
    }

    public int hashCode() {
        return Arrays.hashCode(options)*31+ combination.hashCode();
    }



    public Find open(DataFiles df, List<Closeable> connection) throws IOException {
        Find[] components=new Find[options.length];
        for (int i = 0; i < options.length; i++) components[i]=options[i].open(df,connection);
        switch (combination) {
        case and:return new And(components);
        case or:return new Or(components);
        case like:return like(components, df);
        }
        return null;
    }

    private Find like(Find[] components, DataFiles df) {
        Find result=components[0];
        for (int i=1;i<components.length;i++) {
            result=new RepeatedKeyFilter(
                    df.proteinAnnotationCounts,
                    result,components[i],null);
        }
        return result;
    }

    static Filter simplify(BooleanCombination combination,List<Filter> options) {

        while (options.remove(null));
        if (options.isEmpty()) return null;
        if (options.size()==1) return options.get(0);
        return new BooleanFilter(combination, (Filter[]) options.toArray(new Filter[options.size()]));
    }

    static Filter simplify(BooleanCombination combination,Filter... filters) {
        return simplify(combination,new ArrayList(Arrays.asList(filters)));
    }
}
