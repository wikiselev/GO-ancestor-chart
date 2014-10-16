package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import javax.servlet.http.*;

public class GMultiProteinServlet implements Dispatchable {
    public class ProteinsPage {

        }


    enum Format {standard};

    public void process(Request r) throws Exception {

        DataFiles files = r.getDataFiles();
        if (files==null) return;


        String[] ids=r.getParameterValues("id");
        switch (CollectionUtils.enumFind(r.getParameter("format"), Format.standard)) {
        case standard:standardPage(ids, files, r);break;
        }
    }

    /*
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                Request r=QuickGO.start(getServletContext(), request, response);
                if (r==null) return;
                try {

                } catch (Exception e) {
                    r.error(e);
                } finally {r.close();}
            }
*/
    private void standardPage(String[] ids, DataFiles df, Request r) throws Exception {
        ProteinsPage proteinsPage=new ProteinsPage();
        r.write(r.outputHTML(true,"page/GMultiProtein.xhtml").render(proteinsPage));
    }

}
