package uk.ac.ebi.quickgo.web;

import uk.ac.ebi.interpro.common.performance.PerformanceMonitor;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.servlets.*;
import uk.ac.ebi.quickgo.web.update.*;
import uk.ac.ebi.quickgo.web.graphics.HierarchyGraph;

import java.net.*;
import java.text.*;
import java.util.Arrays;

/**
 * singleton class that is in overall control of the execution of the web server
 */
public class QuickGO {
    final public QuickGOMonitor monitor;
    final public DataManager dataManager=new DataManager();
    final public UpdateSchedule updateSchedule=new UpdateSchedule(this);

    final String configFile;
    public Configuration active;

    public final long started = System.currentTimeMillis();
    private long uniqueSequence = 0;
    public final String hostName;
    public final String uniqueID;
    public Dispatcher dispatcher;

    public boolean down;
    public boolean quit;
    
    public QuickGO(String initParameter) {
        dispatcher = new Dispatcher(this);
        configFile = initParameter;

        hostName = getHostName();
        uniqueID = unique();
        monitor = new QuickGOMonitor(this);
        System.out.println("Configuring");
        setConfiguration(createConfiguration());
        updateSchedule.now();
    }

    public void run() throws InterruptedException {
        synchronized(this) {
            while (!quit) {
	            this.wait();
            }
        }
    }

    public synchronized void quit() {
        quit = true;
        this.notifyAll();
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public synchronized String unique() {
        uniqueSequence++;
        String text = "00000000" + Long.toString(hostName.hashCode() * 256 + started * 65536 + uniqueSequence, 36);
        return text.substring(text.length() - 8);
    }

    public synchronized Configuration getConfiguration() {
        return active;
    }

    private Configuration createConfiguration() {
        return new Configuration(this, configFile);
    }

    public Configuration reload() {
        Configuration cfg = createConfiguration();
        if (cfg.failed()) {
            cfg.close();
        }
        else {
            setConfiguration(cfg);
        }
        return cfg;
    }

    private void setConfiguration(Configuration cfg) {
        Configuration previous;
        synchronized (this) {
            previous = active;
            active = cfg;
        }        
        if (previous != null) {
	        previous.close();
        }
    }

    public void update() {
        PerformanceMonitor m = monitor.performanceLog.start(unique(), "Checking for update");
        try {
            dataManager.updateCheck(m, active);
        }
        finally {
            monitor.performanceLog.stop(m);
        }
    }

    public synchronized void close() {
        updateSchedule.close();
        active.close();
        monitor.close();
        dataManager.close();
    }

    public static final DateFormat humanReadableDate = new SimpleDateFormat("HH:mm d/M/yyyy");

    public String since() {
        return humanReadableDate.format(started);
    }

    public static void main(String[] args) throws Exception {
        String command = args.length > 0 ? args[0] : "";
        String[] remaining = args.length == 0 ? args : Arrays.asList(args).subList(1, args.length).toArray(new String[args.length - 1]);
        if (command.equals("web") || command.equals("")) {
	        QuickGOJetty.main(remaining);
        }
        else if (command.equals("describe")) {
	        DescribeFile.main(remaining);
        }
        else if (command.equals("chart")) {
	        HierarchyGraph.main(remaining);
        }
        else {
	        CreateData.main(args);
        }
    }
}
