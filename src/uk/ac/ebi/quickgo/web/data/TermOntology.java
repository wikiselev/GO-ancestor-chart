package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.quickgo.web.configuration.*;
import static uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import uk.ac.ebi.quickgo.web.render.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.*;

import java.util.*;
import java.io.*;

public class TermOntology {
    //private static Location me = new Location();

    public Map<String,Term> xrefFind=new HashMap<String,Term>();
    public Table termTable;
    public Term[] terms;
    public int[] termTieBreak;
    public Map<String,Terms> slims = new HashMap<String,Terms>();
	public TaxonConstraintSet taxonConstraints = new TaxonConstraintSet();
	public TermOntologyHistory history = new TermOntologyHistory();

	public CV fundingBodies;

	public Map<String, TermCredit> termCredits = new HashMap<String, TermCredit>();
	
    public String version;
    public String timestamp;
    public String url;

    private static final String root = "GO:0003673";

    public TermOntology(DataLocation directory) throws Exception {
        termTable = new TextTableReader(directory.termIDs.file()).extractColumn(0);
        terms = new Term[termTable.size()];
        termTieBreak = new int[termTable.size()];

        for (String[] row :directory.goTermNames.reader(GOTermNames.GO_ID, GOTermNames.NAME, GOTermNames.CATEGORY, GOTermNames.IS_OBSOLETE)) {
            String id = row[0];
            int index = termTable.search(id);
            if (index < 0 || index >= termTable.size()) continue;
			if (!termTable.values[index].equals(id)) continue;

            Term t = new Term(id, row[1], index, row[2], row[3]);
            terms[index]= t;
            termTieBreak[index] = t.name().length();
        }

        for (String[] row :directory.goRelations.reader(GORelation.CHILD_ID, GORelation.PARENT_ID, GORelation.RELATION_TYPE)) {                 
            if (row[1].equals(root)) continue;
            Term child = getTerm(row[0]);
            Term parent =getTerm(row[1]);
            if (child == null || parent == null) continue;
            TermRelation tr = new TermRelation(child, parent, row[2].intern());
            child.parents.add(tr);
            parent.children.add(tr);
        }

        for (Term t : terms) {
			if (t != null) {
				t.getAncestors();
			}
        }

        for (String[] row :directory.goTermSynonyms.reader(GOTermSynonyms.TERM_ID, GOTermSynonyms.NAME, GOTermSynonyms.TYPE)) {
            Term term = getTerm(row[0]);
            if (term != null) {
                term.synonyms.add(new Synonym(row[2], row[1]));
            }
        }

        for (String[] row :directory.goTermDefinitions.reader(GOTermDefinitions.TERM_ID, GOTermDefinitions.DEFINITION)) {
            Term term = getTerm(row[0]);
            if (term != null) {
                term.definition = row[1];
            }
        }

        for (String[] row : directory.goComments.reader(GOComments.TERM_ID, GOComments.COMMENT_TEXT)) {
            Term term = getTerm(row[0]);
            if (term != null) {
	            term.comment = row[1];
            }
        }

        for (String[] row : directory.goCrossOntologyRelations.reader(GOCrossOntologyRelation.TERM_ID, GOCrossOntologyRelation.RELATION, GOCrossOntologyRelation.FOREIGN_NAMESPACE, GOCrossOntologyRelation.FOREIGN_ID, GOCrossOntologyRelation.FOREIGN_TERM, GOCrossOntologyRelation.URL)) {
            Term term = getTerm(row[0]);
            if (term != null) {
                term.addCrossOntologyRelation(row[1], row[2], row[3], row[4], row[5]);
            }
        }

        ColourList colour = new ColourList(0x00000000);

        for (String[] row :directory.subsets.reader(Subset.TERM_ID, Subset.SUBSET, Subset.TYPE)) {
            Term term = getTerm(row[0]);
            if (term == null) {
	            continue;
            }
	        if ("SLIM".equals(row[2])) {
		        Terms slim = slims.get(row[1]);
		        if (slim == null) {
			        slims.put(row[1], slim = new Terms(this, row[1], colour.getColourCode(slims.size())));
		        }
		        term.slims.add(slim);
		        slim.add(term);
	        }
	        else {
				term.setUsage(row[1]);		        
	        }
        }

        for (String[] row : directory.goXrefs.reader(GOXref.TERM_ID, GOXref.DB_CODE, GOXref.DB_ID)) {
            Term term = getTerm(row[0]);
            if (term == null) {
                continue;
            }

            Term.XRef ref = new Term.XRef(row[1], row[2]);
            if (row[1].equals(Term.REPLACED_BY) || row[1].equals(Term.CONSIDER)) {
                Term obsolete = getTerm(row[2]);
                if (obsolete != null) {
	                TermRelation tr = new TermRelation(obsolete, term, row[1]);
                    obsolete.replacements.add(tr);
                    term.replaces.add(tr);
                }
            }
            else if (row[1].equals(Term.GO)) {
                // ignore GO self-referential cross-refs
            }
            else if (row[1].equals(Term.ALT_ID)) {
                term.altIds.add(ref);
                xrefFind.put(row[2],term);
            }
            else {
                term.xrefs.add(ref);
            }
        }

        for (String[] row : directory.goDefinitionXrefs.reader(GOXref.TERM_ID, GOXref.DB_CODE, GOXref.DB_ID)) {
            Term term = getTerm(row[0]);
            if (term == null) {
                continue;
            }

            if ("PMID".equals(row[1])) {
                term.definitionXrefs.add(new Term.XRef(row[1], row[2]));
            }
        }

        for (String[] row:directory.version.reader(Version.VERSION, Version.TIMESTAMP, Version.URL)) {
            version = row[0];
            timestamp = row[1];
            url = row[2];
        }

	    for (String[] row : directory.proteinComplexes.reader(ProteinComplexes.GO_ID, ProteinComplexes.DB, ProteinComplexes.DB_OBJECT_ID, ProteinComplexes.DB_OBJECT_SYMBOL, ProteinComplexes.DB_OBJECT_NAME)) {
		    Term term = getTerm(row[0]);
		    if (term != null) {
			    term.associateProteinComplex(row[1], row[2], row[3], row[4]);				
		    }
	    }

	    for (String[] row : directory.taxonUnions.reader(TaxonUnions.UNION_ID, TaxonUnions.NAME, TaxonUnions.TAXA)) {
		    taxonConstraints.addTaxonUnion(row[0], row[1], row[2]);
	    }

	    for (String[] row : directory.taxonConstraints.reader(TaxonConstraints.RULE_ID, TaxonConstraints.GO_ID, TaxonConstraints.NAME, TaxonConstraints.RELATIONSHIP, TaxonConstraints.TAX_ID_TYPE, TaxonConstraints.TAX_ID, TaxonConstraints.TAXON_NAME, TaxonConstraints.SOURCES)) {
		    taxonConstraints.addConstraint(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]);
	    }

	    for (String[] row : directory.termTaxonConstraints.reader(TermTaxonConstraints.GO_ID, TermTaxonConstraints.RULE_ID)) {
		    Term term = getTerm(row[0]);
		    if (term != null) {
			    term.addTaxonConstraint(taxonConstraints.get(row[1]));
		    }
	    }

	    for (String[] row : directory.goTermHistory.reader(GOTermHistory.TERM_ID,  GOTermHistory.NAME, GOTermHistory.TIMESTAMP, GOTermHistory.ACTION, GOTermHistory.CATEGORY, GOTermHistory.TEXT)) {
		    AuditRecord ar = new AuditRecord(row[0], row[1], row[2], row[3], row[4], row[5]);
		    history.add(ar);

		    Term term = getTerm(row[0]);
			if (term != null) {
				if (term.id().equals(row[0])) {
					term.addHistoryRecord(ar);
				}
			}
	    }

	    fundingBodies = new CV(directory.fundingBodies.reader(FundingBodies.CODE, FundingBodies.URL));

	    for (String[] row : directory.goTermCredits.reader(GOTermCredits.TERM_ID, GOTermCredits.CREDIT_CODE)) {
		    Term term = getTerm(row[0]);
		    if (term != null) {
			    TermCredit credit = termCredits.get(row[1]);
			    if (credit == null) {
				    CV.Item fundingBody = fundingBodies.get(row[1]);
				    credit = new TermCredit(row[1], (fundingBody != null ? fundingBody.description : null));
				    termCredits.put(row[1], credit);
			    }
			    term.addCredit(credit);
		    }
	    }

	    for (String[] row : directory.annotationGuidelines.reader(AnnotationGuidelinesInfo.GO_ID, AnnotationGuidelinesInfo.TITLE, AnnotationGuidelinesInfo.URL)) {
		    Term term = getTerm(row[0]);
		    if (term != null) {
			    term.addGuideline(row[1], row[2]);
		    }
	    }

	    for (String[] row : directory.plannedGOChanges.reader(PlannedGOChangesInfo.GO_ID, PlannedGOChangesInfo.TITLE, PlannedGOChangesInfo.URL)) {
		    Term term = getTerm(row[0]);
		    if (term != null) {
			    term.addPlannedChange(row[1], row[2]);
		    }
	    }
    }

    public Term getTerm(String id) {
        int index=termTable.search(id);
        if (index<0) {
            return xrefFind.get(id);            
        }
        return terms[index];
    }

    public static void main(String[] args) throws Exception {
        TermOntology ontology = new TermOntology(new DataManager(new File(args[0])).getDirectory());
	    System.out.println();

/*
        for (Term t : ontology.terms) {
            List<Term> terms = new ArrayList<Term>();
            terms.addAll(t.getFilteredAncestors(EnumSet.of(RelationType.REGULATES)));
            terms.removeAll(t.getSlimAncestors());
            if (!terms.isEmpty()) {
	            System.out.print(t.id() + " regulates: ");
	            for (Term rt : terms) {
	                System.out.print(rt.id() + " ");
	            }
	            System.out.println();
            }
        }
	    System.out.println();
*/

	    System.out.println("Ancestors of " + args[1] + ":");
        List<TermRelation> anc = ontology.getTerm(args[1]).getAncestors();
        for (TermRelation a : anc) {
            System.out.println(a.typeof + " " + a.parent + " (" + a.parent.name() + ")");
        }
	    System.out.println();

	    System.out.println("Slim Ancestors of " + args[1] + ":");
        List<Term> terms = ontology.getTerm(args[1]).getSlimAncestors();
        for (Term term : terms) {
            System.out.println(term.id() + " (" + term.name() + ")");
        }
	    System.out.println();

        System.out.println("Map to:");
        Slimmer slimmer = new Slimmer(ontology, new String[]{ "GO:0009653", "GO:0044767", "GO:0032502" }, null);
        int[] mapTo = slimmer.slimTranslate[ontology.getTerm("GO:0000902").index()];
        for (int i : mapTo) {
            System.out.println(ontology.terms[i]);
        }
	    System.out.println();
    }
}
