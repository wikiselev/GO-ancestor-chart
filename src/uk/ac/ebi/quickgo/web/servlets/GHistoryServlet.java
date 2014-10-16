package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.quickgo.web.Request;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.data.AuditRecord;
import uk.ac.ebi.quickgo.web.data.TermList;
import uk.ac.ebi.quickgo.web.data.TermOntology;
import uk.ac.ebi.quickgo.web.data.TermOntologyHistory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GHistoryServlet implements Dispatchable {
	public static class TimePeriod {
		private final static Pattern pattern = Pattern.compile("^([0-9]+)([dD]|[wW]|[mM]|[yY])$");
		private final static Matcher matcher = pattern.matcher("");

		private Calendar start;
		private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		private void initialise(String baseDate, String unit, int multiple) {
			start = Calendar.getInstance();
			if (baseDate != null) {
				try {
					start.setTime(df.parse(baseDate));
				}
				catch (Exception e) {
					// just ignore the base date
				}
			}
			if (unit.equalsIgnoreCase("d")) {
				start.add(Calendar.DATE, -multiple);
			}
			else if (unit.equalsIgnoreCase("w")) {
				start.add(Calendar.DATE, -(7 * multiple));
			}
			if (unit.equalsIgnoreCase("m")) {
				start.add(Calendar.MONTH, -multiple);
			}
			if (unit.equalsIgnoreCase("y")) {
				start.add(Calendar.YEAR, -multiple);
			}
		}

		public TimePeriod(String baseDate, String period) throws Exception {
			matcher.reset(period);
			if (matcher.matches()) {
				initialise(baseDate, matcher.group(2), Integer.parseInt(matcher.group(1)));
			}
			else {
				throw new Exception("Unrecognised period: " + period);
			}
		}

		public String startDate() {
			return df.format(start.getTime());
		}
	}

	public static class TermHistoryPage {
		public TermOntologyHistory history = new TermOntologyHistory();

		private TermOntology ontology;
		private TermList terms;
		private String what;
		private String from;
		private String to;
		private int limit;

		public TermHistoryPage(TermOntology ontology, Request r) throws Exception {
			this.ontology = ontology;
			this.terms = new TermList(ontology);
			terms.addAll(r.getParameterValues("id"));

			this.what = r.getParameter("what");
			if ("".equals(this.what)) {
				this.what = null;
			}

			this.limit = Math.min(StringUtils.parseInt(r.getParameter("limit"), 500), 10000);

			this.from = r.getParameter("from");
			if ("".equals(this.from)) {
				this.from = null;
			}
			this.to = r.getParameter("to");
			if ("".equals(this.to)) {
				this.to = null;
			}
			TimePeriod period = new TimePeriod(this.to, StringUtils.nvl(r.getParameter("period"), "6m"));

			if (this.from == null) {
				this.from = period.startDate();					
			}
			else if (this.to != null && this.from.compareTo(this.to) > 0) {
				this.to = null;
			}
		}

		public void getHistory() {
			int count = 0;
			for (AuditRecord ar : ontology.history.auditRecords) {
				if (count >= limit) {
					break;
				}
				if ((terms.count() == 0 || terms.contains(ar.goId)) && (from == null || from.compareTo(ar.timestamp) <= 0) && (to == null || to.compareTo(ar.timestamp) >= 0) && (what == null || ar.category.equals(what))) {
					history.add(ar);
					count++;
				}
			}
		}

		public int count() {
			return history.count();
		}

		public String description() {
			return "Displaying " + count() + " audit records for " + ((terms.count() == 0) ? "all" : Integer.toString(terms.count())) + " terms for the " + ((to == null) ? "period since " + from : (from.equals(to) ? "date " + from : "period between " + from + " and " + to));
		}

		public boolean multiCategory() {
			return (what == null);
		}
	}

	public void process(Request r) throws Exception {
	    DataFiles files = r.getDataFiles();
	    if (files != null) {
		    historyPage(files, r);
	    }
	}

	private void historyPage(DataFiles df, Request r) throws Exception {
		TermHistoryPage thp = new TermHistoryPage(df.ontology, r);
		thp.getHistory();
		r.write(r.outputHTML(true, "page/GTermHistory.xhtml").render(thp));
	}
}
