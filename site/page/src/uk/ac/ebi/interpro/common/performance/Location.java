package uk.ac.ebi.interpro.common.performance;

public class Location {
    public String name;

    public Location() {
        // more reliable than Thread.currentThread().getStackTrace()
        // which has some other random stuff on the beginning.
        try {
            throw new Throwable();
        } catch (Throwable e) {
            name =e.getStackTrace()[1].getClassName();
        }
    }

    public boolean log() {
        return PerformanceMonitor.get()!=null;
    }

    public Action start(String name) {
        return start(name,null);
    }

    public Action start(String name,Object extra) {
        PerformanceMonitor pm=PerformanceMonitor.get();
        if (pm==null) return null;
        return pm.start(this.name,name,extra);
    }

    public void stop(Action action) {
        if (action==null) return;
        PerformanceMonitor pm=PerformanceMonitor.get();
        if (pm==null) return;
        pm.stop(action);
    }

    public Action note(String name) {
        return note(name,null);
    }
    
    public Action note(String name,Object extra) {
        PerformanceMonitor pm=PerformanceMonitor.get();
        if (pm==null) return null;

        Action action = pm.start(this.name, name, extra);
        pm.stop(action);
        return action;
    }

    public static void main(String[] args) {
        System.out.println(new Location().name);
   }

}
