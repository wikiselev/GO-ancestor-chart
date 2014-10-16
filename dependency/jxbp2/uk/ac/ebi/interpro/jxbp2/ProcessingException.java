package uk.ac.ebi.interpro.jxbp2;



import org.w3c.dom.*;

import java.io.*;
import java.util.*;

import uk.ac.ebi.interpro.common.xml.*;

public class ProcessingException extends Exception {

    Node node;

    public ProcessingException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ProcessingException(String string, Throwable throwable, Node node) {
        super(string, throwable);
        this.node = node;
    }


    public static void stackTrace(Throwable e,PrintWriter printWriter) {        

        StackTraceElement[] prev={};
        while (e!=null) {
            printWriter.println(e.getClass().getCanonicalName()+":"+e.getMessage());
            if (e instanceof ProcessingException) {
                DOMLocation l = DOMLocation.getLocation(((ProcessingException)e).node);
                if (l!=null) printWriter.println(l.toString());
            }
            StackTraceElement[] stack=e.getStackTrace();
            int remove=0;
            while (remove<stack.length && remove<prev.length && stack[stack.length-remove-1].equals(prev[prev.length-remove-1])) remove++;
            for (int i = 0; i < stack.length-remove; i++) {
                StackTraceElement element = stack[i];
                printWriter.println("    " + element.getClassName() + "." + element.getMethodName() + " (" + element.getFileName() + ":" + (element.isNativeMethod() ? "native" : element.getLineNumber()) + ")");
            }
            if (remove>0) printWriter.println("    ... " + remove+" more");
            prev=stack;
            e=e.getCause();
        }

    }



}
