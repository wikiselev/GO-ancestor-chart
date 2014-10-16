package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.quickgo.web.Request;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.data.AnnotationExtensionRelationSet;

import java.util.HashMap;
import java.util.Map;

public class GValidationServlet implements Dispatchable {
	public static class ValidationStatus {
		public boolean valid;
		public String message;

		public ValidationStatus(boolean valid, String message) {
			this.valid = valid;
			this.message = message;
		}

		public ValidationStatus(boolean valid) {
			this.valid = valid;
			this.message = null;
		}
	}

	public void process(Request r) throws Exception {
	    DataFiles files = r.getDataFiles();
	    if (files == null) {
		    return;
	    }

	    String service = r.getParameter("service");
		if ("ann_ext".equals(service)) {
			processAnnExtRequest(files, r);
		}
		else if ("taxon".equals(service)) {
			processTaxonRequest(files, r);
		}
	}

	enum Format { xml, xmlcompact, json, jsonMinimal, graph }

	public void processAnnExtRequest(DataFiles df, Request r) throws Exception {
		String action = r.getParameter("action");
		if ("validate".equals(action)) {
			ValidationStatus status = null;
			try {
				df.annotationExtensionRelations.validate(r.getParameter("go_id"), r.getParameter("candidate"));
				status = new ValidationStatus(true);
			}
			catch (AnnotationExtensionRelationSet.AnnExtRelException e) {
				status = new ValidationStatus(false, e.getMessage());
			}
			finally {
				switch (CollectionUtils.enumFind(r.getParameter("format"), Format.xml)) {
					case xml:
						r.write(r.outputXML("page/ValidationStatus.xhtml").render(status));
						break;

					case json:
						r.write(r.outputJSON().render(status));
						break;
				}
			}
		}
		else if ("getRelations".equals(action)) {
			String domain = StringUtils.nvl(r.getParameter("domain"), "");
			Format format = CollectionUtils.enumFind(r.getParameter("format"), Format.xml);
			if ("".equals(domain)) {
				switch (format) {
					case json:
						r.write(r.outputJSON().render(df.annotationExtensionRelations));
						break;

					case graph:
						r.write(r.outputJSON().render(df.annotationExtensionRelations.toGraph()));
						break;
				}
			}
			else {
				switch (format) {
					case json:
						r.write(r.outputJSON().render(df.annotationExtensionRelations.forDomain(domain)));
						break;
				}
			}
		}
	}

	public void processTaxonRequest(DataFiles df, Request r) throws Exception {
		String action = r.getParameter("action");
		if ("getBlacklist".equals(action)) {
			int taxon = StringUtils.parseInt(r.getParameter("taxon"), -1);
			if (taxon > 0) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("blacklist", df.blacklist.forTaxon(taxon));
				r.write(r.outputJSON().render(map));
			}
		}
		else if ("getConstraints".equals(action)) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("constraints", df.ontology.taxonConstraints);
			r.write(r.outputJSON().render(map));
		}
	}
}
