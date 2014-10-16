package uk.ac.ebi.interpro.exchange.compress;

import java.util.*;
import java.io.*;

public class SortedLongList extends LongList {


    public SortedLongList(File f) throws IOException {
        super(f);
    }

    public SortedLongList() {
    }

    public LongList makeQueue(int capacity) {
        final SortedLongList sorted=this;
        return new LongList(capacity) {
            public void sortMerge() {
                long[] target=new long[sorted.length+ data.length];
                Arrays.sort(data,0,length);
                int i=0,j=0,t=0;
                long prev=Long.MIN_VALUE;
                while (i<sorted.length || j<length) {
                    long next=i==sorted.length || (j<length && sorted.data[i]>data[j])? data[j++]:sorted.data[i++];
                    if (next>prev) {
                        target[t++]=next;
                        prev=next;
                    }
                }

                sorted.data=target;
                sorted.length=t;
                length=0;
            }

            public void finish() {
                sortMerge();
                /*System.out.println("Finishing...");*/
            }

            public void add(long l) {
                if (length== data.length) sortMerge();
                super.add(l);
            }
        };
    }

    // thanks to wikipedia
    public int indexOf(long value,boolean find) {
        int low = 0;
        int high = length;
        while (low < high) {
            int mid = (low + high)/2;
            if (data[mid] < value)
                low = mid + 1;
            else
                high = mid;
        }
        if (find && data[low]!=value) return -1;
        return low;
    }


}
