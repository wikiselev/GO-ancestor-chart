package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.*;

import java.io.*;

public class GProteinServlet implements Dispatchable {
    public class ProteinPage {
        public String ac;
        public String description;
       
        public String geneName;
        public int taxid;
        public String taxName;
    }

    enum Format { standard, json }

    public void process(Request r) throws Exception {
        DataFiles files = r.getDataFiles();
        if (files == null) {
	        return;
        }
        
        String ac = r.getParameter("ac");

        switch (CollectionUtils.enumFind(r.getParameter("format"), Format.standard)) {
        case standard:
	        fullPage(ac, files, r);
	        break;
        case json:
	        jsonPage(ac, r.dataFiles, r);
	        break;
        }
    }

    public class Error {
        public boolean noid() {
	        return id == null;
        }
        public boolean notfound() {
	        return id != null;
        }

        public String id;

        public Error(String id) {
            this.id = id;
        }
    }

    private void fullPage(String ac, DataFiles df, Request r) throws Exception {
        int idIndex = df.proteinIDs.use().search(new String[]{ ac });
        int index = idIndex < 0 ? -1 : df.proteinIDIndex.open(r.connection, idIndex).next(0);
        fullPage(index, ac, df, r);
    }

    private void jsonPage(String ac, DataFiles df, Request r) throws Exception {
        int idIndex = df.proteinIDs.use().search(new String[]{ ac });
        int index = idIndex < 0 ? -1 : df.proteinIDIndex.open(r.connection, idIndex).next(0);
        if (index < 0) {
            r.write(r.outputJSON().render(null));
        }
	    else {
			ProteinPage page = makeProteinPage(ac, df, r, index);
			r.write(r.outputJSON().render(page));
        }
    }

    public void fullPage(int index, String ac, DataFiles df, Request r) throws IOException, ProcessingException {
        if (index < 0) {
            r.write(r.outputHTML(true, "page/GNoProtein.xhtml").render(new Error(ac)));
        }
	    else {
	        ProteinPage proteinPage = makeProteinPage(ac, df, r, index);
	        GAnnotationServlet.AnnotationRequest annotation = new GAnnotationServlet.AnnotationRequest(GAnnotationServlet.Column.defaultColumns);
	        annotation.protein(ac);
	        GAnnotationServlet.AnnotationParameters parameters = new GAnnotationServlet.AnnotationParameters(df, annotation);
	        r.write(r.outputHTML(true, "page/GProtein.xhtml").render(proteinPage,parameters));
        }
    }

    private ProteinPage makeProteinPage(String ac, DataFiles df, Request r, int index) throws IOException {
        ProteinPage proteinPage = new ProteinPage();
        proteinPage.ac = ac;

        String[] info = df.proteinInfo.open(r.connection).read(index);
        int[] taxId = df.proteinTaxonomy.use().read(index);

        proteinPage.description = info[1];
        proteinPage.geneName = info[0];
        proteinPage.taxid = taxId[0];
        proteinPage.taxName = df.taxonomy.open(r.connection).read(taxId[0])[0];
        return proteinPage;
    }
}
