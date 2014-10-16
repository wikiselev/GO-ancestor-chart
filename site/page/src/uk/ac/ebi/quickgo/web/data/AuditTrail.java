package uk.ac.ebi.quickgo.web.data;

import java.util.ArrayList;
import java.util.List;

public class AuditTrail {
	public ArrayList<AuditRecord> auditRecords = new ArrayList<AuditRecord>();

	public void add(AuditRecord ar) {
		auditRecords.add(ar);
	}

	public int count() {
		return auditRecords.size();
	}

	List<AuditRecord> getFilteredHistory(String[] categories) {
		List<AuditRecord> filteredHistory = new ArrayList<AuditRecord>();

		for (AuditRecord thr : auditRecords) {
			if (thr.isA(categories)) {
				filteredHistory.add(thr);
			}
		}
		return filteredHistory;
	}

	List<AuditRecord> getFilteredHistory(String category) {
		List<AuditRecord> filteredHistory = new ArrayList<AuditRecord>();

		for (AuditRecord thr : auditRecords) {
			if (thr.isA(category)) {
				filteredHistory.add(thr);
			}
		}
		return filteredHistory;
	}
}
