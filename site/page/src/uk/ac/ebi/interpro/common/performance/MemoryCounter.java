package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.lang.reflect.Array;
import java.sql.*;
import java.io.*;

public final class MemoryCounter implements ToHTML {


    public static class ClassInfo implements Comparable<ClassInfo> {

        public ClassInfo(Class c) {
            this.c = c;
        }
        Class c;
        long count;
        long size;
        long refers;
        boolean open;
        public int compareTo(ClassInfo classInfo) {
            return Long.signum(classInfo.refers - refers);
        }

    }

    public PrintWriter describe;


    public Map<Class, ClassInfo> classSizing = new AutoMap<Class, ClassInfo>(
            new HashMap<Class, ClassInfo>(),
            Creator.reflective(ClassInfo.class, Class.class)
    );


    public void toHTML(PrintWriter wr) {
        wr.println("<table>");
        wr.println("<tr><td>Class Name</td><td>Count</td><td>Size</td><td>References</tr>");
        ArrayList<ClassInfo> ci = new ArrayList<ClassInfo>(classSizing.values());
        Collections.sort(ci);
        for (ClassInfo info : ci) {
            wr.println("<tr><td>" + info.c.getCanonicalName() + "</td><td>" + info.count + "</td><td>" + info.size + "</td><td>" + info.refers + "</tr>");
        }
        wr.println("</table>");
        wr.println("<div>Total: " + total + "</div>");
    }

    private final Map primitiveSizes = new IdentityHashMap() {
        {
            put(boolean.class, new Integer(1));
            put(byte.class, new Integer(1));
            put(char.class, new Integer(2));
            put(short.class, new Integer(2));
            put(int.class, new Integer(4));
            put(float.class, new Integer(4));
            put(double.class, new Integer(8));
            put(long.class, new Integer(8));
        }
    };

    public int getPrimitiveFieldSize(Class clazz) {
        return ((Integer) primitiveSizes.get(clazz)).intValue();
    }
    public int getPrimitiveArrayElementSize(Class clazz) {
        return getPrimitiveFieldSize(clazz);
    }
    public int getPointerSize() {
        return 4;
    }
    public int getClassSize() {
        return 8;
    }

    List<Class> exclude=new ArrayList<Class>();

    private final Map visited = new IdentityHashMap();
    //private final ArrayList stack = new ArrayList();
    public long total;

    public void reset() {
        classSizing.clear();
        visited.clear();
        //stack.clear();
        total = 0;
        exclude.add(ClassLoader.class);
        exclude.add(Thread.class);
    }

    public long estimate(Object obj) {
        reset();
        long result = estimateObject(obj);
        return result;
    }

    public long schedule(Object obj) {
        return estimateObject(obj);
    }

    private boolean skipObject(Object obj) {
        for (Class c : exclude) {
            if (c.isInstance(obj)) return true;
        }
        if (obj instanceof String) {
            // this will not cause a memory leak since
            // unused interned Strings will be thrown away
            if (obj == ((String) obj).intern()) {
                return true;
            }
        }
        return (obj == null)
                || visited.containsKey(obj);
    }

    private long estimateObject(Object obj) {
        if (skipObject(obj)) return 0;
        if (describe!=null) describe.println("<li>"+obj.getClass()+"</li><ul>");

        visited.put(obj, null);
        long result = 0;
        long refers = 0;

        Class<? extends Object> clazz = obj.getClass();

        MemoryCounter.ClassInfo info = classSizing.get(clazz);
        boolean guard=info.open;
        info.open=true;

        if (clazz.isArray()) {
            result = 16;
            int length = Array.getLength(obj);
            if (length != 0) {
                Class arrayElementClazz = obj.getClass().getComponentType();
                if (arrayElementClazz.isPrimitive()) {
                    result += length *
                            getPrimitiveArrayElementSize(arrayElementClazz);
                } else {
                    for (int i = 0; i < length; i++) {
                        result += getPointerSize();
                        refers += schedule(Array.get(obj, i));
                        if (describe!=null) describe.println("<li>["+i+"] "+refers+" "+result+"</li>");
                    }
                }
            }
        } else if (obj instanceof Reference) {
        } else {
            Class superClass = clazz;
            while (superClass != null) {
                Field[] fields = superClass.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    long size = 0;
                    if (!Modifier.isStatic(fields[i].getModifiers())) {
                        if (fields[i].getType().isPrimitive()) {
                            size = getPrimitiveFieldSize(
                                    fields[i].getType());
                        } else {
                            size = getPointerSize();
                            fields[i].setAccessible(true);
                            try {
                                Object toBeDone = fields[i].get(obj);
                                if (toBeDone != null) {
                                    refers += schedule(toBeDone);
                                    if (describe!=null) describe.println("<li>"+fields[i].getName()+" "+refers+" "+result+"</li>");
                                }
                            } catch (IllegalAccessException ex) {
                                assert false;
                            }
                        }
                    }
                    result += size;
                }
                superClass = superClass.getSuperclass();
            }
        }
        result += getClassSize();
        result = roundUpToNearestEightBytes(result);

        info.count++;
        info.size += result;
        if (!guard) {
            info.refers += refers;
            info.open=false;
        }
        total += result;
        if (describe!=null) describe.println("<li><b>"+refers+" "+result+" "+info.refers+" "+info.size+"</b></li></ul>");
        return refers + result;
    }

    private long roundUpToNearestEightBytes(long result) {
        if ((result % 8) != 0) {
            result += 8 - (result % 8);
        }
        return result;
    }

//    protected long estimateArray(Object obj) {
//        return result;
//    }

    static class Q {
        //int[] r = new int[4096];
        //String s = new String("yui".toCharArray());
        ArrayList x=new ArrayList();
        {
            x.add(new Object());
            x.add(new Object());
        }
        List y=new ArrayList(x);

    }

    public static void main(String[] args) {
        MemoryCounter mc = new MemoryCounter();
        Reference r = new SoftReference(new Q());
        mc.estimate(r.get());
        mc.toHTML(new PrintWriter(System.out, true));
        System.exit(0);
        Object[] store = new Object[1];
        long diff = 0;
        for (int i = 0; i < store.length; i++) {
            long m1 = Runtime.getRuntime().freeMemory();
            //HashMap hm=new HashMap();
            //hm.put("4"+"r","7"+"s");
            //Object[] x=new Object[i];
            //Map x=new HashMap(0);
            //x.put(new Object(),null);
            //Q x=new Q();
            //x.r=i;
            Stack x = new Stack();
            x.add(new Q());
            x.remove(0);
            long m2 = Runtime.getRuntime().freeMemory();
            diff += (m1 - m2);
            System.out.println(i + " " + mc.estimate(x) + " " + (m1 - m2));
            store[i] = x;
        }
        //System.out.println(new MemoryCounter().estimate(x)+" "+(m1-m2));
        System.out.println(diff);
        for (int i = 0; i < store.length; i++) {
            System.out.print(store[i]);
        }

    }
}

