package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public abstract class ComparingCursor<X> {
    abstract public int compare(int index,X x) throws IOException;
    abstract public int size();
    public synchronized int search(X x,boolean insertionPoint) throws IOException {
        int low = 0;
        int high = size() -1;
        while (low<=high) {

            int mid = (low + high)/2;
            int cf=compare(mid,x);
            //System.out.println("CF: "+low+"<"+mid+"<"+high+" "+cf);            
            if (cf==0) return mid;
            if (cf<0) low=mid+1;
            else high=mid-1;
        }
        return insertionPoint?low:-low-1;
    }
    public int search(X x) throws IOException {
        return search(x,false);
    }
}
