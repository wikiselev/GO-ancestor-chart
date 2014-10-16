package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.*;

public class IndexReader {
    private static Location me = new Location();

    public long base;
    public long indexBase;

    private int bitmaskBitSize;

    public final Type from;
    public final Type to;
    private File file;

    public final Map<String,String> debugInfo=new LinkedHashMap<String,String>();

    public IndexReader(File file) throws IOException {
        Action action = me.start("Loading Index "+file);
        MemoryMonitor mm = new MemoryMonitor(false);

        this.file = file;
        BitReader br = new BitReader(file);
        IndexWriter.Version version = IndexWriter.format.getVersion(br);
        if (version != IndexWriter.Version.COMPRESSED) {
	        throw new IOException("Not permitted format: " + version);
        }
        from = new Type(br);
        to = new Type(br);        
        IndexWriter.format.confirmCheckPoint(br, IndexWriter.Magic.TABLES);

        bitmaskBitSize = to.cardinality;

        IndexWriter.ValueType[] types = IndexWriter.ValueType.values();

        int[] typeCount = new int[types.length];
        int[] example = new int[types.length];
        Arrays.fill(example, -1);

        long size = br.readLong();
        indexBase = br.bitCount();

        for (int i = 0; i < from.cardinality; i++) {
            IndexWriter.ValueType t= types[br.read(8)];
            br.readLong();
            int typeCode = t.ordinal();
            typeCount[typeCode]++;
            example[typeCode]=i;
        }

        for (int i = 0; i < example.length; i++) {
            debugInfo.put(IndexWriter.ValueType.values()[i].name(),"count:"+typeCount[i]+" last:"+example[i]);
        }

        br.read((int) (8-br.bitCount()%8));

        IndexWriter.format.confirmCheckPoint(br, IndexWriter.Magic.DATA);
        
        base=br.bitCount();
        br.seek(8*(long)size+base);
        IndexWriter.format.confirmCheckPoint(br, IndexWriter.Magic.END);
        br.close();

        me.note(mm.end(),mm);
        me.stop(action);
    }

    public ValueRead open(List<Closeable> holder,int c) throws IOException {
        if (c < 0 || c >= from.cardinality) {
	        throw new IndexOutOfBoundsException("Index lookup "+c+" not in 0.."+from.cardinality);
        }
        ValueRead vr = open(c);
        holder.add(vr);
        return vr;
    }

    public interface ValueRead extends Find,Closeable {
        int count();
    }

    private ValueRead open(int c) throws IOException {
        BitReader br = new BitReader(file);
        br.seek(indexBase+72*(long)c);
        IndexWriter.ValueType type = IndexWriter.ValueType.values()[br.read(8)];
        long value = br.readLong();

        switch (type) {
        case BITMASK:
	        return new BitmaskValueRead(br, ((long)value)*8+base, bitmaskBitSize, c);
        case DELTALIST:
	        return new DeltaListValueRead(br, ((long)value)*8+base, c);
        case NONE:
	        br.close();
	        return new NoneValueRead(c);
        case SINGLE:
	        br.close();
	        return new SingleValueRead((int)value, c);
        }

        throw new IOException("Non recognised value type");
    }

    public static class NoneValueRead implements ValueRead {
        int code;

        public NoneValueRead(int code) {
            this.code = code;
        }

        public int next(int at) throws IOException {
            return Integer.MAX_VALUE;
        }

        public Find[] getChildren() {
            return null;
        }

        public BitReader getBitReader() {
            return null;
        }

        public void close() throws IOException {
        }


        public String toString() {
            return "None at: "+code;
        }

        public int count() {
            return 0;
        }

        public int code() {
            return code;
        }

        public int getBytesRead() {
            return 0;
        }
    }

    public static class BitmaskValueRead implements ValueRead {
        private BitReader br;
        private int code;
        private long start;

        private int count;
        private int bitmaskSize;

        public BitmaskValueRead(BitReader br,long location,int bitmaskSize,int code) throws IOException {
            this.bitmaskSize = bitmaskSize;
            this.br=br;
            this.code = code;

            br.seek(location);
            IndexWriter.format.confirmCheckPoint(br, IndexWriter.Magic.BITMASK);
            count=br.readInt();
            start=br.bitCount();
        }

        public int next(int at) throws IOException {
            if (at>=bitmaskSize) return Integer.MAX_VALUE;
            br.seek(start+at);
            while (at < bitmaskSize && !br.readBit()) at++;
            if (at==bitmaskSize) return Integer.MAX_VALUE;
            return at;
        }

        public Find[] getChildren() {
            return null;
        }

        public void close() throws IOException {
            br.close();
        }

        public String toString() {
            return "Bitmask at: "+code+" size: "+count;
        }

        public int count() {
            return count;
        }


        public BitReader getBitReader() {
            return br;
        }


    }

    public static class SingleValueRead implements ValueRead {
        int value;
        private int code;


        public SingleValueRead(int value,int code) {
            this.value = value;
            this.code = code;
        }

        public int next(int at) throws IOException {
            if (at>value) return Integer.MAX_VALUE;
            return value;
        }

        public Find[] getChildren() {
            return null;
        }

        public BitReader getBitReader() {
            return null;
        }


        public void close() throws IOException {
        }

        public String toString() {
            return "Single at: "+code+" value: "+value;
        }

        public int count() {
            return 1;
        }
    }

    public static class DeltaListValueRead extends FindCache implements ValueRead{

        private BitReader br;

        private int bitSize;
        private int count;
        int code;

        public DeltaListValueRead(BitReader br,long location,int code) throws IOException {
            this.br = br;
            this.code = code;
            br.seek(location);
            IndexWriter.format.confirmCheckPoint(br, IndexWriter.Magic.DELTA);
            //System.out.println("Seek "+br.bitCount());
            bitSize=br.readInt();
            count=br.readInt();
            //System.out.println("Reading "+bitSize+" "+count);
        }

        private int next = -1;
        private int index = -1;

        public int forwards(int at) throws IOException {

            while (at > next) {
                index++;
                if (index >= count) next = Integer.MAX_VALUE;
                else next += br.read(bitSize)+1;
            }
            return next;
        }

        public int count() {
            return count;
        }

        public void close() throws IOException {
            br.close();
        }

        public String toString() {
            return "Delta list: "+code;
        }

        public Find[] getChildren() {
            return null;
        }

        public BitReader getBitReader() {
            return br;
        }
    }
}
