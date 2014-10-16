package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.interpro.common.*;

import java.io.*;
import java.util.*;
import java.text.*;

public class GFeedback implements Dispatchable {

    public static class Thanks {
        public String id;

        public Thanks(String id) {
            this.id = id;
        }
    }

    public static class View {
        public boolean denied=false;
        public List<Response> responses=new ArrayList<Response>();
    }

    final static DateFormat df = new SimpleDateFormat("HH:mm d/M/yyyy");


    public static class Response implements Comparable<Response> {

        public String text;
        public long when;
        public String id;
        public String date() {
            return df.format(when);
        }

        public Response(String text, long when, String id) {
            this.text = text;
            this.when = when;
            this.id = id;
        }

        public int compareTo(Response response) {
            return Long.signum(response.when-when);
        }
    }


    public void process(Request r) throws Exception {
        String action=r.getParameter("action");
        if ("View".equals(action)) {
            View view=new View();
            if (r.getParameter("password").equals(r.configuration.feedback.password)) {
                for (File file : r.configuration.feedback.path.listFiles()) {
                    if (file.getName().endsWith(".txt")) {
                        view.responses.add(new Response(IOUtils.readString(file.toURI().toURL()),file.lastModified(),file.getName()));
                    }
                }
                Collections.sort(view.responses);
            } else view.denied=true;
            r.write(r.outputHTML(true, "page/GFeedbackView.xhtml").render(view));
        } else if ("Feedback".equals(action)) {
            String id=r.quickGO.unique();
            File target=new File(r.configuration.feedback.path,id+".txt");
            String text=r.getParameter("text");
            IOUtils.copy(text,target);
            Thanks thanks=new Thanks(id);
            r.write(r.outputHTML(true, "page/GFeedback.xhtml").render(thanks));
        }

    }
}
