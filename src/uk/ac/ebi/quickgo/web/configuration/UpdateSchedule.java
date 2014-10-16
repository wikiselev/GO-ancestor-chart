package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.*;

import java.util.concurrent.*;
import java.io.*;

import org.w3c.dom.*;

public class UpdateSchedule {
    private final QuickGO quickGO;


    public UpdateSchedule(QuickGO quickGO) {
        this.quickGO = quickGO;
        updateTimer=new ScheduledThreadPoolExecutor(1);
    }
    
    public synchronized void close() {
        updateTimer.shutdown();
    }

    public synchronized void suspend() {
        if (updateFuture==null) return;
        updateFuture.cancel(false);
        updateFuture=null;
    }

    public synchronized void resume() {
        if (updateFuture!=null) return;
        if (updateInterval!=null)
            updateFuture=updateTimer.scheduleWithFixedDelay(update,0,updateInterval.getMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized boolean now() {
        if (updateTimer.getActiveCount()!=0) return false;
        updateTimer.submit(update);
        return true;
    }

    private Interval updateInterval=Interval.minutes(10);
    private ScheduledThreadPoolExecutor updateTimer;
    private ScheduledFuture<?> updateFuture;
    private Update update=new Update();


    public Interval getInterval(String text) {
        if (text==null || text.trim().length()==0) return null;
        return new Interval(text);
    }

    public synchronized boolean updateEnabled() {
        return updateFuture!=null;
    }

    public synchronized boolean autoUpdateConfigured() {
        return updateInterval!=null;
    }

    public synchronized Interval getUpdateInterval() {
        return updateInterval;
    }

    public class Update implements Runnable {


        public void run() {
            try {
                quickGO.update();
            } catch (Throwable e) {
                System.out.println("Update aborted");
                e.printStackTrace(System.out);
            }
        }


    }

    public synchronized void configure(File base, Element element) {
        updateInterval =getInterval(element.getAttribute("updateInterval"));

        suspend();
        resume();

    }

}
