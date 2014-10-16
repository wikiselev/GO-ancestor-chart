package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;
import java.util.*;

public class Or implements Find {
    Find[] component;
    public int next(int from) throws IOException {
        //System.out.println("OR from "+from);
        int rownum=Integer.MAX_VALUE;
        for (Find r : component) {
            int next=r.next(from);
            //System.out.println("> "+next);
            if (next<rownum) rownum=next;
        }
        //System.out.println("OR to "+rownum);
        return rownum;
    }

    public Or(Find... component) {
        this.component = component;
    }

    public <F extends Find> Or(Collection<F> component) {
        this.component = component.toArray(new Find[component.size()]);
    }

    public static <F extends Find> Find or(List<F> options) {
        if (options==null) return null;

        List<Find> all=new ArrayList<Find>();
        for (Find o : options) {
            if (o==null) continue;
            if (o instanceof Or) all.addAll(Arrays.asList(((Or)o).component));
            all.add(o);
        }
        if (all.isEmpty()) return null;
        if (all.size()==1) return all.get(0);
        return new Or(all);
    }

    public static Find or(Find... options) {
        if (options==null) return null;
        return or(Arrays.asList(options));

    }

    public Find[] getChildren() {
        return component;
    }

    public BitReader getBitReader() {
        return null;
    }


    public String toString() {
        return "Or";
    }
}
