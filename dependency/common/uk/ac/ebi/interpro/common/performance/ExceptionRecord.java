package uk.ac.ebi.interpro.common.performance;

import org.w3c.dom.*;

import java.io.*;

public class ExceptionRecord implements ToHTML {

    private static Location me = new Location();

    private Throwable e;

    public ExceptionRecord(Throwable e) {
        this.e = e;
    }


    public void toHTML(PrintWriter pw) {

        pw.println("<h3>"+e.getMessage()+"</h3>");
        if (e instanceof ToHTML) {
            ((ToHTML) e).toHTML(pw);
        }
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");        

    }

    public static void note(String text, Throwable e) {
        me.note(text,new ExceptionRecord(e));
    }

    public static void main(String[] args) {
        Exception e1=new Exception("1");
        Exception e2=new Exception("2",e1);
        PrintWriter out = new PrintWriter(System.out);
        new ExceptionRecord(e2).toHTML(out);
        out.flush();
    }


}
