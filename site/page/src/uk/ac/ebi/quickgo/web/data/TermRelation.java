package uk.ac.ebi.quickgo.web.data;

import java.util.*;

public class TermRelation {
    public Term child;
    public Term parent;
    public RelationType typeof;

    boolean ofAnyType(EnumSet<RelationType> types) {
        return typeof.ofAnyType(types);
    }

    public boolean ofType(RelationType type) {
        return typeof.ofType(type);
    }

	public static EnumSet<RelationType> defaultRelationTypes() {
		return EnumSet.of(RelationType.ISA, RelationType.PARTOF, RelationType.OCCURSIN);
	}

    /**
     * Combining relations. See:
     * http://www.geneontology.org/GO.ontology-ext.relations.shtml
     *
     * @param child Child relationship
     * @param parent Parent relationship
     * @return combined relationship
     */

    static TermRelation combine(TermRelation child, TermRelation parent) {
        if (!child.parent.equals(parent.child)) {
	        throw new RuntimeException("Incorrectly combined relationships");
        }

        RelationType mergedType = RelationType.UNDEFINED;

        if (child.ofType(RelationType.IDENTITY)) {
	        mergedType = parent.typeof;
        }
        else if (parent.ofType(RelationType.IDENTITY)) {
	        mergedType = child.typeof;
        }
        else if (child.ofType(RelationType.ISA)) {
	        mergedType = parent.typeof;
        }
        else if (parent.ofType(RelationType.ISA)) {
	        mergedType = child.typeof;
        }
        else if (child.ofType(RelationType.PARTOF) && parent.ofType(RelationType.PARTOF)) {
	        mergedType = RelationType.PARTOF;
        }
        else if (child.ofType(RelationType.OCCURSIN)) {
	        mergedType = RelationType.OCCURSIN;
        }
        else if (child.ofType(RelationType.REGULATES) && parent.ofType(RelationType.PARTOF)) {
	        mergedType = RelationType.REGULATES;
        }
/*
        else if (child.ofType(RelationType.HASPART) && parent.ofType(RelationType.HASPART)) {
	        mergedType = RelationType.HASPART;
        }
*/

        return new TermRelation(child.child, parent.parent, mergedType);
    }

    public TermRelation(Term child, Term parent, String typeof) {
        this(child,parent, RelationType.byCode(typeof));
    }

    public TermRelation(Term child, Term parent, RelationType typeof) {
        this.child = child;
        this.parent = parent;
        this.typeof = typeof;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
	        return true;
        }
        if (o == null || getClass() != o.getClass()) {
	        return false;
        }

        TermRelation that = (TermRelation)o;
        return (child.equals(that.child) && parent.equals(that.parent) && typeof.equals(that.typeof));
    }

    @Override
    public int hashCode() {
        int result = child.hashCode();
        result = 31 * result + parent.hashCode();
        result = 31 * result + typeof.hashCode();
        return result;
    }

    public String getParentCode() {
        return typeof.code + parent.id();
    }

	@Override
	public String toString() {
		return "TermRelation{" +
				"child=" + child +
				", parent=" + parent +
				", typeof=" + typeof +
				'}';
	}
}