package uk.ac.ebi.quickgo.web;

import org.mortbay.jetty.*;
import org.mortbay.jetty.handler.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class QuickGOJetty {
    public static void main(String[] args) throws Exception {
        String quickGOConfig = "quickgo-config-mini.xml";
        String root="";
        String proxy=null;
        String ssoConfig=null;
        String password=null;
        boolean delayStart=false;
        int port = 8000;
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.equals("--config")) quickGOConfig = args[i++];
            if (arg.equals("--sso")) ssoConfig = args[i++];
            if (arg.equals("--port")) port = Integer.parseInt(args[i++]);
            if (arg.equals("--root")) root = args[i++];
            if (arg.equals("--proxy")) proxy = args[i++];
            if (arg.equals("--password")) password = args[i++];
            if (arg.equals("--delay")) delayStart=true;
        }

        if (!root.startsWith("/")) root="/"+root;
        if (!root.endsWith("/")) root=root+"/";

        final QuickGO quickgo = new QuickGO(quickGOConfig);


        //if (ssoConfig!=null) quickgo.sso=new SingleSignOn(ssoConfig, "QuickGO");

        //quickgo.password=password;


        Server server = new Server(port);
        server.setHandler(new SwitchHandler(root, quickgo, proxy==null?new RedirectHandler(root):new Forwarder(proxy)));
        server.start();
        System.out.println("http://127.0.0.1:"+port+root);

        quickgo.down=delayStart;

        quickgo.run();
        quickgo.monitor.close();
        server.stop();
        

    }

    private static class RedirectHandler extends AbstractHandler {
        private String root;

        public RedirectHandler(String root) {
            this.root = root;
        }

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            if (request.getRequestURI().equals("/")) response.sendRedirect(root+"/");
            else response.sendError(404);
             ((org.mortbay.jetty.Request)request).setHandled(true);
        }
    }

    private static class SwitchHandler extends AbstractHandler {
        private final String root;
        private final QuickGO quickgo;
        private final Handler forwarder;

        public SwitchHandler(String root, QuickGO quickgo, Handler forwarder) {
            this.root = root;
            this.quickgo = quickgo;
            this.forwarder = forwarder;
        }

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            String path=request.getPathInfo();
            if ((path+"/").equals(root)) {response.sendRedirect(root);return;}
            if (path.startsWith(root)) quickgo.dispatcher.dispatch(path.substring(root.length()),request, response);
            else forwarder.handle(target,request, response,dispatch);
            ((org.mortbay.jetty.Request)request).setHandled(true);
        }
    }
}
