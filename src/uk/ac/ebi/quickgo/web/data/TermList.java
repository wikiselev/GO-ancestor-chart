package uk.ac.ebi.quickgo.web.data;

import java.util.HashMap;
import java.util.Map;

public class TermList {
	private Map<String, Term> terms = new HashMap<String, Term>();
	private TermOntology ontology;

	public TermList(TermOntology ontology) {
		this.ontology = ontology;
	}

	public void addAll(String[] all) {
		if (all != null) {
			for (String idList : all) {
			    for (String id : idList.split("[^-A-Za-z0-9\\:]+")) {
				    Term term = ontology.getTerm(id);
				    if (term != null) {
					    terms.put(id, term);
				    }
			    }
			}
		}
	}

	public int count() {
		return terms.size();
	}

	public boolean contains(String id) {
		return terms.get(id) != null;
	}
}
