package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.quickgo.web.data.*;

import java.io.*;
import java.util.*;


/**
 * Map annotation to slim terms.
 *
 * See; http://search.cpan.org/~cmungall/go-perl/scripts/map2slim
 */

public class Slimmer implements DataAction {
    private static Location me = new Location();

    public int[][] ancestorTranslate;
    public int[][] slimTranslate;
	public EnumSet<RelationType> types = TermRelation.defaultRelationTypes();

    public Slimmer(TermOntology ontology, String[] slimIDs, DataAction target,String types) {
        this(ontology, slimIDs, types);
        this.target = target;
    }

    public Slimmer(TermOntology ontology, String[] slimIDs,String typeCodes) {
        if (typeCodes != null && typeCodes.trim().length() > 0) {
	        this.types = RelationType.forCodes(typeCodes);
        }

        Action action = me.start("Getting ancestors");

        int termCount = ontology.terms.length;
        ancestorTranslate = new int[termCount][];

        for (int i = 0; i < ancestorTranslate.length; i++) {
            Term t = ontology.terms[i];
            List<Term> slimAncestors = t.getSlimAncestors();
            int c = 0;
            ancestorTranslate[i] = new int[slimAncestors.size()];
            for (Term a : slimAncestors) {
                ancestorTranslate[i][c++] = a.index();
            }
        }

        me.stop(action);

        if (slimIDs == null) {
	        return;
        }

        action = me.start("Slimming");

        Terms termset = new Terms(ontology);
        termset.addAll(slimIDs);

        // slim terms
        Term[] terms = termset.getTermArray();
        // slim terms which exclude other slim terms
        BitSet[] exclude = new BitSet[terms.length];

        for (int i = 0; i < terms.length; i++) {
            // a term excludes all its ancestors from being used as slim terms
            exclude[i] = terms[i].getAncestors(terms, types);
            // a term does not exclude itself from a slim
            exclude[i].clear(i);
        }

        // for each term indexes, an array of term indexes which it maps to
        slimTranslate = new int[termCount][];

        for (int i = 0; i < slimTranslate.length; i++) {
            // ancestors in the slim of the term
            BitSet ancestors = ontology.terms[i].getAncestors(terms, types);
            BitSet slimTerms = (BitSet)ancestors.clone();
            // iterate the slim ancestors, removing any ancestors which they exclude
            for (int p = 0; (p = ancestors.nextSetBit(p)) >= 0; p++) {
                slimTerms.andNot(exclude[p]);
            }
            // for each remaining slim term lookup its index
            slimTranslate[i] = new int[slimTerms.cardinality()];
            int c = 0;
            for (int p = 0; (p = slimTerms.nextSetBit(p)) >= 0; p++) {
                slimTranslate[i][c++] = terms[p].index();
            }
        }

        me.stop(action);
    }

    DataAction target;

    public boolean act(AnnotationRow row) throws IOException {
        if (slimTranslate == null) {
	        return target.act(row);
        }
	    else {
			for (int t : slimTranslate[row.term]) {
				row.term = t;
				if (!target.act(row)) {
					return false;
				}
			}
			return true;
        }
    }
}
