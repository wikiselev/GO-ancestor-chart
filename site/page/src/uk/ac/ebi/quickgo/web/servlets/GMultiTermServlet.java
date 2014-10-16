package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.data.Term.Info;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.quickgo.web.graphics.*;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.sql.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.GAnnotationServlet;

public class GMultiTermServlet implements Dispatchable {
	private Dispatcher dispatcher;

	public GMultiTermServlet(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public static class OntologyGroup implements Comparable<OntologyGroup> {
		public Term.Ontology ontology;

		public OntologyGroup(Term.Ontology ontology) {
			this.ontology = ontology;
		}

		public List<Term> terms = new ArrayList<Term>();

		public int compareTo(OntologyGroup ontologyGroup) {
			return ontologyGroup.terms.size() - terms.size();
		}
	}

	public class TermPage {
		public String url;
		public int count;
		public Map<Term.Ontology, OntologyGroup> groups = new EnumMap<Term.Ontology,OntologyGroup>(Term.Ontology.class);
		public List<Terms> sets;
		public String idList;
		public String compressed;
	}

	enum Format {
		standard, chart, image, json
	}

	public void process(Request r) throws Exception {
		DataFiles files = r.getDataFiles();
		if (files == null) {
			return;
		}

		String update = r.getParameter("update");

		Format format = CollectionUtils.enumFind(r.getParameter("format"), Format.standard);

		Terms t = new Terms(files.ontology);

		t.addAll(r.getParameterValues("term"));
		t.addAll(r.getParameterValues("id"));
		t.addAll(r.getParameterValues("idlist"));
		if (format != Format.chart) {
			t.addCompressed(r.getParameter("a"));
		}
		t.addAll(r.getParameterValues("goid"));

		if ("Remove".equals(update)) {
			t.removeAll(r.getParameterValues("select"));
		}
		else if ("Preserve".equals(update)) {
			t.clear();
			t.addAll(r.getParameterValues("select"));
		}
		else {
			t.addAll(r.getParameterValues("select"));
		}

		switch (format) {
			case standard:
				termPage(t, files, r);
				break;
			case chart:
				imagePage(t, r);
				break;
			case image:
				image(t, r);
				break;
			case json:
				jsonPage(t, files, r);
				break;
		}
	}

	private void imagePage(Terms t, Request r) throws Exception {
		r.write(r.outputHTML(r.getParameter("embed") == null, "page/GTermChart.xhtml").render(getImage(r, t)));
	}

	private void image(Terms t, Request r) throws Exception {
		dispatcher.image.process(r, getImage(r, t).render());
	}

	private HierarchyImage getImage(Request r, Terms t) throws SQLException {
//		GraphStyle style = new GraphStyle(CollectionUtils.keyFilter(r.getParameterMap(), CollectionUtils.removePrefix("chart_")));
		GraphStyle style = new GraphStyle(r);
		style.initialise(CollectionUtils.keyFilter(r.getParameterMap(), CollectionUtils.removePrefix("chart_")));
		style.saveSettings(r);

		int limit;
		try {
			limit = Integer.parseInt(r.getParameter("limit"));
		}
		catch (Exception e) {
			limit = r.configuration.chartControl.defaultImageLimit;
		}

        limit = Math.min(limit, r.configuration.chartControl.maximumImageLimit);

		HierarchyGraph graph = new HierarchyGraph(r.configuration.chartControl.ancestorLimit, limit*1000000, style);
		for (Term term : t.getTerms()) {
			String colour = t.getTermInfo(term);
			TermGraphNode graphNode = graph.add(term);
			if (graphNode != null && style.fill) {
				graphNode.fill = new Color(colour.length() == 0 ? 0xffffcc : ColourUtils.intDecodeColour("#" + colour));
			}
		}

		return graph.layout(r.configuration.imageArchive);
	}

	private void termPage(Terms t, DataFiles df, Request r) throws Exception {
		TermPage termPage = new TermPage();

		for (Term.Ontology ontology : Term.Ontology.values()) {
			if (ontology==Term.Ontology.R) continue;
			termPage.groups.put(ontology,new OntologyGroup(ontology));
		}

		for (Term term : t.getTerms()) {
			if (term.aspect==Term.Ontology.R) continue;
			termPage.groups.get(term.aspect).terms.add(term);
		}

		termPage.url = "GMultiTerm" + r.request.getQueryString();

		termPage.sets = new ArrayList<Terms>(df.ontology.slims.values());
		termPage.count = t.count();
		termPage.idList = t.getIdList();
		termPage.compressed = t.getCompressed();

        GAnnotationServlet.AnnotationRequest annotation = new GAnnotationServlet.AnnotationRequest(GAnnotationServlet.Column.defaultSlimColumns);
        annotation.terms(t.getIDs());
        GAnnotationServlet.AnnotationParameters parameters = new GAnnotationServlet.AnnotationParameters(df, annotation);

		r.write(r.outputHTML(true, "page/GMultiTerm.xhtml").render(termPage,parameters));
	}

	public class TermInfo {
		public Term.Info info;
		public int count;

		public TermInfo(Info info, int count) {
			this.info = info;
			this.count = count;
		}
	}

	public class JSONData {
		public List<TermInfo> terms = new ArrayList<TermInfo>();
	}

	private void jsonPage(Terms t, DataFiles df, Request r) throws Exception {
		JSONData data = new JSONData();

		for (Term term : t.getTerms()) {
			int count = df.cache.global.ancestorCounts[term.index()];
			data.terms.add(new TermInfo(term.info, count));
		}

		r.write(r.outputJSON().render(data));
	}

	public static void main(String[] args) {
		System.out.println(Integer.toHexString(ColourUtils.combine(0x80ffffff, ColourUtils.intDecodeColour("#FF00FF"))));
	}
}
