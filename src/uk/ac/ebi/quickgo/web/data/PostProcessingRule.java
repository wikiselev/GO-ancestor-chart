package uk.ac.ebi.quickgo.web.data;

public class PostProcessingRule {
	public String ruleId;
	public String ancestorGoId;
	public String ancestorTerm;
	public String relationship;
	public String taxonName;
	public String originalGoId;
	public String originalTerm;
	public String cleanupAction;
	public String affectedTaxGroup;
	public String substitutedGoId;
	public String substitutedTerm;
	public String curatorNotes;

	public PostProcessingRule(String ruleId, String ancestorGoId, String ancestorTerm, String relationship, String taxonName, String originalGoId, String originalTerm, String cleanupAction, String affectedTaxGroup, String substitutedGoId, String substitutedTerm, String curatorNotes) {
		this.ruleId = ruleId;
		this.ancestorGoId = ancestorGoId;
		this.ancestorTerm = ancestorTerm;
		this.relationship = relationship;
		this.taxonName = taxonName;
		this.originalGoId = originalGoId;
		this.originalTerm = originalTerm;
		this.cleanupAction = cleanupAction;
		this.affectedTaxGroup = affectedTaxGroup;
		this.substitutedGoId = substitutedGoId;
		this.substitutedTerm = substitutedTerm;
		this.curatorNotes = curatorNotes;
	}

	public boolean isTransform() {
		return "TRANSFORM".equalsIgnoreCase(cleanupAction);
	}

	public boolean isDelete() {
		return "DELETE".equalsIgnoreCase(cleanupAction);
	}
}
