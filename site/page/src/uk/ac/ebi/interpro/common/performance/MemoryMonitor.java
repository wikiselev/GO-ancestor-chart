package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.*;

import java.text.*;

public class MemoryMonitor {


    Runtime rt = Runtime.getRuntime();
    long before;
    long after;
    long firstGC,secondGC;
    long available;
    long used;

    boolean garbageCollect;


    public MemoryMonitor(boolean garbageCollect) {
        this.garbageCollect = garbageCollect;
        firstGC=gc();
        before = rt.totalMemory()-rt.freeMemory();
        available=rt.maxMemory()-before;
    }

    public MemoryMonitor() {
        this(false);
    }

    private long gc() {
        long start=System.nanoTime();

        if (garbageCollect) rt.gc();

        return System.nanoTime()-start;
    }

    public String end() {
        secondGC=gc();
        after = rt.totalMemory()-rt.freeMemory();
        available=rt.maxMemory()-after;

        used=after-before;

        return "Memory used "+nfbig.format(used/1048576)+"MB";
    }

    static NumberFormat nfbig = new DecimalFormat("###,###,###");

    public String toString() {
        return nfbig.format(after-before)+" "+nfbig.format(before)+" "+nfbig.format(after)+(garbageCollect?" "+ Interval.ns(firstGC)+" "+Interval.ns(secondGC):"");
    }

    public long getUsed() {
        return used;
    }


    public long getAvailable() {
        return available;
    }
}
