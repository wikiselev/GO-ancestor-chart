package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.util.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.w3c.dom.*;

/**
 * singleton class which controls the load and update of data to the web server
 */
public class DataManager implements Closeable {

    private static Location me=new Location();

    public Throwable failure;

    /**
     * the set of currently installed data files
     */
    private DataFiles current;

    URL update;

    File base;


    public volatile long maxUsedMemory;
    private long started;
    private long finished;
    public volatile long diskSpaceRequired;
    public volatile long diskSpaceAvailable;

	static class PropertyExpander {
		private final static Pattern propertyPattern = Pattern.compile(".*(\\$\\{(.*)\\}).*");
		private final static Matcher propertyMatcher = propertyPattern.matcher("");

		public static String expand(String s) {
			String sExpansion = null;

			propertyMatcher.reset(s);
			if (propertyMatcher.matches()) {
				String propertyName = propertyMatcher.group(1);
				String propertyValue = System.getProperty(propertyMatcher.group(2));
				if (!"".equals(propertyValue)) {
					sExpansion = s.replace(propertyName, propertyValue);
				}
			}

			return (sExpansion != null) ? sExpansion : s;
		}
	}

    public DataManager() {
    }

    public DataManager(File base) {
        this.base = base;
    }

    public void configure(File base, Element elt) throws Exception {
        configure(elt.hasAttribute("update") ? IOUtils.relativeURI(base, elt.getAttribute("update")) : null, IOUtils.relativeFile(base, PropertyExpander.expand(elt.getAttribute("base"))));
    }


    public synchronized void configure(URI update, File base) throws MalformedURLException {
        if (update != null) {
            this.update = update.toURL();
        }
        this.base = base;
    }

    public synchronized DataFiles get() {
        return current;
    }

    public synchronized DataFiles install(DataFiles df) {
        DataFiles old=current;
        current=df;
        this.notifyAll();
        return old;
    }


    public synchronized boolean waitForData() {
        while (current==null && finished==0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                break;
            }
        }
        return current!=null;
    }

    public String install(File base, Stamp latest, Configuration cfg) throws Exception {
        Action timer = me.start("Installing");
        MemoryMonitor memory = new MemoryMonitor(true);

        File directory = new File(base, latest.version);

        if (!directory.exists()) {
            File archiveFile = new File(base, latest + ".zip");            
            URL archive = archiveFile.toURI().toURL();
            if (archiveFile.exists()) {
	            unzip(archive, directory);
            }
            else {
	            throw new IOException("Unable to find datafiles " + directory);
            }
        }

        System.out.println("Installing from " + directory);

        DataFiles df = new DataFiles(directory, latest.version, cfg);

        memory.end();
        long memUsed = memory.getUsed();
        me.stop(timer);
        long timeUsed = timer.duration();
        if (memUsed > maxUsedMemory) {
	        maxUsedMemory = memory.getUsed();
        }

        DataFiles old = install(df);
        if (old != null) {
            old.close();
            if (old.directory.base.equals(df.directory.base)) {
                System.out.println("Installed same version - will not delete old data files");
            }
            else {
                if (!old.delete()) {
	                System.out.println("Deletion of old datafiles failed");
                }
            }
        }

        String status = (memUsed/1048576) + "MB in " + Interval.getTextFromNanos(timeUsed);

        System.out.println("Installation complete " + directory + (old==null ? "" : " deleted old: " + old.directory.base) + " " + status);
        return status;
    }

    public void unzip(URL archive, File target) throws IOException {
        target.mkdirs();

        Action a = me.start("Unzipping archive");

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(archive.openStream()));

        for (ZipEntry ze; (ze = zis.getNextEntry()) != null;) {
            File file = new File(target, ze.getName());
            if (!ze.isDirectory()) {
	            OutputStream fos = new FileOutputStream(file);
	            IOUtils.copy(zis,fos);
	            fos.close();
            }
        }
        zis.close();
        me.stop(a);
    }

    static NumberFormat nfbig = new DecimalFormat("###,###,###");

    public void download(URL update, File base, Stamp latest) throws Exception {
        diskSpaceRequired = (latest.size)*12/10;
        diskSpaceAvailable = base.getUsableSpace();

        System.out.println("Disk space - available: " + diskSpaceAvailable + " " + base + " size: " + latest.size + " required: " + diskSpaceRequired);

        if (diskSpaceRequired > diskSpaceAvailable) {
            throw new IOException("Insufficient disk space - available: " + nfbig.format(diskSpaceAvailable) + " required: " + nfbig.format(diskSpaceRequired));
        }

        File target = new File(base, latest.version);

        URL archive = new URL(update, latest.version + ".zip");
        Action a = me.start("Download from " + archive + " to " + target);
        System.out.println("Downloading from " + archive + " to " + target);

        unzip(archive,target);

        latest.write(current(base));
        
        me.stop(a);
    }

    public File current(File base) {
	    return new File(base, DataLocation.stampName);
    }

    public boolean updateLocal(File base, Configuration cfg) {
        String progress = "";
        try {
            progress = "reading stamp";
            File file = current(base);
            if (!file.isFile()) {
	            setLocalStatus("No data found");
	            return false;
            }

            Stamp localStamp = new Stamp(file.toURI().toURL());

            if (localStamp.version.equals(getCurrentStamp())) {
	            setLocalStatus("Up-to-date");
	            return false;
            }
            progress = "installing";
            setLocalStatus("Installed " + install(base, localStamp, cfg));
            return true;
        }
        catch (Exception e) {
            me.note("Failed " + progress, new ExceptionRecord(e));
            setLocalStatus("Failed " + progress);
        }
        return false;
    }

    private synchronized String getCurrentStamp() {
	    return current == null ? null : current.stamp;
    }

    private String remoteStatus;
    private String localStatus;


    public synchronized String getRemoteStatus() {
        return remoteStatus;
    }

    public synchronized void setRemoteStatus(String remoteStatus) {
        this.remoteStatus = remoteStatus;
    }

    public synchronized String getLocalStatus() {
        return localStatus;
    }

    public synchronized void setLocalStatus(String localStatus) {
        this.localStatus = localStatus;
    }

    private boolean updateRemote(URL update, File base, Configuration cfg) {
        String progress = "";
        try {
            if (update == null) {
	            setRemoteStatus("Not configured");
	            return false;
            }
            progress = "reading stamp";
            Stamp remoteStamp = new Stamp(new URL(update, DataLocation.stampName));

            if (remoteStamp.version.equals(getCurrentStamp())) {
	            setRemoteStatus("Up-to-date");
	            return false;
            }
            progress = "downloading";
            download(update, base, remoteStamp);
            progress = "installing";
            setRemoteStatus("Installed " + install(base, remoteStamp, cfg));
            return true;
        }
        catch (Exception e) {
            me.note("Failed " + progress, new ExceptionRecord(e));
            setRemoteStatus("Failed " + progress);
        }
        return false;
    }


    public List<PerformanceMonitor> updateLog=new LimitedSizeList<PerformanceMonitor>(100);
    public PerformanceMonitor recentCheck;


    public synchronized Interval since() {
        return started==0?null:Interval.ms(System.currentTimeMillis()-started);
    }

    /*
     * checks local & remote data repositories for up-to-date data, and installs if found.
     *
     * run as a background task on a timer by QuickGO
     */
    public void updateCheck(PerformanceMonitor monitor, Configuration cfg) {
        recentCheck = monitor;

		URL update;
		File base;

		synchronized (this) {
			update = this.update;
			base = this.base;

			started = System.currentTimeMillis();
		}

		MemoryMonitor memory = new MemoryMonitor(true);
		if (maxUsedMemory*110/100 > memory.getAvailable()) {
			setLocalStatus("insufficient memory: " + memory.getAvailable());
			return;
		}

		boolean updated = updateLocal(base, cfg);
		if (!updated) {
			updated = updateRemote(update, base, cfg);
		}

		if (updated) {
			updateLog.add(monitor);
		}

		synchronized (this) {
			finished = System.currentTimeMillis();
			this.notifyAll();
		}

		monitor.name = "Updated local: " + localStatus + " remote: " + remoteStatus;
    }

    public synchronized void close() {
        if (current != null) {
	        current.close();
        }
    }

    public synchronized File getBase() {
        return base;
    }

    public synchronized URL getUpdate() {
        return update;
    }

    public DataLocation getDirectory() throws Exception {
        return new DataLocation(new File(base,new Stamp(current(base).toURI().toURL()).version));
    }
}