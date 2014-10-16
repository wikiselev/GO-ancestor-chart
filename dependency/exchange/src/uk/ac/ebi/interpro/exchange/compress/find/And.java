package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;
import java.util.*;

public class And implements Find {
    Find[] component;
    public int next(int rownum) throws IOException {
        //System.out.println("AND from "+rownum);
        while (true) {
            //System.out.println("AND try "+rownum);
            int next=rownum;
            for (Find r : component) {
                next=r.next(next);
                //System.out.println("> "+next);
            }
            if (next==rownum) break;
            rownum=next;
        }
        //System.out.println("AND to "+rownum);
        return rownum;
    }

    public Find[] getChildren() {
        return component;
    }

    public BitReader getBitReader() {
        return null;
    }

    public And(Find... component) {
        this.component = component;
    }

    public And(Collection<Find> component) {
        this.component = component.toArray(new Find[component.size()]);
    }

    public static Find and(List<Find> options) {
        if (options==null) return null;

        List<Find> all=new ArrayList<Find>();
        for (Find o : options) {
            if (o==null) continue;
            if (o instanceof And) all.addAll(Arrays.asList(((And)o).component));
            all.add(o);
        }
        if (all.isEmpty()) return null;
        if (all.size()==1) return all.get(0);
        return new And(all);
    }

    public static Find and(Find... options) {
        if (options==null) return null;
        return and(Arrays.asList(options));
    }

    public String toString() {
        return "And";
    }
}
