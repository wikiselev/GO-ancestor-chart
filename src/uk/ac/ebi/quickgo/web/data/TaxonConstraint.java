package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.render.JSONSerialise;

import java.util.*;

public class TaxonConstraint implements JSONSerialise {
	public enum Relationship {
		ONLY_IN_TAXON("only_in_taxon"),
		NEVER_IN_TAXON("never_in_taxon");

		public String text;

		Relationship(String text) {
			this.text = text;
		}
	}

	public enum TaxIdType {
		TAXON("NCBITaxon"),
		TAXON_UNION("NCBITaxon_Union");

		public String text;

		TaxIdType(String text) {
			this.text = text;
		}
	}

	public static class PMID {
		public String pmid;

		public PMID(String pmid) {
			this.pmid = pmid;
		}

		public String url() {
			return "http://europepmc.org/abstract/MED/" + pmid.substring(5);
		}
	}

	public String ruleId;
	public String goId;
	public String name;
	Relationship relationship;
	TaxIdType taxIdType;
	public String taxId;
	public String taxonName;
	public List<PMID> sources = new ArrayList<PMID>();

	public String relationship() {
		return relationship.text;
	}

	public String taxIdType() {
		return taxIdType.text;
	}

	public TaxonConstraint(String ruleId, String goId, String name, String relationship, String taxIdType, String taxId, String taxonName, String sources) {
		this.ruleId = ruleId;
		this.goId = goId;
		this.name = name;
		this.relationship = "never_in_taxon".equals(relationship) ? Relationship.NEVER_IN_TAXON : Relationship.ONLY_IN_TAXON;
		this.taxIdType = "NCBITaxon_Union".equals(taxIdType) ? TaxIdType.TAXON_UNION : TaxIdType.TAXON;
		this.taxId = taxId;
		this.taxonName = taxonName;
		if (sources != null) {
			for (String pmid : sources.split(",")) {
				this.sources.add(new PMID(pmid));
			}
		}
	}

	public Object serialise() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("ruleId", ruleId);
		map.put("goId", goId);
		map.put("constraint", relationship.text);
		if (taxIdType == TaxIdType.TAXON_UNION) {
			map.put("taxa", taxId.split("[^0-9]+"));
		}
		else {
			map.put("taxa", new String[]{ taxId });
		}
		//map.put("sources", sources);

		return map;
	}

	@Override
	public String toString() {
		return "TaxonConstraint{" +
				"ruleId='" + ruleId + '\'' +
				", goId='" + goId + '\'' +
				", name='" + name + '\'' +
				", relationship=" + relationship +
				", taxIdType=" + taxIdType +
				", taxId='" + taxId + '\'' +
				", taxonName='" + taxonName + '\'' +
				", sources='" + sources + '\'' +
				'}';
	}
}
