package uk.ac.ebi.quickgo.web.data;

import java.util.List;

public class TermOntologyHistory extends AuditTrail {
	public List<AuditRecord> historyRelations() {
		return getFilteredHistory("RELATION");
	}

	public List<AuditRecord> historyTerms() {
		return getFilteredHistory("TERM");
	}

	public List<AuditRecord> historyDefinitions() {
		String[] categories = { "DEFINITION", "SYNONYM" };
		return getFilteredHistory(categories);
	}

	public List<AuditRecord> historyXRefs() {
		return getFilteredHistory("XREF");
	}

	public List<AuditRecord> historyObsoletions() {
		return getFilteredHistory("OBSOLETION");
	}

	public List<AuditRecord> historyOther() {
		String[] categories = { "SECONDARY", "SLIM" };
		return getFilteredHistory(categories);
	}
}
