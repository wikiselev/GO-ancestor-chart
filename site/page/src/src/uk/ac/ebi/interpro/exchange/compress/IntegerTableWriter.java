package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;
import java.util.*;

public class IntegerTableWriter {
    protected enum Version {INITIAL}

    protected enum Magic {START,ROWS,END}

    public static Format<Version,Magic> format=new Format<Version,Magic>("CodeTable",Version.values());

    public ValueCompress[] columns;
    protected boolean sorted;
    protected int[][] valueTranslateMap;

    protected int rowCount =0;

    public void compressSorted(Type rowType,Set<int[]> data,Type... types) throws IOException {
        sorted=true;
        for (int[] row : data) write(row);
        compress(rowType,types);
    }

    int[] row;
    int[] zero;

    public void seek(int rownumber) throws IOException {
        if (rownumber<rowCount) throw new IOException("Negative seek");
        while (rowCount<rownumber) write(zero);
    }

    public void write(int value) throws IOException {
        
        row[0]=value;
        write(row);        
    }

    public void write(int[] row) throws IOException {
	    if (row.length != columnCount) throw new IOException("row.length (" + row.length + ") != columnCount (" + columnCount + ")");
        rowCount++;
        for (int i=0;i<columnCount;i++) {
            columns[i].record(row[i]);
            spool.write(row[i]);
        }
    }


    public void setValueTranslateMap(int[]... valueTranslateMap) {
        this.valueTranslateMap = valueTranslateMap;
    }

    public void compress(Type rowType,Type... types) throws IOException {
        spool.rewind();
        BitWriter bw=new BitWriter(file);
        format.writeVersion(bw, Version.INITIAL);
        bw.writeInt(sorted?1:0);
        bw.writeInt(columnCount);
        bw.writeInt(rowCount);
        rowType.write(bw);

        if (rowType.cardinality!=rowCount) throw new IOException("Row type cardinality != row count "+rowType.cardinality+" "+rowCount);

        for (int i = 0; i < columnCount; i++) {            
            columns[i].max=types[i].cardinality-1;
            columns[i].finish();
            types[i].write(bw);
            bw.writeInt(columns[i].bits);
            System.out.println("Row "+i+" "+columns[i].totalBitSize);
        }

        format.writeCheckPoint(bw, Magic.START);

        System.out.println("Size: "+rowCount+" "+bw.bitCount());

        for (int j=0;j< rowCount;j++) {
            for (int k=0;k<columns.length;k++) {

                int value = spool.read();
                if (valueTranslateMap!=null && valueTranslateMap[k]!=null) value=valueTranslateMap[k][value];
                columns[k].write(bw, value);
            }

        }
        spool.close();
        System.out.println("end:"+bw.bitCount());
        format.writeCheckPoint(bw, Magic.END);

        bw.close();

	    if (valueTranslateMap != null) {
	        for (int k=0; k < valueTranslateMap.length; k++) {
	            valueTranslateMap[k] = null;
	        }
		    valueTranslateMap = null;
	    }
    }

    public File file;
    protected int columnCount;


    protected Spool spool;

    public IntegerTableWriter(File f,int columnCount) throws IOException {
        this.file = f;
        this.columnCount = columnCount;
        columns=new ValueCompress[columnCount];
        for (int i = 0; i < columns.length; i++) columns[i] = new ValueCompress();
        spool=new Spool(f,columns.length);
        row=new int[columnCount];
        zero=new int[columnCount];
    }

}