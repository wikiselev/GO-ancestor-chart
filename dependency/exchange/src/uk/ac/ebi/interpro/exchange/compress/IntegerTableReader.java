package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.*;

public class IntegerTableReader {

    private static Location me=new Location();

    int columnCount;
    public int rowCount;
    int rowSize;
    long base;
    public Type rowType;
    public Type[] valueTypes;
    int[] columnSize;
    private File file;
    private long dataSizeBits;
    boolean sorted;

    public IntegerTableReader(File file) throws IOException {

        Action action = me.start("Loading Integer Table "+file);
        MemoryMonitor mm=new MemoryMonitor(false);

        this.file = file;
        BitReader br=new BitReader(file);
        IntegerTableWriter.Version version = IntegerTableWriter.format.getVersion(br);
        if (version!= IntegerTableWriter.Version.INITIAL) throw new IOException("Unrecognised format");
        sorted=br.readInt()>0;
        columnCount=br.readInt();
        rowCount=br.readInt();
        rowType=new Type(br);
        valueTypes =new Type[columnCount];
        columnSize=new int[columnCount];
        for (int i=0;i<columnCount;i++) {
            valueTypes[i]=new Type(br);
            columnSize[i]=br.readInt();
            rowSize+=columnSize[i];
            //System.out.println("Row "+i+" "+columnSize[i]);
        }

        IntegerTableWriter.format.confirmCheckPoint(br, IntegerTableWriter.Magic.START);
        base=br.bitCount();
        //System.out.println("Size: "+rowCount+"*"+rowSize+" "+br.bitCount());
        dataSizeBits = ((long) rowCount) * rowSize;
        br.seek(base+ dataSizeBits);
        IntegerTableWriter.format.confirmCheckPoint(br, IntegerTableWriter.Magic.END);
        br.close();

        me.note(mm.end(),mm);
        me.stop(action);
    }


    public int[] extractColumn(int column) throws IOException {
        int[] values=new int[rowCount];
	    Cursor c = open();
	    int[] row;
        int i=0;
        while ((row=c.read())!=null) {
            values[i++]=row[column];
        }
        c.close();
        return values;
    }


    
    public Cursor open(List<Closeable> connection) throws IOException {
	    Cursor cursor = open();
	    connection.add(cursor);
        return cursor;
    }

	public Cursor open() throws IOException {
		return new Cursor(new BitReader(file));
	}

	public class Cache {
        BitReader.Cache cache;

        public Cache(BitReader.Cache cache) {
            this.cache = cache;
        }

        /**
         * No need to close this cursor, it's not attached to a file
         * @return A cursor using a shared data byte array. 
         * @throws IOException Not unless something goes badly wrong
         */
        public Cursor use() throws IOException {
            return new Cursor(cache.use());
        }
    }

    public Cache cache() throws IOException {
        BitReader br=new BitReader(file);
        Cache cache=new Cache(br.cache(base,dataSizeBits));
        br.close();
        return cache;

        /*SharedCursor cursor = new SharedCursor(all);
        connection.add(cursor);
        return cursor;*/
    }

    public int columnCount() {
        return columnCount;
    }

    public int rowCount() {
        return rowCount;
    }

    /*public class SharedCursor implements Closeable {
        private Cursor cursor;

        SharedCursor(boolean all) throws IOException {
            cursor=new Cursor(all);
        }

        public synchronized boolean read(int rownumber, int[] row) throws IOException {return cursor.read(rownumber, row);}

        public synchronized int[] read(int index) throws IOException {return cursor.read(index);}

        public synchronized void close() throws IOException {cursor.close();}

        public synchronized int size() {return cursor.size();}

        public synchronized int columnCount() {return cursor.columnCount();}

        public synchronized int search(int[] ints) throws IOException {return cursor.search(ints);}
    }*/

    public class Cursor extends ComparingCursor<int[]> implements Closeable {
        BitReader br;

        public Cursor(BitReader br) throws IOException {
            this.br = br;
            br.seek(base);
            row=new int[columnCount];
        }

        int currentIndex=0;

        public boolean read(int rownumber,int[] row) throws IOException {
            if (!seek(rownumber)) return false;
            return read(row);
        }

        public boolean seek(int rownumber) throws IOException {
            if (rownumber>=rowCount) return false;
            currentIndex=rownumber;
            br.seek(base + ((long) rownumber) * rowSize);
            return true;
        }

        public boolean read(int[] row) throws IOException {
            if (currentIndex>=rowCount) return false;
            for (int i=0;i<columnCount;i++) {
                row[i]=br.read(columnSize[i]);
            }
            currentIndex++;
            return true;
        }

        public int[] read(int index) throws IOException {
            if (!seek(index)) return null;
            return read();
        }

        int[] row;
        public int[] read() throws IOException {
            return read(row)?row:null;
        }


        public void close() throws IOException {
            br.close();
        }

        public int compare(int index, int[] row) throws IOException {
            if (!sorted) throw new IOException("Not sorted");
            
            int[] compare=new int[row.length];
            read(index,compare);
            return CollectionUtils.intArrayComparator.compare(compare,row);

        }

        public int size() {
            return rowCount;
        }


        public int columnCount() {
            return columnCount;
        }
    }

}
