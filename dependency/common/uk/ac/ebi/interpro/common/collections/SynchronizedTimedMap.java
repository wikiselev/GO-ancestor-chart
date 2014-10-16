/*
 * Created by IntelliJ IDEA.
 * User: david
 * Date: 02-Sep-2002
 * Time: 12:27:56
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package uk.ac.ebi.interpro.common.collections;


import java.util.*;
import java.lang.ref.*;




/**
 * <p/>
 * SynchronizedTimedMap, a map that stores its values in soft references.
 * </p><p>
 * Note that null values will not be stored in this map.
 * </p><p>
 * Note also that key/value pairs may disappear from this map without warning!
 * </p>
 * <p/>
 * <p/>
 * The general contract is:
 * </p><p>
 * The mappings WILL be kept for the requested number of milliseconds.
 * At some point after this the values will no longer be strongly reachable through
 * this map and the GC MAY reclaim the values in this map.
 * </p>
 */

public class SynchronizedTimedMap extends AbstractReferenceValueMap {


    Object sync;

    long hardExpiry, softExpiry;
    private int softCount, hardCount;


    class LazyTimedHardSoftNoneReference extends SoftReference {
        Object hard;

        long timeStart;

        boolean validate(long now) {
            if (now - timeStart > hardExpiry) hard = null;
            if (now - timeStart > softExpiry) return false;
            return true;
        }


        public LazyTimedHardSoftNoneReference(Object referent) {
            super(referent);

            hard = referent;
            revalidate();
        }

        public void revalidate() {
            timeStart = System.currentTimeMillis();
        }
    }


//    private static final Category log = AutoCategory.autoCategory();

    private String name;

    private class DustMan implements Runnable {

        long time;

        public void run() {

//            log.debug("THE " + name + " dustman IS born. Interval: " + time);

            while (true) {
                try {
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
//                        log.error("Someone killed the " + name + " dustman.");
                        throw new RuntimeException("Dustman killed", e);
                    }

                    long now = System.currentTimeMillis();
                    int hc = 0, sc = 0;

                    synchronized (sync) {
                        for (Iterator it = keySet().iterator(); it.hasNext();) {
                            Object key = it.next();
                            LazyTimedHardSoftNoneReference thsnr = (LazyTimedHardSoftNoneReference) underlying.get(key);
                            if (!thsnr.validate(now)) {
                                it.remove();
                            } else {
                                sc++;
                                if (thsnr.hard != null) {
                                    hc++;
                                }
                            }
                        }
                    }

                    softCount = sc;
                    hardCount = hc;

                } catch (Throwable e) {
//                    log.error("The " + name + " dustman has tripped over it's shoe laces", e);
                    throw new RuntimeException("Dustman failed", e);
                }
            }

        }

        /**
         * Create a dustman that will wake every time milliseconds.
         */
        DustMan(long time) {
            this.time = time;
        }
    }



    public Object put(Object key, Object value) {
        LazyTimedHardSoftNoneReference thsnr = (LazyTimedHardSoftNoneReference) underlying.get(key);

        if ((thsnr != null) && (thsnr.get() == value)) {
            thsnr.revalidate();
            return value;
        }
        return putReference(key, new LazyTimedHardSoftNoneReference(value));
    }

    public static Map create(long hardExpiry, long softExpiry, String name) {

        final SynchronizedTimedMap stm = new SynchronizedTimedMap(hardExpiry, softExpiry, name);
        instances.add(stm);
        final Map map = Collections.synchronizedMap(stm);
        stm.sync = map;
        return map;
    }


    // Status functionality is implemented statically

    private static List instances = new ArrayList();

    public static String getStatus() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < instances.size(); i++) {
            SynchronizedTimedMap synchronizedTimedMap = (SynchronizedTimedMap) instances.get(i);

            sb.append(synchronizedTimedMap.getInstanceStatus() + "\n");
        }
        return sb.toString();
    }

    private String getInstanceStatus() {
        synchronized (sync) {
            return name + " " + hardExpiry + " " + hardCount + " " + softExpiry + " " + softCount;
        }
    }

    private SynchronizedTimedMap(long hardExpiry, long softExpiry, String name) {
        this.softExpiry = softExpiry;
        this.hardExpiry = hardExpiry;
        this.name = name;

        if (hardExpiry > 0) {
//            log.debug("THE " + name + " dustman conception.");
            Thread t = new Thread(new DustMan(hardExpiry), "Dustman for " + name);
            t.setDaemon(true);
            t.start();
        }

    }


    
}

