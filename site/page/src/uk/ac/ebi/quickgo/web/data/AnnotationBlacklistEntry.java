package uk.ac.ebi.quickgo.web.data;

public class AnnotationBlacklistEntry {
	public String proteinAc;
	public int taxonId;
	public String goId;
	public String reason;
	public String methodId;

	public AnnotationBlacklistEntry(String proteinAc, String taxonId, String goId, String reason, String methodId) {
		this.proteinAc = proteinAc;
		this.taxonId = Integer.parseInt(taxonId);
		this.goId = goId;
		this.reason = reason.intern();
		this.methodId = methodId;
	}
}
