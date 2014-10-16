package uk.ac.ebi.interpro.common.performance;


import java.util.*;
import java.lang.ref.*;

public class PerformanceMonitor {

    PerformanceMonitor previous;
    private static ThreadLocal<PerformanceMonitor> current = new ThreadLocal<PerformanceMonitor>();
    public Action root;
    public List<Action> stack = new ArrayList<Action>();
    public final String id;
    public String name;

    public PerformanceMonitor(String id,String name) {
        this.id = id;
        this.name = name;
    }

    public void attach() {
        previous=current.get();
        current.set(this);
        root=start("","Monitor",null);
    }

    public void detach() {
        current.set(previous);
        stop(root);
    }

    public static PerformanceMonitor get() {
        return current.get();
    }


    public Action start(String location,String name,Object info) {

        Action parent = stack.isEmpty() ? null : stack.get(stack.size() - 1);
        Action a = new Action(parent,location,name, info);
        stack.add(a);
        a.setThread(Thread.currentThread());
        return a;
    }

    public void stop(Action a) {
        if (a==null) return;



        int index = -1;
        for (int i = stack.size() - 1; i >= 0; i--) {
            Action action = stack.get(i);
            if (action == a) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        while (stack.size() > index) {
            Action action = (stack.remove(stack.size() - 1));
            action.end();
            action.setThread(null);
            if (action == a) break;
        }
    }




}