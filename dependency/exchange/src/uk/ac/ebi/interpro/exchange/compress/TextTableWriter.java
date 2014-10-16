package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;

import java.io.*;
import java.util.*;

public class TextTableWriter {


    File file;

    int columnCount;

    int[] collation;


    enum Version {INITIAL,COLLATING,XL}

    enum Magic {START,LOCATIONS,ROWS,END}

    public static Format<Version,Magic> format=new Format<Version,Magic>("TextTable",Version.values());


    public TextTableWriter(File file, int columnCount) throws IOException {
        this.file = file;
        this.columnCount = columnCount;
        spool=new Spool(file);
    }



    Spool spool;

    int[] chars = new int[65536];
    IntList rowLengths=new IntList(1024);
    LongList rowSpoolIndex =new LongList(1024);
    //long totalSize=0;

    /*public void seek(int rownumber) throws IOException {
        if (rownumber<rows.size()) throw new IOException("Negative seek "+rows.size()+"->"+rownumber);
        while (rows.size()<rownumber) write((String[])null);
    }
*/
    public int write(String... row) throws IOException {

        int rowNumber=rowLengths.size();
        long spoolIndex= spool.index();

        int count=0;
        for (int f = 0; f < columnCount; f++) {
            String field = row==null?null:row[f];
            if (field!=null) {
                count += field.length();
                for (int i = 0; i < field.length(); i++) chars[field.charAt(i)]++;
                spool.write(field);
            } else {
                spool.write("");
            }
            count++;

        }
        rowLengths.add(count);
        rowSpoolIndex.add(spoolIndex);
        //totalSize+=count;
        return rowNumber;
    }

    /*public int size() {
        return rows.size();
    }
*/
    public void compressSorted(Type rowType,Set<String[]> data,int[] collation) throws IOException {
        this.collation=collation;
        for (String[] row : data) write(row);
        compress(rowType);
    }

    public void compressSortedSingle(Type rowType,Set<String> data,int[] collation) throws IOException {
        this.collation=collation;
        String[] row=new String[1];
        for (String v : data) {row[0]=v;write(row);}
        compress(rowType);
    }

    public void compress(Type rowTypes) throws IOException {
        compress(rowTypes,null,rowLengths.length);
    }

    public void compress(Type rowTypes,int[] reorder,int rowCount) throws IOException {
        spool.rewind();



        long[] locations=new long[rowCount];

        long totalSize=0;
        for (int i = 0; i < rowCount; i++) {
            locations[i] = totalSize;
            totalSize += rowLengths.get(reorder!=null?reorder[i]:i);
        }

        ValueCompress rowLength=new ValueCompress();
        rowLength.record(totalSize);
        rowLength.finish();


        int charCount=1;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != 0) chars[i] = (charCount++);
        }
        final int bitSize = BitWriter.getSize(charCount);

        BitWriter bw=new BitWriter(file);
        format.writeVersion(bw, Version.XL);
        
        if (collation!=null) {
            bw.writeInt(collation.length);
            for (int i : collation) {
                bw.writeInt(i);
            }
        } else bw.writeInt(0);
        bw.writeInt(columnCount);
        bw.writeInt(rowCount);
        bw.writeInt(bitSize);
        bw.writeLong(totalSize);
        bw.writeInt(rowLength.bits);
        bw.writeInt(charCount);
        rowTypes.write(bw);

        if (rowTypes.cardinality!=rowCount) throw new IOException("Row type cardinality != row count "+rowTypes.cardinality+"!="+rowCount);
        
        format.writeCheckPoint(bw, Magic.START);

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 0) {
                bw.write(i, 16);
            }
        }
        format.writeCheckPoint(bw, Magic.LOCATIONS);

        for (long location : locations) {
            rowLength.write(bw,location);
        }

        format.writeCheckPoint(bw, Magic.ROWS);
        long checkLocation=0;
        for (int i = 0; i < rowCount; i++) {
            if (locations[i]!=checkLocation) throw new IOException("Mis-aligned rows: " + locations[i] + " != " + checkLocation);
            spool.seek(rowSpoolIndex.get(reorder!=null?reorder[i]:i));

            for (int j=0;j<columnCount;j++) {
                String value=spool.readString();
                for (int k = 0; k < value.length(); k++)
                    bw.write(chars[value.charAt(k)], bitSize);
                checkLocation+=value.length()+1;
                bw.write(0, bitSize);
            }

        }
        format.writeCheckPoint(bw, Magic.END);
        spool.close();
        bw.close();
    }


    public static void main(String[] args) throws IOException {
        File f = new File("test.tls");
        TextTableWriter w=new TextTableWriter(f,1);
        w.write("Test1");
        w.write("Test2");
        w.compress(new Type("row",2));
        TextTableReader r=new TextTableReader(f);
        TextTableReader.Cursor c=r.open();
        System.out.println(CollectionUtils.concat(c.read(),","));
        System.out.println(CollectionUtils.concat(c.read(),","));
        c.close();
    }

}
