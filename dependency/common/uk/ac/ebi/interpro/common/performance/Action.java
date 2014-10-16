package uk.ac.ebi.interpro.common.performance;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import java.io.*;
import java.text.*;
import java.lang.ref.*;

public class Action extends RCO implements Comparable<Action> {

    public static final String actionStyle=
            "div.title {cursor:pointer;background:#ccf;border:1px solid #fff;}\n"+
            "div.expand {display:block;margin:2px 2px 2px 10px;border-left:1px solid black;padding:1px;}\n";

    public static final String actionScript=
            "function expander(e) {\n" +
            "if (!e) e=window.event;\n" +
            "var t=e.target;\n" +
            "if (!t) t=e.srcElement;\n"+
            "var divs=t.parentNode.getElementsByTagName('div');\n" +
            "var d;\n" +
            "for (var x=0;x!=divs.length;x++) {if (divs[x].className=='expand') {d=divs[x];break;}}\n" +
            "if (d) {if (d.style.display!='block') d.style.display='block'; else d.style.display='none';}\n" +
            "}\n" +
            "window.addEventListener('click',expander,false);\n";




    private Action parent;
    private String location;
    private Object extra;
    public final long when;
    private final long start;
    private long end;
    private String name;
    private Reference<Thread> thread;
    private List<Action> children=new ArrayList<Action>();

    /*public static Comparator<Action> slowness=new Comparator<Action>() {
        public int compare(Action a1, Action a2) {
            long r=a2.duration()-a1.duration();
            if (r<0) return -1;
            if (r>0) return 1;
            return 0;
        }
    };*/

    public synchronized long duration() {
        return (end!=0?end:System.nanoTime())-start;
    }

    public synchronized void addChild(Action child) {
        children.add(child);
    }


    public Action(Action parent,String location,String name,Object extra) {
        this.parent=parent;
        if (parent!=null) parent.addChild(this);
        this.location = location;
        start = System.nanoTime();
        when = System.currentTimeMillis();
        this.extra = extra;
        this.name = name;

    }

    public synchronized void printActionHTML(PrintWriter wr) {

        wr.println("<div><div class='title'>");
        wr.println(toString());
        wr.println("</div><div class='expand'>");

        wr.println("<div>");
        wr.println(dateFormat.format(new Date(when)));        
        wr.print(" ");
        if (parent!=null) {
            wr.print(csn(start-parent.start));
            wr.print("-");
            wr.println(csn(end-parent.start));
        }
        wr.println(location+"</div>");
        if (extra!=null) {
            if (extra instanceof ToHTML)
                ((ToHTML)extra).toHTML(wr);
            else
                wr.println(extra.toString());
        }

        for (Action child : children) {
            child.printActionHTML(wr);
        }
        wr.println("</div></div>");
    }

    static SimpleDateFormat dateFormat = new SimpleDateFormat();

    public synchronized String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append(CollectionUtils.concat(Arrays.asList(name),":"));
        sb.append(" ");
        if (end==0)
            sb.append("<b>Running ").append(System.currentTimeMillis() - when).append("</b>");
        else
            sb.append("(").append(Interval.getTextFromNanos(end - start)).append(")");
        return sb.toString();
    }

    public int compareTo(Action a) {
        long r=start-a.start;
        if (r<0) return -1;
        if (r>0) return 1;
        return 0;
    }

    public long age() {
        return System.currentTimeMillis()-when;
    }
    public String csn(long n) {
        return String.format("%,d",n);
    }



    public synchronized <X> boolean search(Class<X> c,List<X> instances) {
        boolean r=(extra!=null && c.isInstance(extra));
        if (r && instances!=null) instances.add((X)extra);
        for (Action child : children) r=child.search(c,instances) || r;
        return r;
    }

    public <X> List<X> search(Class<X> c) {
        List<X> instances=new ArrayList<X>();
        search(c,instances);
        return instances;
    }

    public synchronized void end() {
        end = System.nanoTime();

    }

    public void setThread(Thread thread) {
        if (thread==null) this.thread=null;
        else this.thread=new WeakReference<Thread>(thread);
    }


    public synchronized void setExtra(Object extra) {
        this.extra=extra;
    }
    public synchronized Object getExtra() {
        return this.extra;
    }
}
