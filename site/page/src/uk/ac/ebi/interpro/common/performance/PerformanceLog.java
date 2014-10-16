package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;

public class PerformanceLog {

    


    final public List<PerformanceMonitor> running = new ArrayList<PerformanceMonitor>();
    final public List<PerformanceMonitor> recent = new LimitedSizeList<PerformanceMonitor>(100);

    final public WeakValueMap<String, PerformanceMonitor> archive = new WeakValueMap<String, PerformanceMonitor>();

    private int sequence=0;

    public PerformanceLog() {
    }

    public synchronized PerformanceMonitor start(String id,String name) {
        PerformanceMonitor monitor = new PerformanceMonitor(id, name);
        archive.put(id,monitor);
        monitor.attach();
        running.add(monitor);
        return monitor;
    }

    public synchronized PerformanceMonitor start(String name) {
        return start(String.valueOf(sequence++),name);
    }

    public synchronized void stop(PerformanceMonitor monitor) {
        monitor.detach();

        running.remove(monitor);
        recent.add(monitor);
        archive.vacuum();
        
    }



}
