package uk.ac.ebi.interpro.exchange.compress;

import java.text.*;

class HuffmanInfo {
    int encodingSize;
    int valueSize;
    int sizeSize;
    int distinct;
    int total;
    int overhead;
    int count;

    static NumberFormat nf1dp = new DecimalFormat("00.0");
    static NumberFormat nfbig = new DecimalFormat("###,###,###");

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
        return "#"+nfbig.format(count) + " total: " + nfbig.format(total) + " distinct:" + nfbig.format(distinct) +
                " data: " + bitSize(encodingSize) + " (bpr: " + nf1dp.format(1.0f * encodingSize / total) +
                ") tables: "+bitSize(tableSize())+" (values: " + bitSize(valueSize) + " size: " + bitSize(sizeSize)+")";
    }

    public int tableSize() {return valueSize+sizeSize+overhead;}

    public static String bitSize(long size) {
        return nfbig.format(size / 8) + "/" + size % 8;
    }
}
