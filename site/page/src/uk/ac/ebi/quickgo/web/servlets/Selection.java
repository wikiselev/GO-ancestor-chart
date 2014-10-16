package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.interpro.webutil.tools.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.data.*;

import java.io.*;
import java.util.*;

public class Selection implements Dispatchable {
    public final static String cookieName = "terms";

    public class SelectionPage {
        public int count;
        public String url;
        public List<Term> terms = new ArrayList<Term>();
        public String idList;
    }

    enum Format { standard, json }

    public void process(Request r) throws IOException, ProcessingException {
        DataFiles files = r.getDataFiles();
        if (files == null) {
	        return;
        }

        SelectionPage page = new SelectionPage();

        Terms terms = new Terms(files.ontology);

        terms.addCompressed(WebUtils.getCookieValue(r.request, cookieName));
        terms.addCompressed(r.getCookieValue("t"));
        terms.addCompressed(r.getParameter("a"));
        terms.addAll(r.getParameterValues("id"));

	    if ("export".equals(r.getParameter("action"))) {
		    OutputStream os = r.outputData("text/tab-separated-values", "term_basket.tsv");
		    Writer wr = new OutputStreamWriter(os, "ASCII");
		    terms.write(wr);
		    wr.close();
	    }
	    else {
		    terms.removeAll(r.getParameterValues("remove"));
		    if (r.getParameterValues("empty") != null) {
			    terms.clear();
		    }

		    page.terms = terms.getTerms();
		    page.count = page.terms.size();
		    page.idList = terms.getIdList();
		    String compressed = terms.getCompressed();
		    page.url = "GMultiTerm?a=" + compressed;

		    r.setCookieValue("t", compressed);

		    switch (CollectionUtils.enumFind(r.getParameter("format"), Format.standard)) {
			case json:
				r.write(r.outputJSON().render(page));
				break;
			case standard:
				r.write(r.outputHTML(false, "page/Selection.xhtml").render(page));
				break;
		    }
	    }
    }
}
