package uk.ac.ebi.interpro.exchange;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.*;

public class Progress {

    private static Location me=new Location();

    long time;
    int count;
    long startMemory;
    private String name;
    Runtime rt= Runtime.getRuntime();
    RowReader.ProgressMonitor monitor;
    private Action action;


    public Progress(RowReader.ProgressMonitor monitor, String name) {
        this(name);
        this.monitor = monitor;
    }

    public Progress(String name) {
        this.name=name;
        reset();
    }

    public void reset() {
        count=0;
        System.out.print(name+":");
        time =System.nanoTime();

        startMemory = memory();
        if (action==null) action=me.start(name);
    }

    public void next() {
        if ((count++)%100000==0) {
            long memory = memory();
            if (monitor !=null) {
                long now=System.nanoTime();
                double pct= monitor.getFraction();
                String estimate=pct==0?"":(int)((now-time)/pct*(1-pct)/Interval.SECOND_NS)+"s";
                System.out.print("["+(int)(pct*100)+"%"+estimate+ memory /1048576+"MB]");
            } else {
                System.out.print("["+count/100000+":"+ memory /1048576+"MB]");
            }
        }
    }

    private long memory() {
        //rt.gc();
        long memory = (rt.totalMemory() - rt.freeMemory());
        return memory;
    }

    public void end() {time =System.nanoTime()-time;
        long usedMemory = memory()-startMemory;
        String message = ":" + time / Interval.SECOND_NS + "s " + count + " " + (count == 0 ? "" : time / count + " ns " + usedMemory / count + " bytes each");
        System.out.println(message);
        me.stop(action);
        if (action!=null) action.setExtra(message);
        action=null;
    }


    public static RowReader monitor(String name,final RowReader.MonitorRowReader in) {
        final Progress p=new Progress(in,name);
        return new RowReader() {
            public boolean read(String[] data) throws Exception {
                p.next();
                return in.read(data);
            }

            public void open() throws Exception {
                in.open();
                p.reset();
            }

            public void close() throws Exception {
                p.end();
                in.close();
            }

            public String[] getColumns() throws Exception {
                return in.getColumns();
            }
        };
    }
    

}
