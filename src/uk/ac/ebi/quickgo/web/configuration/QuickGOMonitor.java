package uk.ac.ebi.quickgo.web.configuration;

import org.w3c.dom.*;

import java.util.*;
import java.util.concurrent.*;
import java.text.*;
import java.io.*;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.quickgo.web.*;

public class QuickGOMonitor {

    final QuickGO quickGO;
    public List<PerformanceMonitor> requestLog=new LimitedSizeList<PerformanceMonitor>(100);


    
    public synchronized void start(Request request) {
        inprogress.add(request);
    }


    public PerformanceLog performanceLog=new PerformanceLog();

    
    public synchronized void stop(Request request) {
        long finished=System.currentTimeMillis();

        inprogress.remove(request);
    }

    
    public long totalMemory;
    public long freeMemory;
    public long gcTime;
    public long memoryDelta;

    synchronized private void monitor(long memoryDelta,long gcTime, StringBuilder sb,Runtime rt,int[] delta) {

        totalMemory = rt.totalMemory();
        freeMemory = rt.freeMemory();
        this.gcTime=gcTime;
        this.memoryDelta=memoryDelta;

    }

    
    

    public enum Status {GOOD,PARTIAL, CLIENTIO, BADREQ, FAILED};

    final int[] count=new int[Status.values().length];
    public List<Request> inprogress=Collections.synchronizedList(new ArrayList<Request>());



    public synchronized int getStillRunning() {
        return inprogress.size();
    }

    public synchronized int getStatusCount(Status s) {
        return count[s.ordinal()];
    }


    Interval monitorInterval =Interval.minutes(1);
    ScheduledThreadPoolExecutor monitorTimer;
    Future<?> monitorFuture;
    public Monitor monitor=new Monitor();

    final DateFormat df = new SimpleDateFormat("HH:mm d/M/yyyy");

    public PrintWriter out;

    

    class Monitor implements Runnable {

        int[] previous=new int[count.length];

        Runtime rt=Runtime.getRuntime();
        public void run() {
            StringBuilder sb=new StringBuilder();
        
            long before=rt.totalMemory()-rt.freeMemory();
            long startGC=System.currentTimeMillis();

            rt.gc();

            long endGC=System.currentTimeMillis();            
            long after=rt.totalMemory()-rt.freeMemory();

            int[] delta=new int[count.length];
            for (int i = 0; i < count.length; i++) {
                delta[i]=count[i]-previous[i];
                previous[i]=count[i];
            }

            monitor(before-after,endGC-startGC, sb,rt, delta);
        }
    }

    class TimerTaskProxy extends TimerTask {
        Runnable target;

        public TimerTaskProxy(Runnable target) {
            this.target = target;
        }

        public void run() {target.run();}
    }


    public Interval getInterval(String text) {
        if (text==null || text.trim().length()==0) return null;
        return new Interval(text);
    }

    public synchronized void configure(File base,Element element) {

        monitorInterval =getInterval(element.getAttribute("monitorInterval"));


        if (monitorFuture!=null) monitorFuture.cancel(false);
        if (monitorInterval!=null) {
            if (monitorFuture!=null) monitorFuture.cancel(false);

            if (monitorInterval!=null)
                monitorFuture=monitorTimer.scheduleWithFixedDelay(monitor,monitorInterval.getMillis(),monitorInterval.getMillis(),TimeUnit.MILLISECONDS);
        }


    }

    public QuickGOMonitor(QuickGO quickGO) {

        this.quickGO = quickGO;
        System.out.println("QuickGO Starting "+df.format(new Date())+" "+quickGO.uniqueID);

        monitorTimer=new ScheduledThreadPoolExecutor(1);

    }

    
    public synchronized void close() {

        monitorTimer.shutdown();

        
    }
}
