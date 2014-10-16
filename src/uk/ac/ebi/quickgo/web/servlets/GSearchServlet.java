package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.interpro.jxbp2.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.GAnnotationServlet;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.data.*;
import static uk.ac.ebi.quickgo.web.data.TextSearch.*;
import uk.ac.ebi.quickgo.web.render.*;

import java.io.*;
import java.util.*;

public class GSearchServlet implements Dispatchable {
    private static Location me = new Location();

    private Dispatcher dispatcher;

    GSearchServlet(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public static class What {
        Term.Ontology ontology;
        boolean go;
        boolean protein;
        boolean ref;
        String name;
        int dbindex;

        private boolean idtype;

        public static What GO(Term.Ontology ontology) {
            What w = new What(ontology == null ? "GO" : ontology.text);
            w.ontology = ontology;
            w.go = true;
            return w;
        }

        public What(String name) {
            this.name = name;
        }

        public static What Protein(String name, int dbindex, boolean idtype) {
            What w = new What(name);
            w.dbindex = dbindex;
            w.protein = true;
            w.idtype = idtype;
            return w;
        }

        public static What Ref(String name) {
            What w = new What(name);
            w.ref = true;
            return w;
        }

        public String toString() {
            return name;
        }
    }

    int upper = 1000;

    public static class Query {
        public String text;

        public int limit;
        public What what;

        public What go = What.GO(null);
        public What protein;
        public What[] ontologies;
        public What[] databases;
        public What pubmed;
        public What interpro;
        public What taxonomy;
        boolean multiTab = false;

        private String[] words;

        public Query(Request req) {
            ontologies = new What[] { What.GO(Term.Ontology.P), What.GO(Term.Ontology.F), What.GO(Term.Ontology.C) };
            databases = new What[req.dataFiles.proteinDatabaseTable.size()];

            for (int i = 0; i < req.dataFiles.proteinDatabaseTable.size(); i++) {
                String dbcode = req.dataFiles.proteinDatabaseTable.read(i);
                boolean idtype = req.dataFiles.proteinIDCodes.contains(dbcode);

                databases[i] = What.Protein(dbcode, i, idtype);
            }
            protein = What.Protein("Protein", -1, false);
            pubmed = What.Ref("Pubmed");
            interpro = What.Ref("InterPro");
            taxonomy = What.Ref("Taxonomy");

            List<What> all = new ArrayList<What>();
            all.addAll(Arrays.asList(ontologies));
            //////all.addAll(Arrays.asList(databases));
            all.add(protein);
            all.add(go);
            all.add(pubmed);                                                                     
            all.add(interpro);
            all.add(taxonomy);

            text = req.getParameter("q");
            if (text == null) {
	            text = req.getParameter("query");
            }
            if (text == null) {
	            text = req.getParameter("selected");
            }
            if (text == null) {
	            text = "";
            }

            String whatName = StringUtils.nvl(req.getParameter("what"), req.getParameter("mode"));
            for (What w : all) {
                if (w.name.equalsIgnoreCase(whatName)) {
	                what = w;
	                break;
                }
            }

            words = TextSearch.split(text);
            try {
                limit = Integer.parseInt(req.getParameter("limit"));
            }
            catch (Exception e) {
                limit = 20;
                multiTab = true;
            }
        }
    }

    public static class Text {
        public boolean hilite() {
            return colour != null;
        }

        public boolean plain() {
            return colour == null;
        }

        public String text;
        public String colour;
        private int to;

        public Text(String text, int from, int to) {
            this.text = text.substring(from, to);
            this.to = to;
        }

        public Text(String text) {
            this.text = text;
        }

        static Text ellipsis = new Text("...");
    }

	public static class TextList {
		public List<Text> components;

		public TextList() {
			this.components = new ArrayList<Text>();	
		}

		public TextList(List<Text> components) {
			this.components = new ArrayList<Text>(components);
		}
	}

    public class Reason {
        public String reason;
        public List<TextList> explanation;

        public Reason(String reason, List<TextList> explanation) {
            this.reason = reason;
            this.explanation = explanation;
        }

	    public Reason(String reason, TextList explanation) {
	        this.reason = reason;
	        this.explanation = new ArrayList<TextList>();
		    this.explanation.add(explanation);
	    }
    }

    public class SearchProtein {
        public String db;
        public String ac;
        public String symbol;
        public String species;
        public TextList description;

        public SearchProtein(String db,String ac, String symbol, String species, TextList description) {
            this.db = db;
            this.ac = ac;
            this.symbol = symbol;
            this.species = species;
            this.description = description;
        }
    }

    public class SearchTerm {
	    public Term term;
        public List<TextList> name;

	    public SearchTerm(Term term, List<TextList> name) {
		    this.term = term;
	        this.name = name;
	    }

	    public SearchTerm(Term term, TextList name) {
		    this.term = term;
	        this.name = new ArrayList<TextList>();
		    this.name.add(name);
	    }

	    public String id() {
		    return term.id();
	    }

	    public String ontology() {
		    return term.aspect.text;
	    }

	    public boolean obsolete() {
		    return term.obsolete();
	    }

	    public boolean notObsolete() {
		    return !term.obsolete();
	    }
    }

    public class SearchRef {
        public String id;
        public TextList name;

        public SearchRef(String id, TextList name) {
            this.id = id;
            this.name = name;
        }
    }

    public class SearchHit implements Comparable<SearchHit> {
        public SearchTerm term;
        public SearchProtein protein;
        public SearchRef ref;

        public String quality;

        private TextSearch.FieldScoreCard result;
        public List<Reason> reasons = new ArrayList<Reason>();

        public SearchHit(TextSearch.FieldScoreCard result) {
            this.result = result;
            quality = result.score + " " + result.tieBreakValue() + " " + result.rownum;
        }

        public int compareTo(SearchHit searchHit) {
            return searchHit.result.compareTo(result);
        }
    }

    public class SearchTab implements Comparable<SearchTab> {
        public String name;
        public What what;
        public int count;
        public int limit;
        public int minScore = 0;
        public List<SearchHit> hits = new ArrayList<SearchHit>();
        public boolean selected;
        public boolean overflowed;

        public String id() {
	        return what.name;
        }

        public boolean nothing() {
	        return hits.isEmpty();
        }

	    public boolean something() {
		    return !hits.isEmpty();
	    }

	    public int upper;

        public boolean more() {
	        return overflowed && limit < upper;
        }

        public List<FieldScoreCard> top = new ArrayList<FieldScoreCard>();

        public int topScore() {
	        return top.isEmpty() ? 0 : top.get(0).score;
        }

        public SearchTab(What what, int limit, int upper) {
            this.name = what.name;
            this.limit = limit;
            this.what = what;
            this.upper = upper;
        }

        public int compareTo(SearchTab searchTab) {
            if (!top.isEmpty() && !searchTab.top.isEmpty()) {
                int c = top.get(0).compareTo(searchTab.top.get(0));
                if (c != 0) {
	                return c;
                }
            }
            return count - searchTab.count;
        }

        public boolean possible(FieldScoreCard card) throws IOException {
            return card.score >= minScore;
        }

        public void record(FieldScoreCard card) throws IOException {
            int i = top.size();
            while (i > 0) {
                if (card.compareTo(top.get(i - 1)) < 0) {
	                break;
                }
                i--;
            }
            FieldScoreCard reject = null;
            if (i < limit) {
                top.add(i, card.copy());
                if (top.size() > limit) {
	                reject = top.remove(limit);
                }
            }
            else {
	            reject = card;
            }

            if (reject != null) {
                overflowed = true;
                if (minScore < reject.score) {
	                minScore = reject.score;
                }
            }

            count++;
        }
    }

	public class TermListPage {
		public List<Term> terms = new ArrayList<Term>();

		private DataFiles df;
		private Term.Ontology ontology;

		public TermListPage(Query query, DataFiles df) throws Exception {
			this.df = df;
			this.ontology = Term.Ontology.fromString(query.what.toString());
		}

		public void search() {
			for (Term t : df.ontology.terms) {
				if (t.aspect == ontology && !t.obsolete()) {
					terms.add(t);
				}
			}
		}

		public String ontology() {
			return this.ontology.description;
		}
	}

    public class SearchPage {
        public List<String> alternatives = new ArrayList<String>();
        public List<Match> matches = new ArrayList<Match>();
        public List<SearchTab> termTabs = new ArrayList<SearchTab>();
        public List<SearchTab> proteinTabs = new ArrayList<SearchTab>();
        public List<SearchTab> refTabs = new ArrayList<SearchTab>();
        public List<SearchTab> taxTabs = new ArrayList<SearchTab>();
        public SearchTab overall;
        public SearchTab go;
        public SearchTab protein;

        public boolean singleTab() {
	        return !multiTab;
        }

        public Query query;

        public int count;
        public int show;
        public What what;
        public boolean multiTab;
        public String first;

        public String proteinSelected() {
	        return first.equals("protein") ? "selected" : "unselected";
        }
        public String goSelected() {
	        return first.equals("term") ? "selected" : "unselected";
        }
        public String refSelected() {
	        return first.equals("ref") ? "selected" : "unselected";
        }

        private DataFiles df;

        public GAnnotationServlet.AnnotationParameters parameters;

        public SearchPage(Query query, DataFiles df, boolean full) {
            this.query = query;
            this.df = df;
            this.what = query.what;
            this.multiTab = query.multiTab;

            if (full) {
				GAnnotationServlet.AnnotationRequest annotation = new GAnnotationServlet.AnnotationRequest(GAnnotationServlet.Column.defaultColumns);
				annotation.advancedQuery(query.text);
				parameters = new GAnnotationServlet.AnnotationParameters(df, annotation);
            }
        }

        void merge() {
            overall = new SearchTab(new What("All"), query.limit, upper);
            overall.hits.addAll(go.hits);
            overall.hits.addAll(protein.hits);
            for (SearchTab tab : refTabs) {
	            overall.hits.addAll(tab.hits);
            }

            Collections.sort(overall.hits);

            overall.hits = overall.hits.subList(0, Math.min(query.limit, overall.hits.size()));
        }

        void goSearch(List<Closeable> c) throws IOException {
            List<Match> readers = new ArrayList<Match>();

            df.termXrefSearch.search(c, query.words, readers, termID, false);
            df.nameSearch.search(c, query.words, readers, termName, true);
            df.definitionSearch.search(c, query.words, readers, termDefinition, true);
            df.synonymSearch.search(c, query.words, readers, termSynonym, true);
            matches.addAll(readers);

            FieldScoreCard card = new FieldScoreCard(df.ontology.termTieBreak);

            go = null;

            if (query.what == null || query.multiTab || (query.what.go && query.what.ontology == null)) {
	            go = new SearchTab(query.go, query.limit, upper);
            }
            Map<Term.Ontology, SearchTab> tabs = new EnumMap<Term.Ontology, SearchTab>(Term.Ontology.class);
            if (query.what == null || (query.multiTab && query.what.ontology == null)) {
                for (What w : query.ontologies) {
	                tabs.put(w.ontology, new SearchTab(w, query.limit, upper));
                }
            }
            else {
                if (query.what.go && query.what.ontology != null)
                    tabs.put(query.what.ontology, new SearchTab(query.what, query.limit, upper));
            }

            Find f = Or.or(readers);
            if (f != null) {
                int rownum = -1;
                Action a = me.start("GO scan");
                while ((rownum = f.next(rownum + 1)) < Integer.MAX_VALUE) {
                    card.calculate(rownum, readers);

                    if (go != null) {
	                    go.record(card);
                    }

                    Term term = df.ontology.terms[rownum];
                    if (term == null) {
                        me.note("Failed " + rownum);
                        continue;
                    }
                    SearchTab tab = tabs.get(term.aspect);
                    if (tab != null) {
	                    tab.record(card);
                    }
                }
                me.stop(a);
            }

            termTabs.addAll(tabs.values());
            if (go != null) {
	            termTabs.add(go);
            }

            for (SearchTab tab : termTabs) {
                Action a = me.start("Result preparation " + tab.name);
                for (FieldScoreCard result : tab.top) {
                    int index = result.rownum;
                    Term term = df.ontology.terms[index];
                    if (term != null) {
						SearchHit sh = new SearchHit(result);
	                    sh.term = new SearchTerm(term, standardHilite.hilite(term.name(), query.words));

						if (TextSearch.findField(result.hits, termID)) {
							sh.reasons.add(new Reason("ID", standardHilite.hilite(term.xrefsText(), query.words)));
						}
						if (TextSearch.findField(result.hits, termDefinition)) {
							sh.reasons.add(new Reason("Definition", standardHilite.hilite(term.definition, query.words)));
						}
						if (TextSearch.findField(result.hits, termSynonym)) {
							sh.reasons.add(new Reason("Synonym", standardHilite.hilite(term.synonymText(), query.words)));
						}
						tab.hits.add(sh);
                    }
                }
                me.stop(a);
            }
        }

        private void proteinSearch(List<Closeable> c) throws IOException {
            TextTableReader.Cursor taxonomyCursor = df.taxonomy.open(c);
            IntegerTableReader.Cursor proteinTaxonomyCursor = df.proteinTaxonomy.use();
            TextTableReader.Cursor proteinInfoCursor = df.proteinInfo.open(c);
            IntegerTableReader.Cursor proteinIDMapCursor = df.proteinIDMap.open(c);
            TextTableReader.Cursor proteinIDCursor = df.proteinIDs.use();

            List<Match> readers = new ArrayList<Match>();

            //df.descriptionSearch.search(c,query.words,readers,proteinDescription,true);
            df.geneSearch.search(c,query.words,readers,proteinGene,true);
            df.proteinIDSearch.search(c,query.words, readers, proteinID,true);
	        df.proteinNameSearch.search(c, query.words, readers, proteinName, true);
			df.dbObjectNameSearch.search(c, query.words, readers, dbObjectName, true);
	        df.dbObjectSymbolSearch.search(c, query.words, readers, dbObjectSymbol, true);
	        //df.dbObjectSynonymSearch.search(c, query.words, readers, dbObjectSynonym, true);

            matches.addAll(readers);

            FieldScoreCard card=new FieldScoreCard(null);

            SearchTab[] tabs;
            protein = null;

            if (query.what == null || query.what.dbindex < 0) {
                protein = new SearchTab(query.protein, query.limit, upper);
                tabs = new SearchTab[2];
                for (int i = 0; i < tabs.length; i++)
                    tabs[i] = new SearchTab(query.databases[i], query.limit, upper);
            }
            else {
                tabs = new SearchTab[] { new SearchTab(query.what, query.limit, upper) };
            }

            int[] taxScore = taxSearch(c);

            Find[] tabIndex = new Find[tabs.length];

            for (int i = 0; i < tabIndex.length; i++) {
                tabIndex[i] = df.proteinDBIndex.open(c, tabs[i].what.dbindex);
            }

            List<Match> scoreReaders = new ArrayList<Match>(readers);
            Collections.sort(scoreReaders);

            Find f = Or.or(scoreReaders);
            if (f == null) {
	            f = new None();
            }

            int rownum = -1;
            Action a = me.start("Protein scan");
            boolean fast = query.text.contains("fastsearch");
            boolean notax = query.text.contains("notaxsearch");
            boolean nodb = query.text.contains("nodbsearch");
            boolean noall = query.text.contains("noallsearch");
            boolean nostop = query.text.contains("nostopsearch");

            while ((rownum = f.next(rownum + 1)) < Integer.MAX_VALUE) {
                if (!fast) {
                    int minScore = Integer.MAX_VALUE;
                    card.calculate(rownum, readers);

                    if (!notax) {
	                    int[] ia = proteinTaxonomyCursor.read(rownum);
	                    if (ia != null) {
                            card.score += taxScore[ia[0]];
	                    }
                    }

                    if (!noall && protein != null) {
                        protein.record(card);
                        minScore = Math.min(protein.minScore, minScore);
                    }

                    if (!nodb) {
                        for (int i = 0; i < tabs.length; i++) {
                            SearchTab st = tabs[i];
                            if (st.possible(card) && tabIndex[i].next(rownum) == rownum) {
                                st.record(card);
                            }
                            minScore = Math.min(st.minScore,minScore);
                        }
                    }

                    // discard a number of readers based on the current minimum score
                    // where the sum of the scores of the discarded readers <= minScore
                    // discard lowest score readers first
                    if (!nostop) {
                        for (Match r : scoreReaders) {
                            minScore -= r.score;
                            if (minScore >= 0) {
	                            r.stop(rownum);
                            }
                        }
                    }
                }



            }
            me.stop(a);

            for (SearchTab tab : tabs) {
                if (tab.count > 0 || tabs.length == 1) {
	                proteinTabs.add(tab);
                }
            }
            if (protein != null) {
	            proteinTabs.add(protein);
            }

            a = me.start("Result preparation");

            for (SearchTab proteinTab : proteinTabs) {
                for (FieldScoreCard result : proteinTab.top) {
                    int index = result.rownum;

	                String taxName = "";
	                int[] ia = proteinTaxonomyCursor.read(index);
	                if (ia != null) {
                        taxName = taxonomyCursor.read(ia[0])[0];
	                }

                    String[] info = proteinInfoCursor.read(index);
                    TextList description = standardHilite.hilite(info[1], query.words);

                    int[] map = proteinIDMapCursor.read(index);
                    What idDatabase = null;

                    // If this database doesn't have ids, find the first one which does have an id for this protein
                    if (proteinTab.what.idtype) {
	                    idDatabase = proteinTab.what;
                    }
                    else {
                        for (What db : query.databases) {
                            if (map[db.dbindex] > 0 && db.idtype) {
                                idDatabase = db;
                                break;
                            }
                        }
                    }

                    // No ID found? ignore this protein
                    if (idDatabase == null) {
	                    continue;
                    }

                    String id = proteinIDCursor.read(map[idDatabase.dbindex])[0];
                    String db = df.proteinDatabaseTable.read(idDatabase.dbindex);

                    SearchHit sh = new SearchHit(result);
                    sh.protein = new SearchProtein(db,id,info[0], taxName, description);

                    proteinTab.hits.add(sh);
                }
            }

            me.stop(a);
        }

        private void refSearch(List<Closeable> c) throws IOException {
            pubmedSearch(c);
            interproSearch(c);
        }

        private void pubmedSearch(List<Closeable> c) throws IOException {
            TextTableReader.Cursor pubmedCursor = df.pubmedInfo.open(c);

            List<Match> readers = new ArrayList<Match>();

            df.pubmedTitleSearch.search(c, query.words, readers, pubmedTitle, true);
            matches.addAll(readers);

            FieldScoreCard card = new FieldScoreCard(null);
            SearchTab pubmed = new SearchTab(query.pubmed, query.limit, upper);

            Find f = Or.or(readers);
            if (f == null) {
	            f = new None();
            }

            int rownum = -1;
            Action a = me.start("Pubmed scan");
            while ((rownum = f.next(rownum + 1)) < Integer.MAX_VALUE) {
                card.calculate(rownum, readers);
                pubmed.record(card);
            }
            me.stop(a);

            a = me.start("Result preparation");

            for (FieldScoreCard result : pubmed.top) {
                int index = result.rownum;
                String[] info = pubmedCursor.read(index);

                SearchHit sh = new SearchHit(result);
                sh.ref = new SearchRef(info[0], standardHilite.hilite(info[1], query.words));
                pubmed.hits.add(sh);
            }
            me.stop(a);
            refTabs.add(pubmed);
        }

        private void interproSearch(List<Closeable> c) throws IOException {
            TextTableReader.Cursor interproCursor = df.interproInfo.open(c);

            List<Match> readers = new ArrayList<Match>();

            df.interproSearch.search(c, query.words, readers, interproName, true);
            matches.addAll(readers);

            FieldScoreCard card = new FieldScoreCard(null);
            SearchTab interpro = new SearchTab(query.interpro, query.limit, upper);

            Find f = Or.or(readers);
            if (f == null) {
	            f = new None();
            }

            int rownum = -1;
            Action a = me.start("InterPro scan");
            while ((rownum = f.next(rownum + 1)) < Integer.MAX_VALUE) {
                card.calculate(rownum, readers);
                interpro.record(card);
            }
            me.stop(a);

            a = me.start("Result preparation");

            for (FieldScoreCard result : interpro.top) {
                int index = result.rownum;
                String[] info = interproCursor.read(index);

                SearchHit sh = new SearchHit(result);
                sh.ref = new SearchRef(info[0], standardHilite.hilite(info[1], query.words));
                interpro.hits.add(sh);
            }

            me.stop(a);
            refTabs.add(interpro);
        }

        private int[] taxSearch(List<Closeable> c) throws IOException {
            TextTableReader.Cursor taxonomyCursor = df.taxonomy.open(c);

            List<Match> readers = new ArrayList<Match>();

            df.taxonomySearch.search(c, query.words, readers,taxonomy, true);
            matches.addAll(readers);

            FieldScoreCard card = new FieldScoreCard(null);
            SearchTab taxonomy = new SearchTab(query.taxonomy, query.limit, upper);

            Find f = Or.or(readers);
            if (f == null) {
	            f = new None();
            }

            int rownum = -1;
            Action a=me.start("Taxonomy scan");
            int[] taxScore=new int[taxonomyCursor.size()];
            while ((rownum = f.next(rownum + 1)) < Integer.MAX_VALUE) {
                card.calculate(rownum, readers);
                taxScore[rownum] = card.score;
                taxonomy.record(card);
            }
            me.stop(a);

            a = me.start("Result preparation");

            for (FieldScoreCard result : taxonomy.top) {
                int index = result.rownum;
                String[] info = taxonomyCursor.read(index);

                SearchHit sh = new SearchHit(result);
                sh.ref = new SearchRef(String.valueOf(index), standardHilite.hilite(info[0] + " " + info[1], query.words));
                taxonomy.hits.add(sh);
            }
            me.stop(a);

            taxTabs.add(taxonomy);
            return taxScore;
        }
    }

    enum Format { standard, json, xml, termlist, mini }

    public void process(Request r) throws Exception {
        DataFiles dataFiles = r.getDataFiles();
        if (dataFiles == null) {
	        return;
        }

        Query query = new Query(r);

        Format format = CollectionUtils.enumFind(r.getParameter("format"), Format.standard);

        if (format == Format.standard && (query.what != null && query.what.protein)) {
            String ac = query.text;
            int code = dataFiles.proteinIDs.use().search(new String[]{ ac });
            if (code > 0) {
                IndexReader.ValueRead vr = dataFiles.proteinIDIndex.open(r.connection, code);
                if (vr.count() == 1) {
                    dispatcher.protein.fullPage(vr.next(0), ac, dataFiles, r);
                    return;
                }
            }
        }

        if (format == Format.standard && (query.what != null && query.what.go) && (query.text.length() <= 7)) {
            String id = "GO:0000000".substring(0, 10 - query.text.length()) + query.text;
            if (dataFiles.termIDs.search(id) >= 0) {
                dispatcher.term.termPage(id, dataFiles, r);
                return;
            }
        }

        switch (format) {
        case standard:
	        standardSearch(query, r, dataFiles);
	        break;
        case mini:
	        miniSearch(query, r,dataFiles);
	        break;
        case json:
	        jsonSearch(query, r, dataFiles);
	        break;
        case xml:
	        xmlSearch(query, r, dataFiles);
	        break;
		case termlist:
			termlistSearch(query, r, dataFiles);
			break;
        }
    }

    //public static Hilite miniHilite = new Hilite(50);
    public static Hilite standardHilite = new Hilite();

    private void xmlSearch(Query query, Request r, DataFiles dataFiles) throws IOException, ProcessingException {
        SearchPage searchPage = new SearchPage(query, dataFiles, false);
        search(query, searchPage, r.connection);
        r.write(r.outputXML((query.what != null && query.what.protein) ? "page/GSearchProteinXML.xhtml" : "page/GSearchTermXML.xhtml").render(query, searchPage));
    }

    private void standardSearch(Query query, Request r, DataFiles dataFiles) throws IOException, ProcessingException {
        boolean full = r.getParameter("embed") == null;
        SearchPage searchPage = new SearchPage(query, dataFiles, full);
        search(query, searchPage,r.connection);
        r.write(r.outputHTML(full, "page/GSearch.xhtml").render(query,searchPage));
    }
    
    private void miniSearch(Query query, Request r, DataFiles dataFiles) throws IOException, ProcessingException {
        SearchPage searchPage = new SearchPage(query, dataFiles, false);
        search(query, searchPage, r.connection);
        searchPage.merge();
        r.write(r.outputHTML(false, "page/GSearchMini.xhtml").render(query,searchPage));
    }

    private void jsonSearch(Query query, Request r, DataFiles dataFiles) throws IOException {
        SearchPage searchPage = new SearchPage(query, dataFiles, false);
        search(query, searchPage, r.connection);
        r.write(r.outputJSON().render(searchPage));
    }

	private void termlistSearch(Query query, Request r, DataFiles dataFiles) throws IOException, ProcessingException, Exception {
		TermListPage termListPage = new TermListPage(query, dataFiles);
		termListPage.search();
		r.write(r.outputHTML(false, "page/GTermList.xhtml").render(termListPage));
	}

    public static class Hilite {
        static ColourList colour = new ColourList(0xccffffff);

        public Hilite() {
        }

        public Hilite(int characterLimit) {
            this.characterLimit = characterLimit;
        }

        int characterLimit = 65536;

        TextList hilite(String source, String[] words) {
            List<String> seek = Arrays.asList(words);
            List<Text> text = new ArrayList<Text>();

            int start = 0;

            for (String s : TextSearch.split(source)) {
                int next = source.indexOf(s, start);
                if (next >= 0) {
					if (start < next) {
						text.add(new Text(source, start, next));
					}
					int end = next + s.length();
					text.add(hilite(new Text(source, next, end), seek));
					start = end;
                }
            }

            if (start < source.length()) {
	            text.add(hilite(new Text(source, start, source.length()), seek));
            }

            text = compact(autoTrim(text));
            return new TextList(text);
        }

	    List<TextList> hilite(List<String> source, String[] words) {
		    ArrayList<TextList> ta = new ArrayList<TextList>();
		    for (String s : source) {
		        ta.add(hilite(s, words));
		    }
		    return ta;
	    }

        private Text hilite(Text word, List<String> seek) {
            int wordNumber = find(word.text, seek);
            word.colour = wordNumber < 0 ? null : colour.getColourCSS(wordNumber);
            return word;
        }

        private int find(String word, List<String> seek) {
            for (int i = 0; i < seek.size(); i++) {
                if (word.toUpperCase().startsWith(seek.get(i).toUpperCase())) {
	                return i;
                }
            }
            return -1;
        }

        /**
         * Concatenate any sequential non-hilited text elements.
         *
         * @param input list of text elements to be compacted
         * @return compacted list text elements.
         */

        private List<Text> compact(List<Text> input) {
            List<Text> output = new ArrayList<Text>();
            StringBuilder current = new StringBuilder();
            for (Text text : input) {
                if (text.colour ==null) {
	                current.append(text.text);
                }
                else {
                    output.add(new Text(current.toString()));
                    current.setLength(0);
                    output.add(text);
                }
            }
            output.add(new Text(current.toString()));
            return output;
        }

        /**
         * Ensure a list of text elements doesn't exceed the character limit by removing elements
         * not close to a hilited element.
         *
         * Elements are protected if they are within three elements of a hilited element.
         *
         * @param input List of text elements
         * @return trimmed list of elements
         */

        private List<Text> autoTrim(List<Text> input) {
            List<Text> output = new ArrayList<Text>();
            boolean ellipsis = false;
            int size = 0;
            for (int i = input.size() - 1; i >= 0; i--) {
                Text text = input.get(i);
                boolean ok = text.to + size < characterLimit;
                if (!ok) {
                    for (int j = -3; j <= 3; j++) {
	                    if (i + j >= 0 && i + j < input.size() && input.get(i + j).hilite()) {
		                    ok = true;
	                    }
                    }
                }
                Text l = null;
                if (ok) {
	                l = text;
                }
                else {
                    if (!ellipsis) {
	                    l = Text.ellipsis;
                    }
                    ellipsis = true;
                }
                if (l != null) {
                    size += l.text.length();
                    output.add(0, l);
                }

            }
            return output;
        }
    }

    SearchField termID = new SearchField(100, "GO ID");
    SearchField termName = new TextSearch.SearchField(38, "GO Name");
    SearchField termDefinition = new SearchField(16, "GO Definition");
    //SearchField termComment = new SearchField(12, "GO Comment");
    SearchField termSynonym = new SearchField(20, "GO Synonym");

    SearchField proteinID = new TextSearch.SearchField(50, "Protein ID");
    SearchField proteinGene = new SearchField(18, "Protein Gene");
    //SearchField proteinDescription = new SearchField(12, "Protein description");
	SearchField proteinName = new SearchField(50, "Protein name");
	SearchField dbObjectName = new SearchField(12, "DB Object name");
	SearchField dbObjectSymbol = new SearchField(12, "DB Object symbol");
	//SearchField dbObjectSynonym = new SearchField(12, "DB Object synonym");
    SearchField taxonomy = new SearchField(10, "Taxonomy");

    SearchField pubmedTitle = new TextSearch.SearchField(18, "Pubmed Title");
    SearchField interproName = new TextSearch.SearchField(18, "InterPro Name");

    private void search(Query query, SearchPage searchPage, List<Closeable> connection) throws IOException {
        if (query.what==null || query.what.go) {
	        searchPage.goSearch(connection);
        }

        if (query.what==null || query.what.protein) {
	        searchPage.proteinSearch(connection);
        }

        if (query.what==null || query.what.ref) {
	        searchPage.refSearch(connection);
        }

        Collections.sort(searchPage.termTabs, Collections.reverseOrder());
        Collections.sort(searchPage.proteinTabs, Collections.reverseOrder());
        Collections.sort(searchPage.refTabs, Collections.reverseOrder());

        int termScore = topScore(searchPage.termTabs);
        int proteinScore = topScore(searchPage.proteinTabs);
        int refScore = topScore(searchPage.refTabs);

        if (termScore > proteinScore && termScore > refScore) {
	        searchPage.first = "term";
        }
        else if (proteinScore > refScore) {
	        searchPage.first = "protein";
        }
        else {
	        searchPage.first = "ref";
        }

        List<Match> alt = new ArrayList<Match>();
        for (Match m : searchPage.matches) {
	        if (m.wildcard) {
		        alt.add(m);
	        }
        }
        Collections.sort(alt, Match.reverseFrequencyOrder);
        for (int i = 0; i < alt.size() && searchPage.alternatives.size() < 10; i++) {
            String text = alt.get(i).makeText(query.words);
            if (!searchPage.alternatives.contains(text)) {
	            searchPage.alternatives.add(text);
            }
        }
    }

    private int topScore(List<SearchTab> tabs) {
        return tabs.size() == 0 ? 0 : tabs.get(0).topScore();
    }

/*
    public static void main(String[] args) {
        Text[] txt = miniHilite.hilite("hello world this is the can has cheese burger kitten collective", new String[] { "kitten", "this" });
        for (Text text : txt) {
            System.out.println(text.colour + " " + text.text);
        }
    }
*/
}
