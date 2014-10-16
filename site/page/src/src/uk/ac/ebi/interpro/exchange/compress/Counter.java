package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;
import java.util.*;

class Counter<X extends Comparable<X>> {
    int size;
    int code;
    int count;


    X data;

    Counter<X> c0, c1;


    public Counter() {
    }

    public Counter(X data) {

        this.data = data;
    }

    public Counter(Counter<X> c0, Counter<X> c1) {
        this.c0 = c0;
        this.c1 = c1;
        this.count = c0.count + c1.count;
    }

    /*public void write(BitWriter w, KompressorDeprecated.ValueOutput<X> o, int initial, int size, HuffmanInfo hi) throws IOException {
        this.size = size;
        this.code = initial;
        if (w != null && data != null) {
            if (size >= 31) {
                throw new IOException("Table too large");
            }
            w.write(size, 5);
            int bc = w.bitCount;
            o.write(w, data);

            //System.out.println(">"+size+":"+data);

            hi.valueSize += w.bitCount - bc;
            hi.sizeSize += 5;
            hi.encodingSize += size * count;
            hi.total += count;
            hi.distinct++;
        }

        if (c0 != null) c0.write(w, o, initial, size + 1, hi);
        if (c1 != null) c1.write(w, o, initial + (1 << size), size + 1, hi);


    }

*/

    public void info(HuffmanInfo hi,int valueSize) {
        hi.sizeSize += 5;
        hi.encodingSize += size * count;
        hi.total += count;
        hi.distinct++;
        hi.valueSize+=valueSize;
    }

    public void write(HuffmanInfo hi, Output<X> o,BitWriter w) throws IOException {
        w.write(size,5);
        //int bc1=w.bitCount();
        o.write(data);
        //info(hi,w.bitCount()-bc1);
        
    }

    public void computeCodeSize(int initial, int size) throws IOException {
        if (size>=31) throw new IOException("Code too large");
        this.size = size;
        this.code = initial;

        if (c0 != null) c0.computeCodeSize(initial, size + 1);
        if (c1 != null) c1.computeCodeSize(initial + (1 << size), size + 1);
    }

    public static <X extends Comparable<X>> Comparator<Counter<X>> valueComparator() {
        return new Comparator<Counter<X>>() {
            public int compare(Counter<X> c1, Counter<X> c2) {
                return c1.data.compareTo(c2.data);
            }
        };
    };

    public static <X extends Comparable<X>> Comparator<Counter<X>> sizeValueComparator() {
        return new Comparator<Counter<X>>() {

            public int compare(Counter<X> c1, Counter<X> c2) {
                int v=c1.size-c2.size;
                if (v!=0) return v;
                return c1.data.compareTo(c2.data);
            }
        };
    };

    public static <X extends Comparable<X>> Comparator<Counter<X>> countComparator() {
        return new Comparator<Counter<X>>() {

            public int compare(Counter<X> c1, Counter<X> c2) {
                return c1.count-c2.count;
            }
        };
    };

/*

    public int compareTo(Counter<X> o) {
        return count - o.count;
    }
*/
/*

    public boolean attach(int size, X data) {
        if (this.data != null) return false;
        if (size == 0) {
            if (c0 != null) return false;
            this.data = data;
            return true;
        }
        if (c0 == null) c0 = new Counter<X>();
        if (c0.attach(size - 1, data)) return true;
        if (c1 == null) c1 = new Counter<X>();
        return c1.attach(size - 1, data);
    }

*/
    public void print() {

        System.out.println(bitString(code, size) + " " + data + " " + count);
        if (c0 != null) c0.print();
        if (c1 != null) c1.print();
    }

    static String bitString(int code, int size) {
            StringBuilder sb = new StringBuilder();
        bitString(code, size, sb);
        return sb.toString();
        }

    private static void bitString(int code, int size, StringBuilder sb) {
        while (size > 0) {
            sb.insert(0,code & 1);
            size--;
            code >>= 1;
        }
    }

    public static String bitString(byte[] data) {
        StringBuilder sb=new StringBuilder();
        for (byte b : data) {
            bitString(b,8,sb);
        }
        return sb.toString();
    }

    public static <X extends Comparable<X>> Counter<X> attach(int size,Counter<X> target,Counter<X> c) {
        if (c.size==size) return c;
        int x=(c.code>>(size)) & 1;
        if (target==null) target=new Counter<X>();
        if (x==0) target.c0=attach(size+1,target.c0,c);
        else target.c1=attach(size+1,target.c1,c);
        return target;        
    }


    public String toString() {
        return data!=null?data.toString():"("+(c0!=null?c0.toString():"-")+","+(c1!=null?c1.toString():"-")+")";
    }


    public void readValue(HuffmanInfo hi,BitReader br,Input<X> in) throws IOException {
        long bc=br.bitCount();
        data=in.read();

        info(hi, (int) (br.bitCount()-bc));
    }


}
