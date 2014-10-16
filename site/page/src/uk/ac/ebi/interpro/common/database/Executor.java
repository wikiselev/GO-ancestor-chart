package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.performance.*;

import javax.sql.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class Executor implements ToHTML {

    final LinkedList queue=new LinkedList();

    boolean shutdown;

    int waiting=0;

    class Agent implements Runnable {

        public void run() {

            while (!shutdown) {

                try {
                    Want want;

                    synchronized(queue) {
                        waiting++;
                        //System.out.println("Queue "+queue.size());
                        if (queue.isEmpty()) {queue.wait();}
                        want=(Want) queue.removeLast();
                        if (want==null) continue;
                        if (!want.infinitePatience() && want.patience()<=0) continue;
                        waiting--;
                    }
                    //System.out.println("Running "+want);
                    want.run();

                } catch (Exception e) {
                    // keep going whatever happens
                }
            }
        }
    }



    class Want implements Runnable {
        Runnable task;
        boolean done;
        RuntimeException e;
        long limit;

        public void run() {
            try {
                task.run();
            } catch (RuntimeException e) {
                this.e=e;
            }
            synchronized(this) {
                done = true;
                notify();
            }

        }

        public boolean infinitePatience() {
            return limit==-1;
        }

        public long patience() {
            return limit-System.currentTimeMillis();
        }

        public Want(Runnable task, long limit) {
            this.task = task;
            this.limit = limit;
        }
    }

    public void execute(Runnable runnable,long limit) throws InterruptedException {

        long end=System.currentTimeMillis()+limit;
        Want want=new Want(runnable,end);
        //System.out.println("Entering "+want+" "+limit);
        synchronized(want) {
            synchronized(queue) {
                queue.addFirst(want);
                queue.notify();
            }

            while (!want.done) {
                long remaining=want.patience();
                if (remaining<=0) {
                    synchronized(queue) {queue.remove(want);}
                    throw new InterruptedException("Timeout");
                }
                want.wait(remaining);
            }

            if (want.e!=null) throw want.e;
        }
    }

    public void async(Runnable action,long limit) {
        long end=System.currentTimeMillis()+limit;
        Want want=new Want(action,end);        
        synchronized(want) {
            synchronized(queue) {
                queue.addFirst(want);
                queue.notify();
            }
        }
    }

    public void async(Runnable action) {

        Want want=new Want(action,-1);
        synchronized(want) {
            synchronized(queue) {
                queue.addFirst(want);
                queue.notify();
            }
        }
    }

    public void shutdown() {
        synchronized(queue) {
            shutdown=true;
            queue.notifyAll();
        }
    }

    List threads=new ArrayList();

    public Executor(int threadCount) {
        for (int i=0;i<threadCount;i++) {
            Thread t=new Thread(new Agent());
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
    }


    public void toHTML(PrintWriter wr) {
        wr.println("<table>");
        wr.println("<tr><td>Threads</td><td>"+threads.size()+"</td></tr>");
        wr.println("<tr><td>Queue</td><td>"+queue.size()+"</td></tr>");
        wr.println("<tr><td>Threads ready</td><td>"+waiting+"</td></tr>");
        wr.println("</table>");
    }
}
