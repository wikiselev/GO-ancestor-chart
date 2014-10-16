package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public class ValueCompress {
    public void write(BitWriter bw,long value) throws IOException {
        if (value<0 || value>max) throw new IllegalArgumentException("Value out of range "+value+": 0<=value<"+max);
        bw.write(value,bits);
    }
    public int bits;
    public long totalBitSize;
    public long max;
    long count;
    long byteSize() {return (totalBitSize +7)/8;}
    void record(long value) {
        if (value<0) throw new IllegalArgumentException("Negative compression value: " + value);
        if (value>max) max=value;
        count++;
    }
    public void finish() {
        bits=BitWriter.getSize(max+1);
        totalBitSize =bits*count;
    }
}
