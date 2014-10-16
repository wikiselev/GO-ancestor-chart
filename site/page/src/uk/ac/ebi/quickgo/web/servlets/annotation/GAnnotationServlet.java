package uk.ac.ebi.quickgo.web.servlets.annotation;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import uk.ac.ebi.interpro.common.Creator;
import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.interpro.common.URLUtils;
import uk.ac.ebi.interpro.common.collections.AutoMap;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.interpro.common.performance.Location;
import uk.ac.ebi.interpro.exchange.compress.IntegerTableReader;
import uk.ac.ebi.interpro.exchange.compress.TextTableReader;
import uk.ac.ebi.interpro.jxbp2.render.Render;
import uk.ac.ebi.quickgo.web.Request;
import uk.ac.ebi.quickgo.web.configuration.Configuration;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.data.CV;
import uk.ac.ebi.quickgo.web.data.EnumCategory;
import uk.ac.ebi.quickgo.web.data.Term;
import uk.ac.ebi.quickgo.web.data.Terms;
import uk.ac.ebi.quickgo.web.graphics.ExcelExporter;
import uk.ac.ebi.quickgo.web.render.Input;
import uk.ac.ebi.quickgo.web.render.JSONSerialise;
import uk.ac.ebi.quickgo.web.servlets.Dispatchable;

public class GAnnotationServlet implements Dispatchable {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Location me = new Location();

    static String[] empty = {};

    static String[] loadList(String[]... parameters) {
        ArrayList<String> all = new ArrayList<String>();

        for (String[] values : parameters) {
            if (values != null) {
				for (String v : values) {
					v = v.trim();
					if (v.length() > 0) {
						for (String s : v.split("[, \t\r\n]+")) {
							all.add("{null}".equals(s) ? "" : s);
						}
					}
				}
            }
        }

        return all.isEmpty() ? null : all.toArray(new String[all.size()]);
    }

	private static final String contentTypeTSV = "text/tab-separated-values";
	private static final String contentTypeGZIP = "application/x-gzip";
	private static final String contentTypeXLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    static class DownloadFormat {
        private String start;
        Column[] columns;
        String separator;
        String endOfLine;
        String missing;
        String fileName;
        String mimeType;
        boolean proteinUnique;
        boolean fasta;

        public DownloadFormat(boolean proteinUnique, String separator, String endOfLine, String missing, String start, boolean fasta, Column[] columns, String fileName, String mimeType) {
            this.fasta = fasta;
            this.proteinUnique = proteinUnique;
            this.start = start;
            this.columns = columns;
            this.separator = separator;
            this.endOfLine = endOfLine;
            this.missing = missing;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }

        // Gene2GO: ftp://ftp.ncbi.nlm.nih.gov/gene/README
        static DownloadFormat gene2go = new DownloadFormat(false, "\t", "\n", "-", "", false, Column.gene2goColumns, "gene2go.goa", contentTypeTSV);
        static DownloadFormat proteinList = new DownloadFormat(true, "\t", "\n", "", "", false, Column.proteinListColumns, "proteins.list", contentTypeTSV);
        static DownloadFormat fastaFormat = new DownloadFormat(true, "|", "\n", "", ">", true, Column.fastaColumns, "proteins.fasta", "text/fasta");
    }

    public enum Column {
        proteinDB("DB", "Database"),
        proteinID("ID", "Gene Product ID"),
        splice("Splice", "Product Form ID"),
        proteinSymbol("Symbol"),
        qualifier("Qualifier"),
        goID("GO ID", "GO Identifier"),
        originalTermID("Original GO ID"),
        ref("Reference"),
        evidence("Evidence"),
        with("With"),
        aspect("Aspect"),
        proteinName("Name"),
        proteinSynonym("Synonym"),
        proteinType("Type"),
        proteinTaxon("Taxon"),
        proteinTaxonName("Taxon Name"),
        date("Date"),
        from("Source", "Assigned By"),
        goName("GO Name", "GO Term Name"),
        originalGOName("Original GO Name", "Original GO Term Name"),
        pubmed("PubMed"),
        rowId("RowID"),
        sequence("Sequence");

	    public String title;
	    public String displayName;

	    Column(String title) {
		    this.title = title;
		    this.displayName = title;
	    }

	    Column(String title, String displayName) {
		    this.title = title;
		    this.displayName = displayName;
	    }

	    public static Column[] allColumns = {
			proteinDB, proteinID, proteinSymbol, qualifier, goID, goName, aspect, originalTermID, originalGOName, evidence, ref, with, proteinTaxon,
			date, from, splice, proteinName, proteinSynonym, proteinType, proteinTaxonName, sequence
	    };
	    public static Column[] defaultColumns = {
			proteinDB, proteinID, splice, proteinSymbol, proteinTaxon, qualifier, goID, goName, ref, evidence, with, aspect, date, from
	    };
	    public static Column[] defaultSlimColumns = {
			proteinDB, proteinID, splice, proteinSymbol, proteinTaxon, qualifier, goID, goName, originalTermID, originalGOName, ref, evidence, with, aspect, date, from
	    };
        public static Column[] gene2goColumns = {
			proteinTaxon, proteinID, evidence, qualifier, goName, pubmed, aspect
        };
        public static Column[] proteinListColumns = {
			proteinDB, proteinID, proteinSymbol, proteinName, proteinSynonym, proteinType, proteinTaxon
        };
        public static Column[] fastaColumns = {
			proteinDB, proteinID, proteinSymbol, proteinName
        };
    }

    enum TermUsage {
        ancestor, slim
    }

    enum Select {
        normal, extend, proteins
    }

    public static class AnnotationRequest {
        Filter field(FieldFilter.FieldName field, String[] values) {
            if (values == null || values.length == 0) {
	            return null;
            }
	        else {
	            List<Filter> filters = new ArrayList<Filter>();
	            for (String value : values) {
		            filters.add(new FieldFilter(field, value));
	            }

	            return BooleanFilter.simplify(BooleanFilter.BooleanCombination.or, filters);
            }
        }

        Filter getTermFilter() {
            List<String> ancestralRelations = new ArrayList<String>();
	        if (slimTypes != null) {
		        for (String term : termIDs) {
		            for (int i = 0; i < slimTypes.length(); i++) {
		                ancestralRelations.add(slimTypes.charAt(i) + term);
		            }
		        }
	        }
            return field(FieldFilter.FieldName.ancestor, ancestralRelations.toArray(new String[ancestralRelations.size()]));
        }

        Filter getFilter() {
            Filter f = BooleanFilter.simplify(
                BooleanFilter.BooleanCombination.and,
                advanced == null ? null : advanced.root,
                field(FieldFilter.FieldName.evidence, evidences),
                field(FieldFilter.FieldName.source, sources),
                field(FieldFilter.FieldName.qualifier, qualifiers),
	            field(FieldFilter.FieldName.aspect, aspects),
                field(FieldFilter.FieldName.ref, ref),
                field(FieldFilter.FieldName.with, with),
                field(FieldFilter.FieldName.tax, tax),
                termUsage == GAnnotationServlet.TermUsage.slim ? null : getTermFilter(),
                field(FieldFilter.FieldName.protein, proteins),
                field(FieldFilter.FieldName.db, db),
                field(FieldFilter.FieldName.db, proteinSets)
            );
            if (f == null) {
	            f = new NoFilter();
            }
            if (select == GAnnotationServlet.Select.extend) {
	            f = new Expand(f);
            }

            //System.out.println("Filter: " + f);
            return f;
        }

        AnnotationQuery getQuery() {
            return new AnnotationQuery(db, termUsage == GAnnotationServlet.TermUsage.slim ? termIDs : null, slimTypes, proteins, getFilter());
        }

        AdvancedRequest advanced;

        String[] evidences;
        String[] db;
        String[] tax;
        String[] proteinSets;

        TermUsage termUsage= TermUsage.ancestor;
        Select select = Select.normal;
        String[] sources;
        String[] qualifiers;
	    String[] aspects;
        String[] proteins;
        String[] termIDs;
        String slimTypes;
        
        String[] ref;
        String[] with;
        List<Column> columns = new ArrayList<Column>();
        int start;
        int count;

        int limit;

        boolean aspectSorter;

        // These settings are for debugging and are not compared or hashed
        boolean useCache = true;
        private Format format;

        public static String[] list(Request request, String name) {
            return (request != null) ? loadList(request.getParameterValues(name)) : null;
        }

        public AnnotationRequest(Request request) {
            format = CollectionUtils.enumFind(request.getParameter("format"), Format.form);

            tax = list(request, "tax");
            db = list(request, "db");

            ref = list(request,"ref");
            with = list(request,"with");

            proteins = list(request, "protein");
            proteinSets = list(request, "proteinset");
            evidences = list(request, "evidence");
            sources = list(request, "source");
	        aspects = list(request, "aspect");

            String[] columnNames = loadList(request.getParameterValues("col"), request.getParameterValues("columns"));
            if (columnNames == null) {
	            columns = Arrays.asList(Column.defaultColumns);
            }
            else {
                for (String columnName : columnNames) {
                    try {
                        columns.add(Column.valueOf(columnName));
                    }
                    catch (Exception e) {
                        // column doesn't exist - never mind
                    }
                }
            }

            Terms t = new Terms(request.dataFiles.ontology);
            t.addAll(request.getParameterValues("goid"));
	        t.addAll(request.getParameterValues("term")); // make "term" synonymous with "goid"
            t.addCompressed(request.getParameter("a"));
            termIDs = t.getIDs();
            termUsage = CollectionUtils.enumFind(request.getParameter("termUse"), TermUsage.ancestor);
            slimTypes = StringUtils.nvl(request.getParameter("slimTypes"));

	        if ("".equals(slimTypes)) {
		        String relType = StringUtils.nvl(request.getParameter("relType"), "IPO=");
				slimTypes = "Custom".equals(relType) ? request.getParameter("customRelType") : relType;
	        }

	        // if we're slimming, we want to eliminate all annotations with any form of NOT qualifier
	        final String slimmableQualifiers = "colocalizes_with,contributes_to,{null}";
	        qualifiers = list(request, "qualifier");
	        if (termUsage == TermUsage.slim) {
		        if (qualifiers == null || qualifiers.length == 0) {
			        qualifiers = loadList(new String[] { slimmableQualifiers });
		        }
		        else {
			        ArrayList<String> subset = new ArrayList<String>();

			        for (String q : qualifiers) {
						if (!q.startsWith("NOT")) {
							subset.add(q);
						}
			        }

			        qualifiers = subset.isEmpty() ? loadList(new String[] { slimmableQualifiers }) : subset.toArray(new String[subset.size()]);
		        }
	        }

            select = CollectionUtils.enumFind(request.getParameter("select"), Select.normal);
            aspectSorter = (request.getParameter("aspectSorter") != null);
            start = StringUtils.parseInt(request.getParameter("start"), 0);
            count = aspectSorter ? 1000 : StringUtils.parseInt(request.getParameter("count"), 25);

            limit = StringUtils.parseInt(request.getParameter("limit"), 10000);

            String stats = request.getParameter("stats");
            if (stats != null) {
                useCache = false;
            }

            advanced = new AdvancedRequest(StringUtils.nvl(request.getParameter("q"), request.getParameter("advanced")), slimTypes);
        }


        public AnnotationRequest(Column... columns) {
            this.columns.addAll(Arrays.asList(columns));
        }

        public void term(String term) {
	        termIDs = new String[] { term };
        }

	    public void terms(String[] terms) {
		    termIDs = terms;
	    }

        public void protein(String protein) {
            proteins = new String[]{protein};
            aspectSorter = true;
            count = Integer.MAX_VALUE;
        }

        public void advancedQuery(String text) {
            advanced = new AdvancedRequest(text);
            count = 25;
        }

        public String toString() {
            return "term:"+CollectionUtils.dump(termIDs)+",protein:"+CollectionUtils.dump(proteins)+" "+System.identityHashCode(this);
        }
    }

    public static class AnnotationParameters {
        public DataFiles system;

        public Input input;

        public Map<String, String[]> parameters = new HashMap<String,String[]>();

        public Column[] allColumns;
        public List<Column> columns = new ArrayList<Column>();
		public ArrayList<Terms> sets;

        public int columnCount() {
	        return columns.size();
        }

        public String columnList;
        public boolean aspectSorter;

        public String advanced;

        public CV proteinIDCodes;

        public String url() {
	        return parameters(false);
        }

	    public String parameters(boolean excludeCol) {
	        Map<String, String> p = new HashMap<String, String>();
	        for (String key : parameters.keySet()) {
		        if ("col".equals(key) && excludeCol) continue;

		        String[] values = parameters.get(key);
	            if (values != null && values.length > 0) {
	                p.put(key, CollectionUtils.concat(values, " "));
	            }
	        }
		    return URLUtils.encodeURL("GAnnotation",p);
	    }

        public List<Term> terms=new ArrayList<Term>();

        public AnnotationParameters(DataFiles database,AnnotationRequest rq) {
			sets = new ArrayList<Terms>(database.ontology.slims.values());

            this.system = database;
            if (rq.termIDs != null) {
                for (String id : rq.termIDs) {
					Term term = database.ontology.getTerm(id);
                    if (term != null) {
	                    terms.add(term);
                    }
                }
			}
            allColumns = Column.allColumns;
            columns = rq.columns;
            columnList = CollectionUtils.concat(columns, " ");
            aspectSorter = rq.aspectSorter;

            parameters.put("count", new String[]{ ""+rq.count });

            parameters.put("termUse", new String[]{ rq.termUsage.name() });
            parameters.put("ref", rq.ref);
            parameters.put("with", rq.with);
            parameters.put("qualifier", rq.qualifiers);
            parameters.put("source", rq.sources);
            parameters.put("goid", rq.termIDs);
	        parameters.put("aspect", rq.aspects);
            parameters.put("evidence", rq.evidences);
            parameters.put("protein", rq.proteins);
            parameters.put("proteinset", rq.proteinSets);
            parameters.put("tax", rq.tax);
            String[] columns = new String[rq.columns.size()];
            for (int i = 0; i < columns.length; i++) {
	            columns[i] = rq.columns.get(i).name();
            }
            parameters.put("col", columns);
            parameters.put("select", new String[]{ rq.select.name() });
            parameters.put("db", rq.db);
            if (rq.slimTypes != null) {
	            parameters.put("slimTypes", new String[]{ rq.slimTypes });
            }
            if (rq.advanced != null) {
	            parameters.put("advanced", new String[]{ rq.advanced.text });
            }

            proteinIDCodes = new CV();
            if (rq.db != null) {
                for (String db : rq.db) {
                    proteinIDCodes.add(db, database.proteinIDCodes.get(db));
                }
            }
            proteinIDCodes.add(database.proteinIDCodes);

            input = new Input(parameters);

            advanced = rq.advanced==null ? "" : StringUtils.nvl(rq.advanced.text);
        }

	    public String[] get(String name) {
		    return parameters.get(name);
	    }
    }

    public class SampleAnnotation {
        public boolean multipage;
        public boolean none;
        public int prior;
        public int begin;
        public int end;
        public boolean more;
        public boolean previous;
        public int rowcount;
        public Group root;
        public AdvancedRequest.ParseException parseException;

        public SampleAnnotation(Sampler sampler) {
            this.prior = Math.max(0, sampler.start - sampler.count);
            this.begin = sampler.start + 1;
            this.end = sampler.rowCount;
            this.rowcount = end - begin + 1;
            this.more = sampler.after;
            this.previous = sampler.before;
            this.multipage = more || previous;
            this.none = rowcount == 0;
            this.root = sampler.root;
            this.parseException = sampler.parseException;
        }
    }

    public enum Format {
        form, sample, stats, statsFull, tsv, /*xml,*/ compare, compareJSON, association, gaf, gene2go, proteinList, fasta, gpa, downloadStatistics
    }

    public void process(Request r) throws Exception {
        AnnotationRequest annRQ = new AnnotationRequest(r);

        boolean embed = (r.getParameter("embed") != null);

        DataFiles files = r.getDataFiles();
        if (files == null) {
	        return;
        }

        AnnotationParameters parameters = new AnnotationParameters(files, annRQ);

        switch (annRQ.format) {
        case form:
	        formPage(r, parameters);
	        break;
        case tsv:
	        tsvPage(annRQ, r, files,null);
	        break;
        case association:
	    case gaf:
	        gafPage(annRQ, r, files, parameters);
	        break;
		case gpa:
			gpaPage(annRQ, r, files, parameters);
			break;
        case gene2go:
	        tsvPage(annRQ, r, files, DownloadFormat.gene2go);
	        break;
        case proteinList:
	        tsvPage(annRQ, r, files, DownloadFormat.proteinList);
	        break;
        case fasta:
	        tsvPage(annRQ, r, files, DownloadFormat.fastaFormat);
	        break;
        case sample:
	        samplePage(annRQ, r, files, parameters, embed);
	        break;
        case stats:
	        statsPage(annRQ, r, files, parameters, embed);
	        break;
        case statsFull:
	        statsFullPage(annRQ, r, files, parameters);
	        break;
		case compare:
			comparePage(annRQ, r, files);
			break;
		case compareJSON:
			compareJSON(annRQ, r, files);
			break;
		case downloadStatistics:
			downloadStatistics(annRQ, r, files, parameters);
			break;
        }
    }

    private void formPage(Request r, AnnotationParameters parameters) throws Exception {
        r.write(r.outputHTML(true, "page/GAnnotation.xhtml").render(parameters));
    }

    public static NumberFormat nf = new DecimalFormat("#,##0.00");

    public class TermComparison implements Comparable<TermComparison> {
        public Term term;

        private double s;
        public String stext() {
	        return nf.format(s * 100);
        }

	    private double pr;
        public String prtext() {
	        return nf.format(pr);
        }

        public int data;
        public int overlap;
        public int background;
        Filter dataFilter;
        Filter backgroundFilter;

        public String filterParameter(Filter filter) {
            return URLUtils.encodeURL(new BooleanFilter(BooleanFilter.BooleanCombination.like, filter, new FieldFilter(FieldFilter.FieldName.term, term.id())).toString());
        }

        public String backgroundParameter() {
            return filterParameter(backgroundFilter);
        }

        public String dataParameter() {
            return filterParameter(dataFilter);
        }

        public TermComparison(Filter dataFilter, Filter backgroundFilter, Term compared, double bias, int data, int overlap, int background) {
            this.backgroundFilter = backgroundFilter;
            this.dataFilter = dataFilter;

            this.term = compared;
            this.data=data;
            this.overlap = overlap;
            this.background = background;
            pr = overlap*1.0/background/bias;
            s = overlap*1.0/(data+background-overlap);
        }

        public int compareTo(TermComparison termComparison) {
            return (termComparison.overlap == 0 && overlap == 0) ? (background - termComparison.background) : Double.compare(termComparison.s, s);
        }
    }

    public class Comparison implements JSONSerialise {
        public TermComparison[] over;
        //public TermComparison[] under;

        public double totalRatio;
	    public double threshold;
        public int data;
        public int background;
        public int allCount, overCount; //, underCount;
        private Statistics dataStatistics;
        private Statistics backgroundStatistics;

        public String filterParameter(Filter filter) {
            return URLUtils.encodeURL(filter.toString());
        }

        public String backgroundParameter() {
            return filterParameter(backgroundStatistics.query.filter);
        }

        public String dataParameter() {
            return filterParameter(dataStatistics.query.filter);
        }

        Comparison(DataFiles df,Statistics dataStatistics, Statistics backgroundStatistics, int limit, double threshold) {
            this.dataStatistics = dataStatistics;
            this.backgroundStatistics = backgroundStatistics;
            this.data = dataStatistics.totalProteins;
            this.background = backgroundStatistics.totalProteins;
	        this.threshold = threshold;

            totalRatio = this.data * 1.0 / this.background;
            List<TermComparison> all = new ArrayList<TermComparison>();
            for (int i = 0; i < backgroundStatistics.termProteinCounts.length; i++) {
                int background = backgroundStatistics.termProteinCounts[i];
                int overlap = dataStatistics.termProteinCounts[i];
                if (background > 0) {
	                TermComparison termComparison = new TermComparison(dataStatistics.query.filter, backgroundStatistics.query.filter, df.ontology.terms[i], totalRatio, dataStatistics.totalProteins, overlap, background);
	                all.add(termComparison);
                }
            }

            Collections.sort(all);
            overCount = Math.min(all.size(), limit);
            over = all.subList(0, overCount).toArray(new TermComparison[overCount]);
            //underCount = Math.min(all.size() - overCount, limit);
            //under = all.subList(all.size() - underCount, all.size()).toArray(new TermComparison[underCount]);
            allCount = all.size();
        }

	    public class CoOccurringTerm {
			public String id;
		    public String name;
		    public String aspect;
		    public String s;

		    public CoOccurringTerm(Term term, double s) {
			    this.id = term.id();
			    this.name = term.name();
			    this.aspect = term.aspect.text;
			    this.s = nf.format(s);
		    }
	    }

	    public Object serialise() {
		    List<CoOccurringTerm> cotList = new ArrayList<CoOccurringTerm>();

		    for (TermComparison tc : over) {
			    double s = tc.s * 100.0;
			    if (s < threshold) {
				    break;
			    }
			    else {
				    cotList.add(new CoOccurringTerm(tc.term, s));
			    }
		    }

		    Map<String, Object> map = new HashMap<String, Object>();
		    map.put("co_occurring_terms", cotList);
	        return map;
	    }
    }

	private void comparePage(AnnotationRequest annRQ, Request r, DataFiles dataFiles) throws Exception {
	    AnnotationRequest termRequest = new GAnnotationServlet.AnnotationRequest(GAnnotationServlet.Column.defaultColumns);
	    termRequest.terms(annRQ.termIDs);
	    termRequest.slimTypes = annRQ.slimTypes;

	    Filter termFilter = termRequest.getFilter();
	    annRQ.termIDs = empty;

	    Filter backgroundFilter = annRQ.getFilter();
	    Filter dataFilter = new BooleanFilter(BooleanFilter.BooleanCombination.like, backgroundFilter, termFilter);

	    Statistics data = dataFiles.cache.make(new AnnotationQuery(dataFilter), annRQ.useCache);
	    Statistics background = dataFiles.cache.make(new AnnotationQuery(backgroundFilter), annRQ.useCache);

		r.write(r.outputHTML(false, "page/GCompareStats.xhtml").render(new Comparison(dataFiles, data, background, 100, 0.0)));
	}

	private void compareJSON(AnnotationRequest annRQ, Request r, DataFiles dataFiles) throws Exception {
	    AnnotationRequest termRequest = new AnnotationRequest(GAnnotationServlet.Column.defaultColumns);
	    termRequest.terms(annRQ.termIDs);
	    termRequest.slimTypes = annRQ.slimTypes;
		termRequest.evidences = annRQ.evidences;
		termRequest.advanced = annRQ.advanced;

	    Filter termFilter = termRequest.getFilter();
	    annRQ.termIDs = empty;
	    Filter backgroundFilter = annRQ.getFilter();
	    Filter dataFilter = new BooleanFilter(BooleanFilter.BooleanCombination.like, backgroundFilter, termFilter);

	    Statistics data = dataFiles.cache.make(new AnnotationQuery(dataFilter), annRQ.useCache);
	    Statistics background = dataFiles.cache.make(new AnnotationQuery(backgroundFilter), annRQ.useCache);

		r.write(r.outputJSON().render(new Comparison(dataFiles, data, background, annRQ.limit, Double.parseDouble(StringUtils.nvl(r.getParameter("threshold"), "0.0")))));
	}

    private void statsPage(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters, boolean embed) throws Exception {
        Summary summary = new Summary(dataFiles.cache.make(annRQ.getQuery(), annRQ.useCache), dataFiles, r.connection, embed ? r.configuration.statsControl.defaultLimit : 0);
        Render render = r.outputHTML(!embed, embed ? "page/GAnnotationStatsEmbed.xhtml" : "page/GAnnotationStats.xhtml");
        r.write(render.render(parameters, summary));
    }

	private void statsFullPage(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters) throws Exception {
		Summary summary = new Summary(dataFiles.cache.make(annRQ.getQuery(), annRQ.useCache), dataFiles, r.connection, 0);

		String fileName = "annotation_statistics";
		OutputStream os = ("true".equals(r.getParameter("gz"))) ? new GZIPOutputStream(r.outputData(contentTypeGZIP, fileName + ".gz")) : r.outputData(contentTypeTSV, fileName);

		Writer wr = new OutputStreamWriter(os, "ASCII");
		summary.write(wr, parameters.parameters(true));
		wr.close();
	}
	
	private void downloadStatistics(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters) throws Exception {	    

		Summary summary = new Summary(dataFiles.cache.make(annRQ.getQuery(), annRQ.useCache), dataFiles, r.connection, 0);
		Map<String, String> options = r.getParameterMap();	
				
		List<String> checked = new ArrayList<String>();		
		
		// Get checked values
		for (String key : options.keySet()) {			
			if (Boolean.parseBoolean(options.get(key)) == true) {
				// If the 'protein' option is checked and there are protein statistics or option is different from 'protein', add the option  
				if((key.equals(EnumCategory.DOWNLOAD_PROTEIN.getValue()) && summary.hasProteinStatistics) || !key.equals(EnumCategory.DOWNLOAD_PROTEIN.getValue())){					
						checked.add(key);				
				}				
			}
		}
		
		// Generate Excel file depending on the checked values
		ExcelExporter excelExporter = new ExcelExporter();
		
		ByteArrayOutputStream excelStatisticsOutputStream = excelExporter.generateFile(summary, checked);
		
		String fileName = "annotation_statistics.xlsx";
		OutputStream outputStream = r.outputData(contentTypeXLSX, fileName);		
		
		outputStream.write(excelStatisticsOutputStream.toByteArray());
		
		outputStream.flush();
		outputStream.close();
	}

    private void samplePage(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters,boolean embed) throws Exception {
        AnnotationQuery query = annRQ.getQuery();
        Scanner scanner = new Scanner(dataFiles, r.connection,annRQ.select == Select.proteins);
        Sampler sampler = new Sampler(r.configuration, dataFiles, r.connection,annRQ);
        scanner.scan(query, new Slimmer(dataFiles.ontology, query.slimIDs, sampler, query.slimTypes));
        Render render = r.outputHTML(!embed, embed ? "page/GAnnotationSampleEmbed.xhtml" : "page/GAnnotationSample.xhtml");
        r.write(render.render(parameters, new SampleAnnotation(sampler)));
    }

    private void tsvPage(AnnotationRequest annRQ, Request r, DataFiles dataFiles,DownloadFormat format) throws Exception {
        if (format == null) {
	        format = new DownloadFormat(annRQ.select == Select.proteins, "\t", "\n", "", "", false, annRQ.columns.toArray(new Column[0]), "associations.tsv", contentTypeTSV);
        }

        AnnotationQuery query = annRQ.getQuery();
        Scanner scanner = new Scanner(dataFiles, r.connection, format.proteinUnique);

        TableWriter output = new TableWriter(r.configuration, dataFiles,r.connection, annRQ,format);
        OutputStream os = ("true".equals(r.getParameter("gz"))) ? new GZIPOutputStream(r.outputData(contentTypeGZIP, format.fileName + ".gz")) : r.outputData(format.mimeType, format.fileName);
        Writer wr = new OutputStreamWriter(os, "ASCII");
        output.open(wr);
        scanner.scan(query, new Slimmer(dataFiles.ontology, query.slimIDs, output, query.slimTypes));
        wr.close();
    }

	private void gafPage(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters) throws Exception {
	    AnnotationQuery query = annRQ.getQuery();
	    Scanner scanner = new Scanner(dataFiles, r.connection, false);

	    GAFWriter output = new GAFWriter(r.configuration, dataFiles, r.connection, annRQ, parameters);
	    Slimmer slimmer = new Slimmer(dataFiles.ontology, query.slimIDs, output, query.slimTypes);
		String fileName = "gene_association.goa";

		OutputStream os = ("true".equals(r.getParameter("gz"))) ? new GZIPOutputStream(r.outputData(contentTypeGZIP, fileName + ".gz")) : r.outputData(contentTypeTSV, fileName);
	    Writer wr = new OutputStreamWriter(os, "ASCII");
	    output.open(wr);
	    scanner.scan(query, slimmer);
	    wr.close();
	}

	private void gpaPage(AnnotationRequest annRQ, Request r, DataFiles dataFiles, AnnotationParameters parameters) throws Exception {
	    AnnotationQuery query = annRQ.getQuery();
	    Scanner scanner = new Scanner(dataFiles, r.connection, false);

	    GPAWriter output = new GPAWriter(r.configuration, dataFiles, r.connection, annRQ, parameters);
	    Slimmer slimmer = new Slimmer(dataFiles.ontology, query.slimIDs, output, query.slimTypes);
		String fileName = "gp_association.goa";

		OutputStream os = ("true".equals(r.getParameter("gz"))) ? new GZIPOutputStream(r.outputData(contentTypeGZIP, fileName + ".gz")) : r.outputData(contentTypeTSV, fileName);
	    Writer wr = new OutputStreamWriter(os, "ASCII");
	    output.open(wr);
	    scanner.scan(query, slimmer);
	    wr.close();
	}

    public interface Key<K extends Key<K>> extends Comparable<K> {
        String name();
    }

    public static class AspectKey implements Key<AspectKey> {
        Term.Ontology ontology;
        public int compareTo(AspectKey aspectKey) {
            return ontology.compareTo(aspectKey.ontology);
        }

        public String name() {
            return ontology.text;
        }

        public AspectKey(Term.Ontology ontology) {
            this.ontology = ontology;
        }
    }

    public static class Group {
        public List<Annotation> annotation = new ArrayList<Annotation>();
        public Map<Key<?>,Group> groups = new AutoMap<Key<?>,Group>(new TreeMap<Key<?>, Group>(), Creator.reflective(Group.class));
    }

    static class DataTranslate {
        public DataFiles dataFiles;
        private List<Closeable> connection;

        int[] idMapIndex;
        IntegerTableReader.Cursor idMapCursor;

        TextTableReader.Cursor proteinInfoCursor;
	    TextTableReader.Cursor proteinMetadataCursor;
        IntegerTableReader.Cursor proteinTaxonomyCursor;
        TextTableReader.Cursor taxonomyCursor;
        private TextTableReader.Cursor sequenceCursor;
        private TextTableReader.Cursor idCursor;
        private IntegerTableReader.Cursor pubmedRefCursor;
        private TextTableReader.Cursor pubmedTitleCursor;
        private IntegerTableReader.Cursor proteinWithCursor;


        DataTranslate(DataFiles dataFiles, List<Closeable> connection, String[] db) throws IOException {
            this.dataFiles = dataFiles;
            this.connection = connection;
            idMapCursor = dataFiles.proteinIDMap.open(connection);

            List<Integer> dbs = new ArrayList<Integer>();
	        if (db != null) {
				for (String database : db) {
					int code = dataFiles.proteinDatabaseTable.search(database);
					if (code >= 0) {
						dbs.add(code);
					}
				}
	        }
            idMapIndex = CollectionUtils.toIntArray(dbs);

            proteinInfoCursor = dataFiles.proteinInfo.open(connection);
	        proteinMetadataCursor = dataFiles.proteinMetadata.open(connection);
            proteinTaxonomyCursor = dataFiles.proteinTaxonomy.use();
            idCursor = dataFiles.proteinIDs.use();
        }

        public void configureSequence() throws IOException {
            sequenceCursor = dataFiles.proteinSequences.open(connection);
        }

        public Annotation makeAnnotation(Configuration config) {
            Annotation annotation = new Annotation(config, dataFiles);
            annotation.configureProteinInfo(proteinTaxonomyCursor, idMapCursor, idMapIndex, proteinInfoCursor, idCursor, proteinMetadataCursor);
            if (taxonomyCursor != null) {
	            annotation.configureTaxonomy(taxonomyCursor);
            }
            if (sequenceCursor != null) {
	            annotation.configureSequences(sequenceCursor);
            }
            if (pubmedRefCursor != null && pubmedTitleCursor != null) {
	            annotation.configurePubmed(pubmedRefCursor, pubmedTitleCursor);
            }
            if (proteinWithCursor != null) {
	            annotation.configureProteinRefInfo(proteinWithCursor);
            }
            return annotation;
        }

        public void configureTaxonomy() throws IOException {
            taxonomyCursor = dataFiles.taxonomy.open(connection);
        }

        public void configureRefInfo() throws IOException {
            pubmedRefCursor = dataFiles.pubmedRef.open(connection);
            pubmedTitleCursor = dataFiles.pubmedInfo.open(connection);
            proteinWithCursor = dataFiles.proteinWith.open(connection);
        }
    }

    static class Sampler implements DataAction {
        Group root = new Group();
        boolean before, after;

        int count, start, rowCount;

        boolean aspectSorter;

        boolean proteinOnly;

        private AdvancedRequest.ParseException parseException;
	    private Configuration config;

        DataTranslate translate;

        Sampler(Configuration config, DataFiles df,List<Closeable> connection, AnnotationRequest annRQ) throws Exception {
	        this.config = config;
            translate = new DataTranslate(df, connection, annRQ.db);

            start = annRQ.start;
            count = annRQ.count;
            aspectSorter = annRQ.aspectSorter;
            proteinOnly = (annRQ.select == Select.proteins);
            parseException = annRQ.advanced.broke;
            translate.configureSequence();

            translate.configureTaxonomy();
            translate.configureRefInfo();
        }

        public boolean act(AnnotationRow row) throws IOException {
            if (rowCount >= start + count) {
                after = true;
                return false;
            }

            Annotation annotation=translate.makeAnnotation(config);
            if (!(proteinOnly ? annotation.loadProtein(row) : annotation.load(row))) {
	            return true;
            }

            rowCount++;
            if (rowCount <= start) {
                before = true;
                return true;
            }

            Group group = root;
            if (aspectSorter) {
                group = root.groups.get(new AspectKey(annotation.term.aspect));
            }

            group.annotation.add(annotation);
            return true;
        }
    }

    static class TableWriter implements DataAction {
        Writer wr;
        private int limit;

        String empty = "-";
        String separator = "\t";
        String endline = "\n";
        String startline = "";
        boolean fasta;

        DataTranslate translate;

        Annotation annotation;
        private Column[] columns;

        TableWriter(Configuration config, DataFiles df, List<Closeable> connection, AnnotationRequest annRQ, DownloadFormat format) throws Exception {
            translate = new DataTranslate(df,connection, annRQ.db);

            this.limit = annRQ.limit;

            this.columns = format.columns;
            this.separator = format.separator;
            this.endline = format.endOfLine;
            this.fasta = format.fasta;
            this.startline = format.start;
            if (Arrays.asList(columns).indexOf(Column.proteinTaxonName) >= 0) {
	            translate.configureTaxonomy();
            }
            if (fasta) {
	            translate.configureSequence();
            }
            annotation = translate.makeAnnotation(config);
        }

        public void open(Writer wr) throws IOException {
            this.wr = wr;
            if (!fasta) {
                for (int i = 0; i < columns.length; i++) {
                    if (i != 0) {
	                    this.wr.write(separator);
                    }
                    this.wr.write(columns[i].title);
                }
                this.wr.write(endline);
            }
        }

        public String field(Column name) throws IOException {
            switch (name) {
                case aspect:
	                return annotation.term.aspect.text;
                case rowId:
	                return annotation.rowId;
                case date:
	                return annotation.date;
                case sequence:
	                return annotation.sequence;
                case evidence:
	                return annotation.evidence;
                case from:
	                return annotation.source;
                case goID:
	                return annotation.term.id();
                case originalTermID:
	                return annotation.originalTerm.id();
                case goName:
	                return annotation.term.name();
                case originalGOName:
	                return annotation.originalTerm.name();
                case proteinDB:
	                return annotation.db;
                case proteinID:
	                return annotation.proteinAc;
                case proteinName:
	                return annotation.name;
                case proteinSymbol:
	                return annotation.symbol;
                case proteinSynonym:
	                return annotation.synonym;
                case proteinTaxon:
	                return annotation.taxId;
                case proteinTaxonName:
	                return annotation.taxName;
                case proteinType:
	                return annotation.type;
                case qualifier:
	                return annotation.qualifier;
                case ref:
	                return annotation.ref.db + ":" + annotation.ref.id;
                case pubmed:
	                return annotation.ref.isPubmed() ? annotation.ref.id : "";
                case splice:
	                return annotation.splice;
                case with:
	                return annotation.with.withString;
                default:
	                return "";
            }
        }

        public boolean act(AnnotationRow row) throws IOException {
            if (limit == 0) {
	            return false;
            }
	        limit--;

            if (!annotation.load(row)) {
	            return true;
            }

            wr.write(startline);

            for (int i = 0; i < columns.length; i++) {
                if (i != 0) {
	                wr.write(separator);
                }
                String field = field(columns[i]);
                if (field == null || field.length() == 0) {
	                field = empty;
                }
                wr.write(field);
            }

            wr.write(endline);

            if (fasta) {
                int i = 0;
                int len = annotation.sequence.length();
                while (i < len) {
                    int size = Math.min(len-i, 80);
                    wr.write(annotation.sequence.substring(i, i + size));
                    i += size;
                }
                wr.write(endline);
            }

            return true;
        }
    }

/*
    public static void main(String[] args) throws Exception {
        AdvancedRequest request = new AdvancedRequest("evidence=IPI,IDA | source=SWIS,HGNC & ancestor=GO:0000001 & ref=PUBMED:");
        if (request.broke != null) {
            System.out.println(request.broke.error);
            System.out.println(request.broke.where.index);
        }
        System.out.println(request.toString());
    }
*/
}
