package uk.ac.ebi.interpro.exchange;

import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;

public class Kompressor {

    static int[] magic = {13299, 52639, 40937};

//    static class Encoding {
//        Value data;
//        int code;
//        int size;
//
//        public Encoding(Encoding initial, int extraBit) {
//            code = (initial.code) | extraBit << initial.size;
//            size = initial.size + 1;
//        }
//
//        public Encoding() {
//        }
//
//        public Encoding(Encoding initial, Value data) {
//            this.code = initial.code;
//            this.size = initial.size;
//            this.data = data;
//        }
//    }
//

    static class HuffmanInfo {
        int encodingSize;
        int valueSize;
        int sizeSize;
        int distinct;
        int total;
        int overhead;
        int count;

        public HuffmanInfo(int overhead) {
            this.overhead = overhead;
            this.count = 1;
        }

        public HuffmanInfo() {
        }

        public void add(HuffmanInfo hi) {
            encodingSize += hi.encodingSize;
            valueSize += hi.valueSize;
            sizeSize += hi.sizeSize;
            distinct += hi.distinct;
            total += hi.total;
            overhead += hi.overhead;
            count += hi.count;
        }


        public String toString() {
            return nfbig.format(count) + " total: " + nfbig.format(total) + " distinct:" + nfbig.format(distinct) + " encoding: " + bitSize(encodingSize) + " bpr: " + nf1dp.format(1.0f * encodingSize / total) + " values: " + bitSize(valueSize) + " size: " + bitSize(sizeSize) + " overhead: " + bitSize(overhead);
        }
    }

    static NumberFormat nf1dp = new DecimalFormat("00.0");

    static class Counter<X> implements Comparable<Counter> {
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

        public void write(BitWriter w, ValueOutput<X> o, int initial, int size, HuffmanInfo hi) throws IOException {
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


        public int compareTo(Counter o) {
            return count - o.count;
        }

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
            if (c1.attach(size - 1, data)) return true;
            return false;
        }

        public void print() {

            System.out.println(bitString(code, size) + " " + data + " " + count);
            if (c0 != null) c0.print();
            if (c1 != null) c1.print();
        }
    }


    static String bitString(int code, int size) {
        StringBuilder sb = new StringBuilder();
        while (size > 0) {
            sb.append(code & 1);
            size--;
            code >>= 1;
        }
        return sb.toString();
    }


    static class BitReader {
        InputStream is;
        int buffer;
        int bufferBits = 0;
        int bitCount = 0;

        int read(int size) throws IOException {


            int b = 0;
            int data = 0;
            while (b < size) {

                if (bufferBits == 0) {
                    buffer = is.read();
                    if (buffer == -1) throw new EOFException("Unable to read bits");
                    // System.out.println("In: "+Integer.toBinaryString(buffer));
                    bufferBits = 8;
                }

                int i = Math.min(size - b, bufferBits);

                data |= ((buffer >> (8 - bufferBits)) & (0xff >> (8 - i))) << b;

                b += i;
                bufferBits -= i;
            }
            bitCount += size;

            //System.out.println("Read "+Integer.toBinaryString(data)+" bits "+size);
            return data;
        }

        String readASCIIString() throws IOException {
            StringBuilder buffer = new StringBuilder();
            int i;
            while ((i = read(7)) != 0) buffer.append((char) i);
            return buffer.toString();
        }

        String readUTF16String() throws IOException {
            StringBuilder buffer = new StringBuilder();
            int i;
            while ((i = read(16)) != 0) buffer.append((char) i);
            return buffer.toString();
        }

        public BitReader(InputStream is) {
            this.is = is;
        }

    }


    static class BitWriter {

        int buffer;

        int bufferBits;
        private OutputStream os;

        int bitCount = 0;

        void writeASCIIString(String text) throws IOException {
            for (int i = 0; i < text.length(); i++) {
                write((int) text.charAt(i), 7);
            }

            write(0, 7);
        }

        void writeUTF16String(String text) throws IOException {
            for (int i = 0; i < text.length(); i++) {
                write((int) text.charAt(i), 16);
            }
            write(0, 16);
        }


        void write(int data, int bits) throws IOException {
            int b = 0;
            while (b < bits) {
                int i = Math.min(8 - bufferBits, bits - b);
                buffer |= ((data >>> b) << bufferBits) & 0xff;
                bufferBits += i;
                if (bufferBits == 8) flush();
                b += i;
            }
            //System.out.println("Write "+Integer.toBinaryString(data)+" bits "+bits);
            bitCount += bits;
        }

        private void flush() throws IOException {
            //System.out.println("Flush: "+Integer.toBinaryString(buffer));
            os.write(buffer);
            buffer = 0;
            bufferBits = 0;
        }

        BitWriter(OutputStream os) {
            this.os = os;
        }


    }

//    public static void encode(RowReader rd, File f, String[] columnNames, PrintWriter pw, String compression) throws Exception {
//        OutputStream os = new FileOutputStream(f);
//        encode(rd, os, columnNames, pw, compression);
//        os.close();
//    }


    static class Writer implements RowWriter {

        BitWriter w;
        private File file;
        long tsz;
        private String[] columnNames;
        PrintWriter pw;
        Column[] columns;
        ColumnType[] columnTypes;
        List<Object[]> data = new ArrayList<Object[]>();
        int rowCount;

        Writer(File file, String[] columnNames, PrintWriter status) throws Exception {
            this.file = file;
            this.columnNames = columnNames;
            this.pw = status;
        }



        public void setColumns(String[] columnNames) throws Exception {
            if (this.columnNames==null)
                this.columnNames=columnNames;
        }

        public void write(String[] row) throws Exception {
            Object[] values = new Object[columns.length];
            data.add(values);
            rowCount++;
            if (rowCount % 100000 == 0) pw.print("," + rowCount / 100000);
            //          boolean reset = resetMode==2;
            for (int i = 0; i < columns.length; i++) {
//                if (reset) ((MarkovColumn) columns[i]).reset();
                values[i] = columns[i].add(row[i]);
//                if (((MarkovColumn) columns[i]).change && resetMode==1) reset = true;
            }
        }

        public void open() throws Exception {

            columns = new Column[columnNames.length];
            columnTypes = new ColumnType[columnNames.length];

            for (int i = 0; i < columns.length; i++) {
                columnTypes[i] = ColumnType.Markov;
                columns[i] = columnTypes[i].make("");
            }

            w = new BitWriter(new FileOutputStream(file));
            int tsz = w.bitCount;

            w.write(magic[0], 16);
        }


        public void close() throws Exception {
            pw.println();

            w.write(columns.length, 8);

            int huffmanSize = w.bitCount;

            pw.println("Calculating Compression");

            //Map<Object, Encoding>[] huffman = new Map[columns.length];

            for (int i = 0; i < columns.length; i++) {
                w.writeASCIIString(columnNames[i]);

                w.write(columnTypes[i].ordinal(), Integer.highestOneBit(ColumnType.values().length - 1));

                int tableSize = w.bitCount;

                columns[i].writeTables(w);

                tableSize = w.bitCount - tableSize;

                pw.println("Column " + columnNames[i] + " encoded " + bitSize(tableSize) + " ; " + columns[i]);

            }


            huffmanSize = w.bitCount - huffmanSize;

            pw.println("Huffman tables written: " + bitSize(huffmanSize));

            w.write(magic[1], 16);

            //PrintWriter fw=new PrintWriter(new BufferedWriter(new FileWriter("match.txt")));

            w.write(rowCount, 32);

            pw.println();
            pw.print("Encoding");

            int ebc = w.bitCount;

            rowCount = 0;


            for (Object[] r : data) {
                rowCount++;
                if (rowCount % 100000 == 0) pw.print("," + rowCount / 100000);
                //          boolean reset=resetMode==2;
                for (int i = 0; i < columns.length; i++) {

                    //              if (reset) ((MarkovColumn) columns[i]).reset();
                    columns[i].encode(r[i], w);
//                if (((MarkovColumn) columns[i]).change && resetMode==1) reset = true;
                    //pw.println("WR " + i + " " + v + " " + w.bitIndex);

                }

            }

            pw.println();

            ebc = w.bitCount - ebc;

            pw.println("Encoded " + bitSize(ebc) + " " + ebc / rowCount + " bits/row");


            w.write(magic[2], 16);
            w.write(0, 16);
            w.flush();

            tsz = w.bitCount - tsz;

            pw.println("Written " + bitSize(tsz));
        }




    }

    static NumberFormat nfbig = new DecimalFormat("###,###,###");

    private static String bitSize(long size) {
        return nfbig.format(size / 8) + ":" + size % 8;
    }

    enum ColumnType {
        /*Delta(StringDeltaColumn.class), */Markov(MarkovColumn.class)/*,String(StringColumn.class),StringRepeat(StringRepeatColumn.class)*/;
        Class<? extends Column> c;

        Column make(String s) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
            return c.getConstructor(String.class).newInstance(s);
        }

        ColumnType(Class<? extends Column> c) {
            this.c = c;
        }
    }

    interface Column<X> {
//        Value get(String text);
//        Value read(BitReader r) throws IOException;

        // Write interface

        X add(String text);

        void writeTables(BitWriter w) throws IOException;


        // Read interface
        void readTables(BitReader r) throws IOException;

        String read(BitReader r) throws IOException;

        void encode(X v, BitWriter w) throws IOException;
    }

    interface Value {
        //String extract();

        //void write(BitWriter w) throws IOException;
    }

    interface ValueInput<X> {
        X read(BitReader br) throws IOException;
    }

    interface ValueOutput<X> {
        void write(BitWriter wr, X x) throws IOException;
    }


    static class HuffmanTable<X> {

        HuffmanInfo hi = new HuffmanInfo(5);

        Counter<X> root = new Counter<X>();
        Map<X, Counter<X>> counters = new HashMap<X, Counter<X>>();


        void add(X v) {
            Counter<X> c = counters.get(v);
            if (c == null) counters.put(v, c = new Counter<X>(v));
            c.count++;
        }


        void readTable(BitReader rd, ValueInput<X> valueInput) throws IOException {
            //if (magic[1]!=rd.read(16)) throw new IOException("HTRE1");
            int size;
            //System.out.println("Table: "+rd.bitCount);
            while ((size = rd.read(5)) != 0x1f) {
                int s = rd.bitCount;
                X v = valueInput.read(rd);
                //System.out.println(">"+size+":"+v);
                root.attach(size, v);
                hi.distinct++;
                hi.valueSize += rd.bitCount - s;
                hi.sizeSize += 5;
            }
            //if (hi.distinct!=rd.read(16)) throw new IOException("HTRE2");

        }


        void writeTable(BitWriter w, ValueOutput<X> o) throws IOException {
            //w.write(magic[1],16);
            //System.out.println("Table: "+w.bitCount);
            if (!counters.isEmpty()) {
                List<Counter<X>> counterList = new ArrayList<Counter<X>>(counters.values());

                Collections.sort(counterList);

                while (counterList.size() > 1) {

                    Counter<X> c0 = counterList.remove(0);
                    Counter<X> c1 = counterList.remove(0);

                    Counter<X> nc = new Counter<X>(c0, c1);
                    int index = Collections.binarySearch(counterList, nc);
                    if (index < 0) index = -(index + 1);
                    counterList.add(index, nc);

                }

                root = (Counter<X>) counterList.get(0);

                root.write(w, o, 0, 0, hi);
            }
            w.write(0x1f, 5);
            //w.write(hi.distinct, 16);


        }

        void write(X v, BitWriter w) throws IOException {
            Counter<X> e = counters.get(v);
            //System.out.println("["+bitString(e.code,e.size)+"]");
            w.write(e.code, e.size);
        }

        X read(BitReader rd) throws IOException {
            Counter<X> c = root;
            //System.out.print("[");
            while (c.data == null) {
                int b = rd.read(1);
                //System.out.print(b);
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

        public static void writeEmpty(BitWriter w) throws IOException {
            empty.writeTable(w, null);
        }
    }

    static class CharacterOutput implements ValueOutput<Character> {
        public void write(BitWriter wr, Character character) throws IOException {
            wr.write(character, 16);
        }
    }

    static class IntegerOutput implements ValueOutput<Integer> {
        public void write(BitWriter wr, Integer i) throws IOException {
            wr.write(i, 32);
        }
    }

    static class CharacterInput implements ValueInput<Character> {

        public Character read(BitReader br) throws IOException {
            return (char) br.read(16);
        }
    }

    static class IntegerInput implements ValueInput<Integer> {

        public Integer read(BitReader br) throws IOException {
            return br.read(32);
        }
    }

    static class StringDeltaOutput implements ValueOutput<StringDelta> {

        private HuffmanTable<Integer> sameness;
        HuffmanTable<Character> characters;

        public StringDeltaOutput(HuffmanTable<Integer> sameness, HuffmanTable<Character> characters) {
            this.sameness = sameness;
            this.characters = characters;
        }

        public void write(BitWriter wr, StringDelta s) throws IOException {
            if (s.isNull) wr.write(0,1);
            else {
                wr.write(1,1);
                sameness.write(s.sameness, wr);
                writeText(wr, s.difference);
            }

        }


        public void writeText(BitWriter wr, String s) throws IOException {
            wr.write(s.length(), 16);
            for (int i = 0; i < s.length(); i++)
                characters.write(s.charAt(i), wr);
        }
    }

    static class StringDeltaInput implements ValueInput<StringDelta> {

        HuffmanTable<Integer> sameness;
        HuffmanTable<Character> characters;


        public StringDeltaInput(HuffmanTable<Integer> sameness, HuffmanTable<Character> characters) {

            this.sameness = sameness;
            this.characters = characters;
        }


        public StringDelta read(BitReader br) throws IOException {
            if (br.read(1)==0)
                return new StringDelta();
            else
                return new StringDelta(sameness.read(br), readText(br));
        }

        private String readText(BitReader br) throws IOException {
            int length = br.read(16);
            char[] data = new char[length];
            for (int i = 0; i < length; i++) data[i] = characters.read(br);
            return new String(data);
        }
    }

    static class MarkovColumn implements Column<IndexStringValue> {

        boolean stateless;


        public MarkovColumn(String config) {
            stateless = config.contains("Stateless");

        }

        int keySize, huffmanSize, valueTableSize;


        Map<IndexStringValue, HuffmanTable<IndexStringValue>> following = new HashMap<IndexStringValue, HuffmanTable<IndexStringValue>>();
        Map<String, IndexStringValue> values = new TreeMap<String, IndexStringValue>(new Comparator<String>(){
            public int compare(String a, String b) {
                if (a==null && b==null) return 0;
                if (a==null) return -1;
                if (b==null) return 1;
                return a.compareTo(b);
            }
        });

        //Map<String, List<String>> following =new HashMap<String, List<String>>();
        //new HashMap<Value, HuffmanTable>();

        IndexStringValue initial = makeValue("");
        IndexStringValue previous = initial;

        String tableStatus;

        //boolean change,autoReset;

        public IndexStringValue add(String text) {


            checkReset(text);
            IndexStringValue value = makeValue(text);

            HuffmanTable<IndexStringValue> huffman = following.get(previous);
            if (huffman == null) following.put(previous, huffman = new HuffmanTable<IndexStringValue>());
//            change = previous != value;
            huffman.add(value);
            previous = value;
            return value;
        }

        private void checkReset(String text) {
            if (stateless) previous = initial;
        }

        private IndexStringValue makeValue(String text) {
            IndexStringValue value = values.get(text);
            if (value == null) {
                values.put(text, value = new IndexStringValue(text));
            }
            return value;
        }

        public void writeTables(BitWriter w) throws IOException {


            int index = 0;
            IndexStringValue prev = initial;

            List<IndexStringValue> sortedValues = new ArrayList<IndexStringValue>(values.values());

            List<StringDelta> deltaValues = new ArrayList<StringDelta>();
            HuffmanTable<StringDelta> stringDiff = new HuffmanTable<StringDelta>();
            final HuffmanTable<Integer> valueIndexDelta = new HuffmanTable<Integer>();
            HuffmanTable<Character> characters = new HuffmanTable<Character>();
            HuffmanTable<Integer> sameness = new HuffmanTable<Integer>();

            //Map<String,HuffmanTable<IntegerValue>> markovHuffman=new HashMap<String, HuffmanTable<IntegerValue>>();

            w.write(sortedValues.size(), 16);
            //System.out.println("Values:");
            for (IndexStringValue s : sortedValues) {
                s.index = index++;
                StringDelta delta = new StringDelta(prev.text, s.text);
                deltaValues.add(delta);
                stringDiff.add(delta);

                //value.writeText(w, prev);
//                value.index = index++;
//                HuffmanTable<IntegerValue> f=new HuffmanTable<IntegerValue>();
//
//                markovHuffman.put(,f);
//                for (String s : following.get(value)) {
//                    IntegerValue i=listofvalues.indexOf(s);
//                    valueIndexDelta.add(i);
//                };
//                prev = value.text;

                //System.out.println(s.text+" "+s);

                prev = s;
            }
            for (StringDelta delta : stringDiff.counters.keySet()) {
                sameness.add(delta.sameness);
                for (int i = 0; i < delta.difference.length(); i++)
                    characters.add(delta.difference.charAt(i));
            }


            characters.writeTable(w, new CharacterOutput());
            sameness.writeTable(w, new IntegerOutput());
            stringDiff.writeTable(w, new StringDeltaOutput(sameness, characters));

            for (StringDelta value : deltaValues) {
                stringDiff.write(value, w);

            }

            //valueTableSize = w.bitCount - bc;
            //System.out.println("Writing "+following.size()+" tables");


            for (IndexStringValue s : sortedValues) {
                //bc = w.bitCount;
                //keySize += w.bitCount - bc;
                HuffmanTable<IndexStringValue> followers = following.get(s);
                if (followers != null)
                    for (IndexStringValue s2 : followers.counters.keySet()) {
                        valueIndexDelta.add(s2.index - s.index);
                    }

            }

            valueIndexDelta.writeTable(w, new IntegerOutput());

            for (final IndexStringValue s : sortedValues) {

                HuffmanTable<IndexStringValue> followers = following.get(s);
                if (followers == null) HuffmanTable.writeEmpty(w);
                else
                    followers.writeTable(w, new ValueOutput<IndexStringValue>() {
                        public void write(BitWriter wr, IndexStringValue s2) throws IOException {
                            valueIndexDelta.write(s2.index - s.index, wr);
                        }
                    });

                // huffmanSize += w.bitCount - bc;
                //System.out.println(value);
                //table.root.print();

            }
            previous = initial;

            tableStatus = "Characters:[" + characters.toString() + "]\n" +
                    "ValueIndexDelta:[" + valueIndexDelta + "]\n" +
                    "StringDelta:[" + stringDiff + "]\n" +
                    "Sameness:[" + sameness + "]\n";
        }

        public void encode(IndexStringValue v, BitWriter w) throws IOException {
            checkReset(v.text);
//            change = previous != v;
            following.get(previous).write(v, w);
            previous = v;
        }

        public void readTables(final BitReader rd) throws IOException {
            int valuesize = rd.read(16);
            System.out.println("values:" + valuesize);
            HuffmanTable<Character> characters = new HuffmanTable<Character>();
            characters.readTable(rd, new CharacterInput());
            HuffmanTable<Integer> sameness = new HuffmanTable<Integer>();
            sameness.readTable(rd, new IntegerInput());
            HuffmanTable<StringDelta> stringdiff = new HuffmanTable<StringDelta>();
            stringdiff.readTable(rd, new StringDeltaInput(sameness, characters));

            final List<IndexStringValue> sortedValues = new ArrayList<IndexStringValue>();

            System.out.println("C:" + characters.toString());
            System.out.println("SN:" + sameness.toString());
            System.out.println("SD:" + stringdiff.toString());

            //System.out.println("Values:");
            String previous = "";
            for (int i = 0; i < valuesize; i++) {
                StringDelta delta = stringdiff.read(rd);
                String current = delta.extract(previous);
                //System.out.println(current+" "+delta);
                sortedValues.add(new IndexStringValue(current, i));
                previous = current;
            }

            final HuffmanTable<Integer> valueIndexDelta = new HuffmanTable<Integer>();
            valueIndexDelta.readTable(rd, new IntegerInput());


            System.out.println("VID:" + valueIndexDelta.toString());

            for (int i = 0; i < valuesize; i++) {

                HuffmanTable<IndexStringValue> mt = new HuffmanTable<IndexStringValue>();
                final int i1 = i;
                mt.readTable(rd, new ValueInput<IndexStringValue>() {
                    public IndexStringValue read(BitReader br) throws IOException {
                        return sortedValues.get(i1 + valueIndexDelta.read(br));
                    }
                });
                following.put(sortedValues.get(i), mt);
            }

            /*final List<IndexStringValue> isv = new ArrayList<IndexStringValue>();
                        String previous = "";
                        for (int i = 0; i < valuesize; i++) {
                            IndexStringValue v = new IndexStringValue(rd, i, previous);
                            isv.add(v);
                            previous = v.text;
                        }

                        //System.out.println("Reading "+mapsize+" tables");
                        for (int i = 0; i < valuesize; i++) {
                            HuffmanTable huffman = new HuffmanTable();
                            IndexStringValue key = isv.get(i);
                            following.put(key, huffman);
                            //System.out.println("Key "+key);
                            huffman.readTable(rd, new ValueInput() {
                                public Value read(BitReader br) throws IOException {
                                    return IndexStringValue.find(rd, isv);
                                }
                            });


                        }
            //            for (Value value : following.keySet()) {
            //                HuffmanTable table = following.get(value);
            //                table.root.getEncodings(null,0,0);
            //                System.out.println(value);
            //                table.root.print();
            //            }
            */
        }

        public String read(BitReader r) throws IOException {

            IndexStringValue value = following.get(previous).read(r);
            previous = value;
            //Value value = previous = following.get(previous).read(r);
            //System.out.println("V "+value);
            //return value.extract();
            return value.text;
        }


        public String toString() {
            HuffmanInfo hi = new HuffmanInfo();
            for (HuffmanTable huffmanTable : following.values()) hi.add(huffmanTable.hi);
            return "markov:[" + hi.toString() + "]\n" + tableStatus;

            //return hi.toString() + " huffman: " + bitSize(huffmanSize) + " key: " + bitSize(keySize) + " values: " + bitSize(valueTableSize) + " tables: " + following.size();
            //" encoding "+bitSize(huffman.encodingSize)+" huffman entries:" + huffman.counters.size();

        }

    }


    static class CharacterValue implements Value {
        char c;

        public CharacterValue(char c) {
            this.c = c;
        }

        public void write(BitWriter w) throws IOException {
            w.write(c, 16);
        }
    }

    class IntegerValue implements Value {
        int i;
        int range;

        public IntegerValue(int i, int range) {
            this.i = i;
            this.range = range;
        }

        public void write(BitWriter w) throws IOException {
            w.write(i, range);
        }
    }

    /*static class StringDeltaColumn implements Column {

        String previous = "";

        HuffmanTable huffman = new HuffmanTable();

        public Value add(String text) {
            StringDelta v = new StringDelta(previous, text);
            huffman.add(v);
            previous = text;
            return v;
        }

        public void writeTables(BitWriter w) throws IOException {
            huffman.writeTable(w);
            previous = "";
            //huffman.root.print();
        }

        public void encode(Value v, BitWriter w) throws IOException {
            huffman.write(v, w);
        }

        public void readTables(final BitReader rd) throws IOException {

            huffman.readTable(rd, new ValueInput() {
                public Value read(BitReader br) throws IOException {
                    return new StringDelta(rd, StringDeltaColumn.this);
                }
            });

        }

        public String read(BitReader r) throws IOException {
            return previous = huffman.read(r).extract();
        }

        public String toString() {
            return huffman.toString();

        }
    }

//    static class xStringDeltaColumn {
//
//        String previous="";
//
//        public Value get(String text) {
//            StringDelta current = new StringDelta(previous, text);
//            previous=text;
//            return current;
//        }
//
//        public Value read(BitReader r) throws IOException {
//            return new StringDelta(r,this);
//        }
//
//        public void reset() {
//            previous="";
//        }
//    }
*/
    static class StringDelta implements Value {
        boolean isNull;
        int sameness;
        //int lengthChange;
        String difference = "";

//        private StringDeltaColumn column;

        StringDelta() {
            isNull = true;
        }


        public StringDelta(int sameness, String difference) {
            this.sameness = sameness;
            this.difference = difference;

        }

        StringDelta(String previous, String current) {
            if (current == null) isNull = true;
            else {
                if (previous==null) previous="";
                int limit = Math.min(previous.length(), current.length());
                int copy = 0;
                while (copy < limit && previous.charAt(copy) == current.charAt(copy)) copy++;

                difference = current.substring(copy);

                sameness = current.length() - previous.length();
            }
        }

//        StringDelta(BitReader r, StringDeltaColumn column) throws IOException {
//            this.column = column;
//            isNull = r.read(1) == 1;
//            if (!isNull) {
//                sameness = r.read(16);
//                difference = r.readASCIIString();
//            }
//        }
//
//        public void write(BitWriter w) throws IOException {
//            w.write(isNull ? 1 : 0, 1);
//            if (!isNull) {
//                w.write(sameness, 16);
//                w.writeASCIIString(difference);
//            }
//        }

//        public String extract() {
//            String current = null;
//            if (!isNull) {
//                if (column.previous == null) current = difference;
//                else current = column.previous.substring(0, sameness) + difference;
//            }
//            column.previous = current;
//            return current;
//        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringDelta that = (StringDelta) o;

            if (isNull != that.isNull) return false;
            if (sameness != that.sameness) return false;
            if (!difference.equals(that.difference)) return false;

            return true;
        }

        public int hashCode() {
            return 31 * (31 * (isNull ? 1 : 0) + sameness) + difference.hashCode();
        }

        public String toString() {
            return sameness + ":" + difference;
        }

        public String extract(String previous) {
            String current = null;
            if (!isNull) {
                if (previous == null) current = difference;
                else {
                    current = previous.substring(0, previous.length() + sameness - difference.length()) + difference;
                }
            }
            return current;
        }
    }

    /*static class StringRepeatColumn implements Column {

        String previous="";

        public Value get(String text) {
            StringRepeat current = new StringRepeat(previous, text);
            previous=text;
            return current;
        }

        public Value read(BitReader r) throws IOException {
            return new StringRepeat(r,this);
        }

        public void reset() {
            previous="";
        }
    }

    static class StringRepeat implements Value {
            boolean isNull;
        boolean repeat;

            String text="";
            private StringRepeatColumn column;

            StringRepeat(String previous, String current) {
                if (current==null) isNull=true;
                else {
                    repeat=previous.equals(current);
                    if (!repeat) text=current;
                }
            }

            StringRepeat(BitReader r,StringRepeatColumn column) throws IOException {
                this.column = column;
                isNull=r.read(1)==1;
                if (!isNull) {
                    repeat = r.read(1)==1;
                    if (!repeat) text = r.readASCIIString();
                }
            }

            public void write(BitWriter w) throws IOException {
                w.write(isNull?1:0,1);
                if (!isNull) {
                    w.write(repeat?1:0, 1);
                    if (!repeat) w.writeASCIIString(text);
                }
            }

            public String extract() {
                String current=null;
                if (!isNull) {
                    if (repeat) return column.previous; else return text;
                }
                column.previous=current;
                return current;
            }


            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                StringRepeat that = (StringRepeat) o;

                if (isNull != that.isNull) return false;
                if (repeat != that.repeat) return false;
                if (!text.equals(that.text)) return false;

                return true;
            }

            public int hashCode() {
                return 31*(31*(isNull ? 1 : 0) + (repeat?1:0)) + text.hashCode();
            }

            public String toString() {
                return repeat?"&":text;
            }
        }


    static class StringColumn implements Column {


        public Value get(String text) {
            return new StringValue(text);
        }

        public Value read(BitReader r) throws IOException {
            return new StringValue(r.readASCIIString());
        }

        public void reset() {}
    }
    */
    static class StringValue implements Value {

        String text;

        public StringValue(String text) {
            this.text = text;
        }

        public StringValue(BitReader rd) throws IOException {
            if (rd.read(1) == 0) text = rd.readASCIIString();
        }

        public void write(BitWriter w) throws IOException {
            w.write(text == null ? 1 : 0, 1);
            if (text != null) w.writeASCIIString(text);
        }

        public String extract() {
            return text;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringValue that = (StringValue) o;

            return !(text != null ? !text.equals(that.text) : that.text != null);
        }

        public int hashCode() {
            return (text != null ? text.hashCode() : 0);
        }

        public String toString() {
            return text;
        }
    }


    static class IndexStringValue implements Value {

        String text;
        int index;

        public IndexStringValue(String text) {
            this.text = text;
        }

        public IndexStringValue(String text, int index) {
            this.text = text;
            this.index = index;
        }

//
//        public IndexStringValue(BitReader rd, int index, String previous) throws IOException {
//            this.index = index;
//            if (rd.read(1) == 0) {
//                if (previous == null) previous = "";
//                int sameness = rd.read(16);
//                String difference = rd.readASCIIString();
//                text = previous.substring(0, sameness) + difference;
//            }
//        }
//
//        public void write(BitWriter w) throws IOException {
//            w.write(index, 16);
//        }
//
//        public void writeText(BitWriter w, String previous) throws IOException {
//            w.write(text == null ? 1 : 0, 1);
//            if (text != null) {
//                if (previous == null) previous = "";
//                String current = text;
//                int sameness = 0;
//                int limit = Math.min(previous.length(), current.length());
//                while (sameness < limit && previous.charAt(sameness) == current.charAt(sameness)) sameness++;
//                String difference = current.substring(sameness);
//                w.write(sameness, 16);
//                w.writeASCIIString(difference);
//            }
//        }
//
//        public String extract() {
//            return text;
//        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IndexStringValue that = (IndexStringValue) o;

            return !(text != null ? !text.equals(that.text) : that.text != null);
        }

        public int hashCode() {
            return (text != null ? text.hashCode() : 0);
        }

        public String toString() {
            return text;
        }

//        public static IndexStringValue find(BitReader rd, List<IndexStringValue> values) throws IOException {
//            return values.get(rd.read(16));
//        }
    }

//    public static void decode(File source, RowWriter wr, String[] columns, PrintWriter pw) throws Exception, SQLException {
//        InputStream is = new FileInputStream(source);
//        decode(is, wr, columns, pw);
//        is.close();
//    }

    static class Reader implements RowReader {
        private File file;
        private String[] exportColumns;
        private PrintWriter pw;
        String[] columnNames;
        BitReader rd;
        int[] columnIndex;
        int rowCount,rowsRead;
        String[] internalRow;
        Column[] columns;

        Reader(File file, String[] exportColumns, PrintWriter pw) {
            this.file = file;
            this.exportColumns = exportColumns;

            this.pw = pw;

        }


        //private static void decode(InputStream inStream, RowWriter wr, String[] targetColumnNames, PrintWriter pw) throws Exception, SQLException {
        public void open() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {


            rd = new BitReader(new FileInputStream(file));

            if (rd.read(16) != magic[0]) throw new IOException("Corrupted archive");

            int columnCount = rd.read(8);


            columns = new Column[columnCount];

            //Counter[] root = new Counter[columnCount];


            columnNames = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = rd.readASCIIString();

                columns[i] = ColumnType.values()[rd.read(Integer.highestOneBit(ColumnType.values().length - 1))].make("");

                pw.println("Read column " + columnNames[i] + " " + columns[i].getClass().getCanonicalName());
                columns[i].readTables(rd);
            }

            internalRow = new String[columnCount];

            if (exportColumns==null) exportColumns=columnNames;
            columnIndex = Script.findColumnIndexes(exportColumns, columnNames);

            if (rd.read(16) != magic[1]) throw new IOException("Corrupted archive");


            rowCount = rd.read(32);
            rowsRead=0;
        }

        public boolean read(String[] row) throws Exception {
            if ((rowsRead++)==rowCount) return false;
            for (int i = 0; i < columns.length; i++) {
                internalRow[i] = columns[i].read(rd);
            }

            for (int i = 0; i < columnIndex.length; i++) {
                if (columnIndex[i] >= 0)
                    row[i] = internalRow[columnIndex[i]];
            }
            return true;

        }


        public void close() throws IOException {

            if (rd.read(16) != magic[2]) throw new IOException("Corrupted archive");

        }

        public String[] getColumns() throws Exception {
            return columnNames;
        }
    }

    public static void main(String[] args) throws Exception {
//
//        ByteArrayOutputStream baos=new ByteArrayOutputStream();
//        BitWriter bw=new BitWriter(baos);
//        bw.write(6,3);
//        bw.flush();
//        BitReader br=new BitReader(new ByteArrayInputStream(baos.toByteArray()));
//        System.out.println(br.read(3));
//        br = new BitReader(new ByteArrayInputStream(baos.toByteArray()));
//        for (int i=0;i<3;i++)
//            System.out.println(br.read(1));
//

//
//        PrintWriter pw = new PrintWriter(System.out) {public void print(String s) {super.print(s);flush();}};
//
//
//        DataSourceCollection dsc = new DataSourceCollection();
//        DataSource ds = dsc.get(new File(args[0]).toURL());
//        Connection conn = ds.getConnection();
//
//
//        PreparedStatement sps = conn.prepareStatement("select protein_ac,go_id,evidence from GO.GOA_ASSOCIATIONS_COW where rownum<=10 order by protein_ac");
//        File f = new File("sample.kdt");
//        FileOutputStream os = new FileOutputStream(f);
//        encode(sps, os,pw);
//        os.close();
//
//        PreparedStatement ips = conn.prepareStatement("insert into INTER_WORK.TEST_LOAD_P2G(protein_ac,go_id,evidence) values(?,?,?)");
//        FileInputStream is = new FileInputStream(f);
//        decode(ips, is,pw);
//        is.close();
//        conn.commit();
//        conn.close();

    }
}
