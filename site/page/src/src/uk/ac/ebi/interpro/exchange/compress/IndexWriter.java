package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;
import java.util.*;

public class IndexWriter {
    private File file;

    public enum ValueType {NONE,SINGLE,BITMASK,DELTALIST}

    List<ValueWrite> values=new ArrayList<ValueWrite>();

    public class ValueWrite {
        long value;
        int size;
        ValueType type;
        byte[] data;
        BitWriter valueWriter;
        ValueCompress compress=new ValueCompress();
        int previous=-1;
        int byteSize;

        void setSize(int size) {
            this.size=size;
            byteSize = (size+7) / 8 ;
            data=new byte[byteSize];
            valueWriter =new BitWriter(data);
        }

        long finish(int bitmaskSize, long location) throws IOException {
            previous = -1;
            compress.finish();
            if (compress.count == 0) {
                type = ValueType.NONE;
            }
            else if (compress.count == 1) {
                value = (long)compress.max;
                type = ValueType.SINGLE;
            }
            else if (compress.totalBitSize>bitmaskSize) {
                type = ValueType.BITMASK;
                value = location;
                setSize(bitmaskSize+64);

                format.writeCheckPoint(valueWriter, Magic.BITMASK);
                valueWriter.writeInt((int) compress.count);
            }
            else {
                type = ValueType.DELTALIST;
                value = location;
                setSize((int)(compress.totalBitSize+96));

                format.writeCheckPoint(valueWriter, Magic.DELTA);
                valueWriter.writeInt(compress.bits);
                valueWriter.writeInt((int)compress.count);
            }

            return byteSize;
        }

        void record(int row) {
            if (row != previous) {
	            compress.record(row-previous - 1);
	            previous = row;
            }
        }

        void write(int row) throws IOException {
            if (row==previous) return;
            switch (type) {
            case BITMASK:
                while (true) {
                    previous++;if (previous==row) break;
                    valueWriter.write(0,1);
                }
                valueWriter.write(1,1);
                break;
            case DELTALIST:
                compress.write(valueWriter,row-previous-1);
                break;
            case NONE:break;
            case SINGLE:break;
            }

            previous=row;
        }

        void writeKey(BitWriter bw) throws IOException {
            bw.write(type.ordinal(), 8);
            bw.writeLong(value);
        }

        void writeData(BitWriter bw) throws IOException {
            if (size != 0) {
	            if (valueWriter.bitCount() > size) {
		            throw new IOException("Overflow");
	            }
	            valueWriter.flush();
	            bw.writeBytes(data, byteSize);
            }
        }
    }

    public enum Version { INITIAL, COMPRESSED }
    public enum Magic { TABLES, DATA, END, DELTA, BITMASK }

    public static Format<Version,Magic> format=new Format<Version,Magic>("Index",Version.values());

    Spool spool;

    public IndexWriter(File file) throws IOException {
        this.file = file;
        spool = new Spool(file, 2);
    }

    public void write(int rownumber,int value) throws IOException {
        spool.write(rownumber);
        spool.write(value);
        getValue(value).record(rownumber);
    }

    private IndexWriter.ValueWrite getValue(int value) {        
        while (value>=values.size()) values.add(new ValueWrite());
        return values.get(value);
    }

    public void compress(Type from, Type to) throws IOException {
        int[] map = new int[from.cardinality];
        for (int i = 0; i < map.length; i++) {
	        map[i] = i;
        }
        compress(from, to, map, null);
    }

	public void compress(Type from, Type to, int[] map) throws IOException {
		compress(from, to, map, null);
	}

	public void compressFragmented(Type from, Type to, int[] rowNumberMap) throws IOException {
	    int[] map = new int[from.cardinality];
	    for (int i = 0; i < map.length; i++) {
		    map[i] = i;
	    }
	    compress(from, to, map, rowNumberMap);
	}

	public void compressFragmented(Type from,Type to, int[] map, int[] rowNumberMap) throws IOException {
		compress(from, to, map, rowNumberMap);
	}

    public void compress(Type from,Type to, int[] map, int[] rowNumberMap) throws IOException {
        spool.write(Integer.MAX_VALUE);
        spool.rewind();

        BitWriter bw = new BitWriter(file);
        format.writeVersion(bw, Version.COMPRESSED);

        from.write(bw);
        to.write(bw);

        format.writeCheckPoint(bw, Magic.TABLES);

        if (map.length != from.cardinality) {
	        throw new IOException("Incorrect number values " + map.length + " " + from.cardinality);
        }

        long location = 0;
        for (int i : map) {
	        location += getValue(i).finish(to.cardinality, location);
        }
        
        bw.writeLong(location);

        for (int i : map) {
	        getValue(i).writeKey(bw);
        }
        
        while (true) {
            int rownumber = spool.read();
            if (rownumber == Integer.MAX_VALUE) {
	            break;
            }
            int value = spool.read();
	        //System.out.println("compress: rownumber = " + rownumber + "  value = " + value + "  from.cardinality = " + from.cardinality);
            if (value < 0 || value >= from.cardinality) {
	            throw new IOException("Value " + value + " out of range 0<=value<" + from.cardinality);
            }
			getValue(value).write(rowNumberMap == null ? rownumber : rowNumberMap[rownumber]);
        }
        spool.close();

        bw.byteAlign();

        format.writeCheckPoint(bw, Magic.DATA);

        for (int i : map) {
            values.get(i).writeData(bw);
        }

        format.writeCheckPoint(bw, Magic.END);

        bw.close();

	    values = null;
    }
}