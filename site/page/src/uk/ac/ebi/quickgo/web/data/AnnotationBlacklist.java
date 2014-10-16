package uk.ac.ebi.quickgo.web.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnnotationBlacklist {
	HashMap<String, List<AnnotationBlacklistEntry>> subsets = new HashMap<String, List<AnnotationBlacklistEntry>>();

	public void add(String category, String proteinAc, String taxonId, String goId, String reason, String methodId) {
		List<AnnotationBlacklistEntry> subset = subsets.get(category);
		if (subset == null) {
			subset = new ArrayList<AnnotationBlacklistEntry>();
			subsets.put(category, subset);
		}
		
		subset.add(new AnnotationBlacklistEntry(proteinAc, taxonId, goId, reason, methodId));
	}

	public List<AnnotationBlacklistEntry> blacklistIEAReview() {
		return subsets.get("IEA Review");
	}

	public List<AnnotationBlacklistEntry> blacklistUniProtCaution() {
		return subsets.get("UniProt COMMENT_CAUTION");
	}

	public List<AnnotationBlacklistEntry> blacklistNotQualified() {
		return subsets.get("NOT-qualified manual");
	}

	public static class BlacklistEntryMinimal {
		public String proteinAc;
		public String goId;

		public BlacklistEntryMinimal(String proteinAc, String goId) {
			this.proteinAc = proteinAc;
			this.goId = goId;
		}
	}

	public List<BlacklistEntryMinimal> forTaxon(int taxonId) {
		List<BlacklistEntryMinimal> ab = new ArrayList<BlacklistEntryMinimal>();

		for (String category : subsets.keySet()) {
			for (AnnotationBlacklistEntry abe : subsets.get(category)) {
				if (abe.taxonId == taxonId) {
					ab.add(new BlacklistEntryMinimal(abe.proteinAc, abe.goId));
				}
			}
		}

		return ab;
	}
}
