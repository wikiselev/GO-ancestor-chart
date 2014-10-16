package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.performance.*;

import java.util.*;

public class TimedStorage<X> implements Iterable<X>{

    public List<X> recent = new ArrayList<X>();
    public List<X> old = new ArrayList<X>();
    private long previousSwap;
    private long horizon;
    public long swapInterval = 30000;

    public TimedStorage(Interval swapInterval) {
        this.swapInterval = swapInterval.getMillis();
        horizon=System.currentTimeMillis();
        previousSwap=horizon;
    }
    public void add(X a) {
        recent.add(a);
        vacuum();
    }
    public void vacuum() {
        long now = System.currentTimeMillis();
        if (now - previousSwap > swapInterval) {
            horizon=previousSwap;
            previousSwap = now;
            old.clear();
            List<X> x = recent;
            recent = old;
            old = x;
        }
    }

    public long age() {
        return System.currentTimeMillis()-horizon;
    }

    public Iterator<X> iterator() {
        vacuum();
        final Iterator<X> rc1 = recent.iterator();
        final Iterator<X> rc2 = old.iterator();
        return new Iterator<X>() {

            public boolean hasNext() {
                return rc1.hasNext() || rc2.hasNext();
            }
            public X next() {
                if (rc1.hasNext()) return rc1.next(); else return rc2.next();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    public static void main(String[] args) throws InterruptedException {


        new RCO();

        MutableInteger mi=RCO.refCounts.get(RCO.class.getName());

        TimedStorage s=new TimedStorage(new Interval("100ms"));
        for (int i=0;i<20;i++) {
        s.add(new RCO());
        Thread.sleep(50);
            Runtime.getRuntime().gc();
        System.out.println(s.recent.size()+" "+s.old.size()+" "+ Interval.getTextFromMillis(s.age())+" "+mi.i);
        }

    }

    public int size() {
        return recent.size()+old.size();
    }
}
