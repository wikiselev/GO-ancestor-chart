package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.*;

public class TimerCounter {
    public long count;
    public long nanos;
    public int[] freq=new int[14];

    private static final double log10 = Math.log(10);

    public static Creator.Factory<TimerCounter> factory = new Creator.Factory<TimerCounter>() {
        public TimerCounter make() {return new TimerCounter();}
    };

    public void snapshot(TimerCounter previous, TimerCounter delta) {
        delta.count=count-previous.count;
        previous.count=count;
        delta.nanos=nanos-previous.nanos;
        previous.nanos=nanos;
        for (int i = 0; i < freq.length; i++) {
            delta.freq[i]=freq[i]-previous.freq[i];
            previous.freq[i]=freq[i];
        }
    }

    public void add(long interval) {
        count++;
        nanos +=interval;
        int code=interval==0?0:1+(int)Math.round(Math.floor(Math.log(interval)/log10));
        if (code>=freq.length) code=freq.length-1;
        freq[code]++;
    }
}
