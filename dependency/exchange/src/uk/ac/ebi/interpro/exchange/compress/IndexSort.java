package uk.ac.ebi.interpro.exchange.compress;

import java.util.*;

public class IndexSort<X> {



    public static final CollatingStringComparator caseless=new CollatingStringComparator(upper());

    public static int[] upper() {
        int[] u=new int[65536];
        for (int i = 0; i < u.length; i++) u[i]=Character.toUpperCase((char)i);
        return u;
    }

    public static class CollatingStringComparator implements Comparator<String> {
        public int[] collation;


        public CollatingStringComparator(int[] collation) {
            this.collation = collation;
        }

        public int compare(String s1, String s2) {
            for (int i = 0; i < s1.length(); i++) {
                if (i>=s2.length()) return 1;
                int v=collation[s1.charAt(i)]-collation[s2.charAt(i)];
                if (v!=0) return v;
            }
            if (s1.length()==s2.length()) return 0;
            return -1;
        }
    }

    public IndexSort() {
        map = new TreeMap<X, Integer>();
    }

    public IndexSort(Comparator<X> comparator) {
        map = new TreeMap<X, Integer>(comparator);
    }

    public Map<X, Integer> map;

    public int add(X x) {
        int v=search(x);
        if (v>=0) return v;
        map.put(x, v = map.size());
        return v;
    }

    public int search(X x) {
        Integer v = map.get(x);
        return v==null?-1:v;
    }

    /**
     * Get the a list of sorted indexes, in the original index order
     *
     * @return list of sorted indexes
     */

    public int[] indexesTranslate() {
        int[] order = new int[map.size()];
        Arrays.fill(order, -1);
        int i = 0;
        for (Integer code : map.values()) order[code] = i++;
        return order;
    }

    /**
     * Get the a list of the original indexes, sorted by value.
     *
     * @return list of original indexes
     */
    public int[] sortedIndexes() {
        int[] order = new int[map.size()];
        Arrays.fill(order, -1);
        int i = 0;
        for (Integer code : map.values()) order[i++] = code;
        return order;
    }

    public Set<X> sortedValues() {
        return map.keySet();
    }

    public int size() {return map.size();}
}
