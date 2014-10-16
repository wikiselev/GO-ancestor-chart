package uk.ac.ebi.interpro.exchange.compress;

import java.util.*;
import java.io.*;

class HuffmanTable<X extends Comparable<X>> implements Iterable<X> {

    HuffmanInfo hi = new HuffmanInfo(5);

    Counter<X> root = new Counter<X>();
    Map<X, Counter<X>> counters = new TreeMap<X, Counter<X>>();

    List<Counter<X>> counterList;

    void add(X v) {
        Counter<X> c = counters.get(v);
        if (c == null) counters.put(v, c = new Counter<X>(v));
        c.count++;
    }


/*
    void readTable(BitReader rd, Input<X> valueInput) throws IOException {
        //if (magic[1]!=rd.read(16)) throw new IOException("HTRE1");
        int size;
        //System.out.println("Table: "+rd.bitCount);
        while ((size = rd.read(5)) != 0x1f) {
            int s = rd.bitCount;
            X v = valueInput.read();
            //System.out.println(">"+size+":"+v);
            root.attach(size, v);
            hi.distinct++;
            hi.valueSize += rd.bitCount - s;
            hi.sizeSize += 5;
        }
        //if (hi.distinct!=rd.read(16)) throw new IOException("HTRE2");

    }
*/


    void writeCanonicalTable(BitWriter w, Output<X> o) throws IOException {
        for (Counter<X> c : counterList) c.write(hi,o,w);
        w.write(0x1f,5);
    }


    void dump() throws IOException {
        for (Counter<X> c : counterList) System.out.println(c.size+" "+Counter.bitString(c.code,c.size)+" "+c.data);        
    }

    void computeCanonicalTable() throws IOException {

        sort();
        counterList=new ArrayList<Counter<X>>(this.counters.values());
        root.computeCodeSize(0, 0);
        computeCodes();

        Collections.sort(counterList,Counter.<X>valueComparator());
    }

    public void computeCodes() {
        root=null;
        Collections.sort(counterList, Counter.<X>sizeValueComparator());
        int code=0;
        int size=0;
        for (Counter<X> c : counterList) {
            code<<=c.size-size;
            size=c.size;
            c.code=reverse(code,size);
            //System.out.println(c.data+" "+code+" "+size+" "+Counter.bitString(c.code,c.size));
            code++;
            // construct the decode tree:
            root=Counter.attach(0,root,c);

        }
    }

    // reverse the specified number of bits
    private static int reverse(int code,int size) {
        int acc=0;
        while (size>0) {
            acc<<=1;
            acc+=code & 1;
            code>>>=1;
            size--;
        }
        return acc;
    }

    public HuffmanInfo info(int valueSize) {
        for (Counter<X> c : counterList) c.info(hi,valueSize);
        return hi;
    }

    public void readCanonicalTable(BitReader r, Input<X> in) throws IOException {
        counterList=new ArrayList<Counter<X>>();

        int size;
        while (true) {
            Counter<X> counter=new Counter<X>();
            size=r.readShort(5,0x1f);
            //System.out.println("ReadCT "+size);
            if (size==0x1f) break;
            counter.size=size;
            counter.readValue(hi, r, in);
            //System.out.println("Data: "+counter.data);
            counterList.add(counter);
        }
        computeCodes();

    }




/*
    void writeTable(BitWriter w, Output<X> o) throws IOException {

        //w.write(magic[1],16);
        //System.out.println("Table: "+w.bitCount);
        if (!counters.isEmpty()) {
            counterList = new ArrayList<Counter<X>>(counters.values());
            sort();

            root.write(w, o, 0, 0, hi);
            root.print();
        }
        w.write(0x1f, 5);
        //w.write(hi.distinct, 16);

    }
*/

    private void sort() {
        counterList=new ArrayList<Counter<X>>(this.counters.values());
        Comparator<Counter<X>> cc = Counter.countComparator();
        Collections.sort(counterList, cc);

        while (counterList.size() > 1) {

            Counter<X> c0 = counterList.remove(0);
            Counter<X> c1 = counterList.remove(0);

            Counter<X> nc = new Counter<X>(c0, c1);
            int index = Collections.binarySearch(counterList, nc, cc);
            if (index < 0) index = -(index + 1);
            counterList.add(index, nc);

        }

        if (counterList.size()>0) root = counterList.get(0);
    }

    void write(X v, BitWriter w) throws IOException {
        Counter<X> e = counters.get(v);
        //System.out.println("["+bitString(e.code,e.size)+"]");

        int c = e.code;
        w.write(c, e.size);
    }

    X read(BitReader rd) throws IOException {
        Counter<X> c = root;
        //System.out.print("[");
        while (c.data == null) {
            int b = rd.readShort(1,1);
            //System.out.print(b+":"+c+" " );
            if (b == 0) c = c.c0;
            if (b == 1) c = c.c1;
        }
        //System.out.println("]");
        return c.data;
    }


    public String toString() {
        return hi.toString();
    }

    public static HuffmanTable<Integer> empty = new HuffmanTable<Integer>();

/*
    public static void writeEmpty(BitWriter w) throws IOException {
        empty.writeTable(w, null);
    }
*/


    public Iterator<X> iterator() {
        final Iterator<Counter<X>> i = counterList.iterator();
        return new Iterator<X>() {
            public boolean hasNext() {return i.hasNext();}
            public X next() {return i.next().data;}
            public void remove() {throw new UnsupportedOperationException();}
        };
    }

    public static void main(String[] args) {
        System.out.println(Integer.toHexString(reverse(0x381,10)));
    }
}

