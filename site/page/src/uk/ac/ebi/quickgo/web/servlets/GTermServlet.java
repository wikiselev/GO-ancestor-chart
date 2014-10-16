package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.render.*;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.*;

import java.util.*;

public class GTermServlet implements Dispatchable  {
    public class Ancestor {
        public Term term;
        public int colspan;
        public String[] relations;

        public Ancestor(Term term, int colspan) {
            this.term = term;
            this.colspan = colspan;
        }
    }

    public class TermPage{
        public Object termInfo = new JSONSerialise() {public Object serialise() {return term;}};
        public Term term;
        public Ancestor[] ancestry;

        TermPage(DataFiles df,String id) {
            if (id == null) return;
            term = df.ontology.getTerm(id);
        }

        void loadAncestors() {
            if (term==null) return;
            List<Term> terms=term.getAllAncestors();

            Collections.sort(terms, Term.ancestorComparator);

            ancestry=new Ancestor[terms.size()];
            int[] childCount=new int[terms.size()];
            for (int i = 0; i < terms.size(); i++) {
                Term child = terms.get(i);
                for (TermRelation cr : child.children) if (terms.contains(cr.child)) childCount[i]++;
                Ancestor a = new Ancestor(child,terms.size()-i);
                ancestry[i]=a;
                a.relations=new String[i];
                int parentCount=child.parents.size();
                for (int j = i-1; j >=0; j--) {
                    Term parent = terms.get(j);
                    String relation=null;

                    for (TermRelation termRelation : child.parents) {
                        if (termRelation.parent==parent) {
                            relation=termRelation.typeof.code;                        
                            childCount[j]--;
                            parentCount--;
                        }
                    }
                    if (relation==null) {
                        if (parentCount==0 && childCount[j]==0) relation="0";
                        else if (childCount[j]==0) relation="0h";
                        else if (parentCount==0) relation="0v";
                        else relation="0hv";
                    }

                    a.relations[j]=relation;
                }

            }
        }


    }

    enum Format { standard, json, oldxml, obo, oboxml, mini, jsonMinimal }

    public void process(Request r) throws Exception {

        DataFiles files = r.getDataFiles();
        if (files==null) return;

        String id=r.getParameter("id");

        switch (CollectionUtils.enumFind(r.getParameter("format"), Format.standard)) {
        case standard:
	        termPage(id, files, r);
	        break;
        case mini:
	        miniPage(id, files, r);
	        break;
        case oldxml:
	        oldXMLPage(id, files, r);
	        break;
        case oboxml:
	        oboXMLPage(id, files, r);
	        break;
        case obo:
	        oboTextPage(id, files, r);
	        break;
        case json:
	        jsonPage(id, files, r);
	        break;
        case jsonMinimal:
	        jsonMinimalPage(id, files, r);
	        break;
        }
    }

    public class Error {
        public boolean noid() {return id==null;}
        public boolean notfound() {return id!=null;}
        public String id;

        public Error(String id) {
            this.id = id;
        }
    }

    public void jsonPage(String id, DataFiles df, Request r) throws Exception {
        TermPage page = new TermPage(df, id);
	    page.loadAncestors();
        r.write(r.outputJSON().render(page));
    }

	public void jsonMinimalPage(String id, DataFiles df, Request r) throws Exception {
	    Term term = df.ontology.getTerm(id);
	    r.write(r.outputJSON().render(term));
	}

    public void termPage(String id, DataFiles df, Request r) throws Exception {
        if (r.getParameter("embed") != null) {
	        miniPage(id, df, r);
	        return;
        }

        TermPage page = new TermPage(df,id);

        if (page.term==null) {
            r.write(r.outputHTML(true,"page/GNoTerm.xhtml").render(new Error(id)));
            return;
        }

        page.loadAncestors();

        //termPage.hierarchy= new HierarchyGraph(term).layout(r.configuration.imageArchive);

        GAnnotationServlet.AnnotationRequest annotation = new GAnnotationServlet.AnnotationRequest(GAnnotationServlet.Column.defaultColumns);
        annotation.term(page.term.id());
        GAnnotationServlet.AnnotationParameters parameters = new GAnnotationServlet.AnnotationParameters(df, annotation);

        r.write(r.outputHTML(true,"page/GTerm.xhtml")
                .render(page,page.term,parameters));
    }

    private void miniPage(String id, DataFiles df, Request r) throws Exception {
        TermPage termPage=new TermPage(df,id);

        //termPage.hierarchy= new HierarchyGraph(term).layout(r.configuration.imageArchive);
        r.write(r.outputHTML(false,"page/GTermMini.xhtml")
                .render(termPage,termPage.term));
    }

    private void oldXMLPage(String id, DataFiles df, Request r) throws Exception {
        TermPage termPage=new TermPage(df,id);

        r.write(r.outputXML("page/GTermOldXML.xml").render(termPage,termPage.term));
    }

    private void oboXMLPage(String id, DataFiles df, Request r) throws Exception {
        TermPage termPage = new TermPage(df,id);
        r.write(r.outputXML("page/GTermOBOxml.xml").render(termPage, termPage.term));
    }

    private void oboTextPage(String id, DataFiles df, Request r) throws Exception {
        TermPage termPage=new TermPage(df,id);

        r.write(r.outputText("text/plain","page/GTermOBO.xml").render(termPage, termPage.term));
    }
}
/*
biggest hierarchies as of Sep 2007

90	GO:0001814
89	GO:0001815
76	GO:0002853
75	GO:0002854
72	GO:0002859
71	GO:0002860
69	GO:0001813
68	GO:0002847
68	GO:0002848
67	GO:0001797

often used term as of Oct 2007:

select * from (select go_id,count(*),count(distinct protein_ac) from go.protein2go group by go_id order by count(distinct protein_ac) desc) where rownum<=20;

GO_ID        COUNT(*) COUNT(DISTINCTPROTEIN_AC)
---------- ---------- -------------------------
GO:0016020     931313                    574653
GO:0016021     716159                    504091
GO:0016491     667189                    446914
GO:0016740     394213                    374946
GO:0005524     868862                    360155
GO:0006810     471357                    360085
GO:0003824     437271                    341644
GO:0003677     625697                    328882
GO:0016787     341516                    297995
GO:0006118     608801                    296908
GO:0000166     474127                    296755
GO:0008152     367538                    280064
GO:0046872     253769                    236072
GO:0006355     443750                    214735
GO:0003723     266462                    198879
GO:0005622     240222                    198862
GO:0005739     205496                    195895
GO:0005737     292850                    188140
GO:0005506     228752                    187579
GO:0006350     220734                    175118

20 rows selected.

Elapsed: 00:03:42.88


Longest name: GO:0016706
Longest definition: GO:0031556

 */