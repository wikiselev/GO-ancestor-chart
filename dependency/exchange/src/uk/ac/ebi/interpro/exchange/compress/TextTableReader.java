package uk.ac.ebi.interpro.exchange.compress;

import static uk.ac.ebi.interpro.exchange.compress.TextTableWriter.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.*;

/**
 * Read a compressed text table.
 *
 * Limited to 2**31 characters
 */

public class TextTableReader {

    private static Location me=new Location();

    File file;
    int columnCount;
    int rowCount;
    int charSizeBits;
    char[] decode;
    long dataStart;
    long rowInfoStart;
    int[] collation;
    int rowSizeBits;

    boolean sorted;

    public final Type rowType;

    BitReader.Cache rowInfoCache;

    public final Map<String,String> debugInfo=new LinkedHashMap<String,String>();
    private long dataSizeBits;

    public TextTableReader(File file) throws IOException {


        Action action = me.start("Loading Text Table "+file);
        MemoryMonitor mm=new MemoryMonitor(false);

        this.file = file;
        BitReader br=new BitReader(file);

        Version version = format.getVersion(br);
        if (version!= Version.XL) throw new IOException("Unrecognised format");
        int cLength=br.readInt();
        if (cLength>0) {
            sorted=true;
            collation=new int[cLength];
            for (int i=0;i<cLength;i++) {
                collation[i]=br.readInt();
            }
        }
        columnCount=br.readInt();
        rowCount=br.readInt();
        charSizeBits=br.readInt();
        long totalSize=br.readLong();
        rowSizeBits = br.readInt();
        int charCount=br.readInt();
        rowType=new Type(br);

        debugInfo.put("Column Count", String.valueOf(columnCount));
        debugInfo.put("Row Count", String.valueOf(rowCount));
        debugInfo.put("Char Size Bits", String.valueOf(charSizeBits));
        debugInfo.put("Data Size Characters", String.valueOf(totalSize));
        debugInfo.put("Row Size Bits", String.valueOf(rowSizeBits));
        debugInfo.put("Char count ", String.valueOf(charCount));
        debugInfo.put("Collation Length ", String.valueOf(cLength));

        format.confirmCheckPoint(br, Magic.START);

        debugInfo.put("Rows Start bits ", String.valueOf(br.bitCount()));

        decode = new char[charCount];
        for (int i = 1; i < charCount; i++) {
            decode[i] = (char) br.read(16);
        }

        format.confirmCheckPoint(br, Magic.LOCATIONS);
        rowInfoStart=br.bitCount();
        int rowInfoSize = rowCount * rowSizeBits;

        rowInfoCache=br.cache(rowInfoStart,rowInfoSize);

        br.seek(rowInfoStart+rowInfoSize);

        format.confirmCheckPoint(br, Magic.ROWS);
        dataStart =br.bitCount();
        dataSizeBits = totalSize*charSizeBits;
        br.seek(dataStart + dataSizeBits);
        format.confirmCheckPoint(br, Magic.END);
        br.close();

        debugInfo.put("Data Start bits ", String.valueOf(dataStart));

        me.note(mm.end(),mm);
        me.stop(action);
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

        public int size() {
            return rowCount;
        }
    }

    public Cache cache() throws IOException {
        BitReader br=new BitReader(file);
        Cache cache=new Cache(br.cache(dataStart,dataSizeBits));
        br.close();
        return cache;        
    }
    

    public Cursor open(List<Closeable> holder/*,boolean all*/) throws IOException {
        Cursor cursor = open();
        holder.add(cursor);
        return cursor;
    }

    public Cursor open() throws IOException {
        return new Cursor(new BitReader(file)/*all*/);        
    }

    /*public SharedCursor openShared(List<Closeable> holder,boolean all) throws IOException {
        SharedCursor cursor = new SharedCursor(all);
        holder.add(cursor);
        return cursor;
    }*/

    public int size() {
        return rowCount;
    }

    public Table extractColumn(int column) throws IOException {
        String[] data=new String[rowCount];
        Cursor c=new Cursor(new BitReader(file));
        String[] s;
        int i=0;
        while ((s=c.read())!=null) data[i++]=s[column];
        c.close();
        if (column==0 && collation!=null)  return new Table(data,rowType,new IndexSort.CollatingStringComparator(collation));
        else return new Table(data,rowType);
    }

    public int columnCount() {
        return columnCount;
    }

    /*public class SharedCursor implements Closeable {
        Cursor cursor;

        public SharedCursor(boolean all) throws IOException {
            cursor=new Cursor(all);
        }

        public synchronized void close() throws IOException {cursor.close();}

        public synchronized int size() {return cursor.size();}

        public synchronized String[] read(int index) throws IOException {return cursor.read(index);}


        public synchronized int search(String[] strings) throws IOException {return cursor.search(strings);}
    }
*/
    public class Cursor extends ComparingCursor<String[]> implements Closeable {
        BitReader br;

        StringBuilder[] sb=new StringBuilder[columnCount];

        Cursor(BitReader br) throws IOException {
            for (int i=0;i<columnCount;i++) sb[i]=new StringBuilder();
            this.br=br;
            br.seek(dataStart);
        }

        int currentIndex=0;

        public boolean seek(int index) throws IOException {
            if (index>=rowCount) return false;
            currentIndex=index;
            
            BitReader rib=rowInfoCache.use();
            rib.seek(rowInfoStart+rowSizeBits*index);
            long location=rib.readLong(rowSizeBits);
            rib.close();

            br.seek(dataStart +location*charSizeBits);
            return true;
        }

        boolean read(StringBuilder[] sb) throws IOException {
            if (currentIndex>=rowCount) return false;
            int v;
            for (StringBuilder s : sb) {
                s.setLength(0);
                while ((v = br.read(charSizeBits)) != 0) s.append(decode[v]);
            }
            currentIndex++;
            return true;
        }

        String[] row=new String[1];
        public int  search(String s) throws IOException {
            row[0]=s;
            return search(row);
        }


        public String[] read(int index) throws IOException {
            if (!seek(index)) return null;
            return read();            
        }

        public String[] read() throws IOException {            
            if (!read(sb)) return null;
            String[] row=new String[columnCount];
            for (int i = 0; i < sb.length; i++) row[i] = sb[i].toString();
            return row;
        }

        /**
         * Compare strings at given index with supplied string. Uses configured collator.
         *
         * Returns:
         * 0 if all the supplied string equals the current string
         * otherwise, for the first non-equals comaparison:
         * 1 if the supplied string is a substring of the current string
         * -1 if the supplied string is a substring of the current string
         * 2 if the supplied string is before the current string
         * -2 if the supplied string is after the current string
         * 3 if the current position is after the end of the table
         * -3 if the supplie text is null
         */
        public int compare(int index,String[] text) throws IOException {
            seek(index);

            return compare(text,read());
        }

        public int compare(String[] text,String[] currentRow) throws IOException {
            if (!sorted) throw new IOException("Not sorted");

            if (currentRow==null) return 3;
            if (text==null) return -3;


            for (int column = 0; column < text.length; column++) {
                String supplied = text[column];

                int len = supplied.length();
                String current = currentRow[column];
                for (int i = 0; i < current.length(); i++) {
                    if (i >= len) return 1;
                    int cf = collation[current.charAt(i)] - collation[supplied.charAt(i)];
                    if (cf<0) return -2;
                    if (cf>0) return 2;
                }
                if (current.length() < len) return -1;
            }

            return 0;
        }


        public String[] nextPrefix(String[] text) throws IOException {
            String[] row=read();
            if (compare(text,row)<2) return row;
            return null;
        }


        public int size() {
            return rowCount;
        }


        public void close() throws IOException {
            br.close();
        }

        
    }
}
