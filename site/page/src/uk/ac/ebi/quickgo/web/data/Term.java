package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.render.JSONSerialise;

import java.util.*;

public class Term implements Comparable<Term>, JSONSerialise {
	public class Info {
		public int code;
		public String id;
		public String name;
		public String ontology;
		public boolean obsolete;
		public Usage usage;

		public Info(int code, String id, String name, String ontology, boolean obsolete) {
			this.code = code;
			this.id = id;
			this.name = name;
			this.ontology = ontology;
			this.obsolete = obsolete;
			this.usage = Usage.U;
		}
	}

    public static final String ALT_ID = "ALT_ID";
    public static final String CONSIDER = "consider";
    public static final String REPLACED_BY = "replaced_by";
    public static final String GO = "GO";

    public static class XRef {
        public String db;
        public String id;

        public XRef(String db, String id) {
            this.db = db;
            this.id = id;
        }

	    @Override
	    public String toString() {
		    return "XRef{" +
				    "db='" + db + '\'' +
				    ", id='" + id + '\'' +
				    '}';
	    }
    }

    public static class CrossOntologyRelation {
        public String relation;
        public String otherNamespace;
        public String foreignID;
        public String foreignTerm;
        public String url;

        public CrossOntologyRelation(String relation, String otherNamespace, String foreignID, String foreignTerm, String url) {
            this.relation = relation;
            this.otherNamespace = otherNamespace;
            this.foreignID = foreignID;
            this.foreignTerm = foreignTerm;
            this.url = url;
        }
    }

	public static class ProteinComplex {
		public String db;
		public String id;
		public String symbol;
		public String name;

		public ProteinComplex(String db, String id, String symbol, String name) {
			this.db = db;
			this.id = id;
			this.symbol = symbol;
			this.name = name;
		}

		@Override
		public String toString() {
			return "ProteinComplex{" +
					"db='" + db + '\'' +
					", id='" + id + '\'' +
					", symbol='" + symbol + '\'' +
					", name='" + name + '\'' +
					'}';
		}
	}

	public static class NamedURL {
		public String title;
		public String url;

		public NamedURL(String title, String url) {
			this.title = title;
			this.url = url;
		}
	}

    public enum Ontology {
	    P("Process", "Biological Process", "P", "biological_process"),
	    F("Function", "Molecular Function", "F", "molecular_function"),
	    C("Component", "Cellular Component", "C", "cellular_component"),
	    R("Root", "Root", "R", "gene_ontology");

        public String text;
        public String description;
	    public String abbreviation;
	    public String namespace;

        Ontology(String text, String description, String abbreviation, String namespace) {
            this.text = text;
            this.description = description;
	        this.abbreviation = abbreviation;
	        this.namespace = namespace;
        }

	    public static Ontology fromString(String s) throws Exception {
		    if ("Process".equalsIgnoreCase(s)) {
			    return P;
		    }
		    else if ("Function".equalsIgnoreCase(s)) {
			    return F;
		    }
		    else if ("Component".equalsIgnoreCase(s)) {
			    return C;
		    }
			else {
			    throw new Exception("Invalid ontology: " + s);
		    }
	    }
    }

	public enum Usage {
		U("U", "Unrestricted", "This term may be used for any kind of annotation."),
		E("E", "Electronic", "This term should not be used for direct manual annotation. This term may be used for mapping to external vocabularies in order to create electronic annotations."),
		X("X", "None", "This term should not be used for direct annotation.");

		public String code;
		public String text;
		public String description;

		Usage(String code, String text, String description) {
			this.code = code;
			this.text = text;
			this.description = description;
		}

		public static Usage fromString(String s) {
			if ("gocheck_do_not_annotate".equals(s)) {
				return X;
			}
			else if ("gocheck_do_not_manually_annotate".equals(s)) {
				return E;
			}
			else {
				return U;
			}
		}
	}

	public Info info;
    public int code;

	public static class MinimalTermInfo {
		public String id;
		public String name;
		public String usage;

		public MinimalTermInfo(String id, String name, String usage) {
			this.id = id;
			this.name = name;
			this.usage = usage;
		}
	}

	public MinimalTermInfo getMinimalTermInfo() {
		return new MinimalTermInfo(info.id, info.name, info.usage.code);
	}

    public Object serialise() {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("id", info.id);
        map.put("name", info.name);
	    map.put("obsolete", info.obsolete);
	    map.put("aspect", aspect.text);
	    map.put("comment", comment);
	    map.put("definition", definition);
	    map.put("usage", info.usage.text);

	    Set<MinimalTermInfo> lineage = new HashSet<MinimalTermInfo>();
	    for (TermRelation r : ancestors) {
		    if (r.typeof.polarity != RelationType.Polarity.NEGATIVE) {
				lineage.add(r.parent.getMinimalTermInfo());
		    }
	    }
		map.put("ancestors", lineage);	    

        return map;
    }

    public List<TermRelation> parents = new ArrayList<TermRelation>();
    public List<TermRelation> children = new ArrayList<TermRelation>();
    private List<TermRelation> ancestors;
    public List<Synonym> synonyms = new ArrayList<Synonym>();
    public List<XRef> xrefs = new ArrayList<XRef>();
    public List<XRef> altIds = new ArrayList<XRef>();
    public List<XRef> definitionXrefs = new ArrayList<XRef>();
    public List<CrossOntologyRelation> crossOntologyRelations = new ArrayList<CrossOntologyRelation>();
    // in an 'obsoletes' replacement the parent is the replacing term and the child is the obsolete term
    public List<TermRelation> replacements = new ArrayList<TermRelation>();
    public List<TermRelation> replaces = new ArrayList<TermRelation>();
    public List<Terms> slims = new ArrayList<Terms>();
	public List<ProteinComplex> proteinComplexes = new ArrayList<ProteinComplex>();
	public List<TaxonConstraint> taxonConstraints = new ArrayList<TaxonConstraint>();
	public TermOntologyHistory history = new TermOntologyHistory();
	public List<TermCredit> credits = new ArrayList<TermCredit>();
	public List<NamedURL> guidelines = new ArrayList<NamedURL>();
	public List<NamedURL> plannedChanges = new ArrayList<NamedURL>();
    public String definition = "";
    public Ontology aspect;
    public String comment;

	public void associateProteinComplex(String db, String id, String symbol, String name) {
		proteinComplexes.add(new ProteinComplex(db, id, symbol, name));
	}

	public void addTaxonConstraint(TaxonConstraint constraint) {
		taxonConstraints.add(constraint);
	}

	public void addHistoryRecord(AuditRecord ar) {
		history.add(ar);
	}

	public void addCredit(TermCredit credit) {
		credits.add(credit);
	}

    public void addCrossOntologyRelation(String relation, String foreignNamespace, String foreignID, String foreignTerm, String url) {
        crossOntologyRelations.add(new CrossOntologyRelation(relation, foreignNamespace, foreignID, foreignTerm, url));
    }

	public void addGuideline(String title, String url) {
		guidelines.add(new NamedURL(title, url));
	}

	public void addPlannedChange(String title, String url) {
		plannedChanges.add(new NamedURL(title, url));
	}

	public String secondaries() {
		StringBuilder ids = new StringBuilder();
		for (XRef x : altIds) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(x.id);
		}
		return ids.toString();
	}

    public String ontology() {
        return aspect.description;
    }

	public String namespace() {
	    return aspect.namespace;
	}

    public boolean hasComment() {
        return comment != null;
    }

	public List<TermRelation> getChildren() {
		List<TermRelation> kids = new ArrayList<TermRelation>();

		for (TermRelation tr : children) {
			if (tr.ofAnyType(EnumSet.of(RelationType.ISA, RelationType.PARTOF, RelationType.OCCURSIN))) {
				kids.add(tr);
			}
		}

		return kids;
	}

    public List<TermRelation> getAncestors() {
        if (ancestors == null) {
			Set<TermRelation> anc = new HashSet<TermRelation>();
			anc.add(new TermRelation(this, this, RelationType.IDENTITY));
			for (TermRelation relation : parents) {
				for (TermRelation parent : relation.parent.getAncestors()) {
					TermRelation combined = TermRelation.combine(relation, parent);
					if (combined.typeof != RelationType.UNDEFINED) {
						anc.add(combined);
					}
				}
			}
			ancestors = new ArrayList<TermRelation>(anc);
        }

        return ancestors;
    }

    public List<Term> getFilteredAncestors(EnumSet<RelationType> types) {
        Set<Term> results = new HashSet<Term>();
        for (TermRelation relation : getAncestors()) {
            if (relation.ofAnyType(types)) {
	            results.add(relation.parent);
            }
        }
        return new ArrayList<Term>(results);
    }

    public List<Term> getSlimAncestors() {
        return getFilteredAncestors(EnumSet.of(RelationType.ISA, RelationType.PARTOF, RelationType.OCCURSIN));
    }

    public List<Term> getAllAncestors() {
        return getFilteredAncestors(EnumSet.of(RelationType.UNDEFINED));
    }

    public BitSet getAncestors(Term[] terms, EnumSet<RelationType> relations) {
        BitSet results = new BitSet();
        List<Term> anc = getFilteredAncestors(relations);
        for (int i = 0; i < terms.length; i++) {
	        if (anc.contains(terms[i])) {
		        results.set(i);
	        }
        }
        return results;
    }

	public boolean hasAncestor(String id) {
		for (TermRelation r : ancestors) {
			if (id.equals(r.parent.id())) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAncestor(Term term) {
		for (TermRelation r : ancestors) {
			if (term == r.parent) {
				return true;
			}
		}
		return false;
	}

    public List<TermRelation> isa() {
        List<TermRelation> relations = new ArrayList<TermRelation>();

	    for (TermRelation tr : parents) {
		    if (tr.ofType(RelationType.ISA)) {
			    relations.add(tr);
		    }
	    }

        return relations;
    }

    public List<TermRelation> otherParents() {
	    List<TermRelation> relations = new ArrayList<TermRelation>();

		for (TermRelation tr : parents) {
			if (!tr.ofType(RelationType.ISA) && !tr.ofType(RelationType.OCCURSIN) && !tr.ofType(RelationType.HASPART)) {
				relations.add(tr);
			}
		}

        return relations;
    }

    public Term(String id, String name,int index, String aspect, String is_obsolete) {
        this.aspect = Ontology.valueOf(aspect);
        this.code = Integer.parseInt(id.substring(3));
		this.info = new Info(index, id, name, this.aspect.text, "Y".equals(is_obsolete));
    }

    public static Comparator<Term> ancestorComparator=new Comparator<Term>() {
        public int compare(Term t1, Term t2) {
            return t1.getAllAncestors().size()-t2.getAllAncestors().size();
        }
    };

	public ArrayList<String> xrefsText() {
	    ArrayList<String> list = new ArrayList<String>();
	    for (XRef xr : xrefs) {
	        list.add(xr.id);
	    }
		for (XRef xr : altIds) {
		    list.add(xr.id);
		}
		for (TermRelation tr : replacements) {
		    list.add(tr.parent.id());
		}
		for (TermRelation tr : replaces) {
		    list.add(tr.child.id());
		}
	    return list;
	}

	public ArrayList<String> synonymText() {
	    ArrayList<String> list = new ArrayList<String>();
	    for (Synonym s : synonyms) {
	        list.add(s.name);
	    }
	    return list;
	}

    public int compareTo(Term term) {
        return id().compareTo(term.id());
    }

	public String id() {
		return info.id;
	}
	
	public String name() {
		return info.name;
	}

	public int index() {
		return info.code;
	}

	public boolean obsolete() {
		return info.obsolete;
	}

	public boolean active() {
		return !info.obsolete;
	}

	public ArrayList<Term> replacedBy() {
		ArrayList<Term> replacedBy = new ArrayList<Term>();
	    for (TermRelation r : replacements) {
		    if (r.ofType(RelationType.REPLACEDBY)) {
			    replacedBy.add(r.parent);
		    }
	    }
	    return replacedBy;
	}

	public ArrayList<Term> consider() {
		ArrayList<Term> replacedBy = new ArrayList<Term>();
	    for (TermRelation r : replacements) {
		    if (r.ofType(RelationType.CONSIDER)) {
			    replacedBy.add(r.parent);
		    }
	    }
	    return replacedBy;
	}

	public String gonutsURL() {
		return "http://gowiki.tamu.edu/wiki/index.php/Category:" + id();	
	}

	public void setUsage(String usage) {
		info.usage = Usage.fromString(usage);
	}

	public String usage() {
		return info.usage.description;
	}

	public boolean isRestricted() {
		return info.usage != Usage.U;
	}

	public boolean isUnrestricted() {
		return info.usage == Usage.U;
	}

    @Override
    public String toString() {
        return id();
    }
}