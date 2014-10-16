package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.render.JSONSerialise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonConstraintSet extends HashMap<String, TaxonConstraint> implements JSONSerialise {
	public Map<Integer, TaxonUnion> taxonUnions = new HashMap<Integer, TaxonUnion>();

	public void addTaxonUnion(String id, String name, String taxa) {
		Integer i = Integer.parseInt(id);
		taxonUnions.put(i, new TaxonUnion(i, name, taxa));
	}

	public void addConstraint(String ruleId, String goId, String name, String relationship, String taxIdType, String taxId, String taxonName, String sources) {
		if ("NCBITaxon_Union".equals(taxIdType)) {
			TaxonUnion tu = taxonUnions.get(Integer.parseInt(taxId));
			if (tu != null) {
				taxId = tu.taxa;
			}
		}
		this.put(ruleId, new TaxonConstraint(ruleId, goId, name, relationship, taxIdType, taxId, taxonName, sources));
	}

	public Object serialise() {
		List<Object> constraints = new ArrayList<Object>();

		for (String id : this.keySet()) {
			constraints.add(this.get(id).serialise());
		}

		return constraints;
	}
}
