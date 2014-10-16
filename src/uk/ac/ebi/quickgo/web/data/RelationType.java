package uk.ac.ebi.quickgo.web.data;

import java.awt.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
* User: dbinns
* Date: 15-Jan-2010
* Time: 16:07:23
* To change this template use File | Settings | File Templates.
*/
public enum RelationType {
    UNDEFINED("?", "Ancestor", "ancestor"),
    IDENTITY("=", "Identity", "equals"),
    ISA("I", "Is a", "is_a", new Color(0,0,0)),
    PARTOF("P", "Part of", "part_of", new Color(0,0,255)),
    REGULATES("R", "Regulates", "regulates", new Color(255,192,0)),
    POSITIVEREGULATES("+", "Positively regulates", "positively_regulates", "PR", new Color(0,255,0)),
    NEGATIVEREGULATES("-", "Negatively regulates", "negatively_regulates", "NR", new Color(255,0,0)),
    REPLACEDBY(">", "Replaced by", "replaced_by", "replaced_by", new Color(255,0,255)),
    CONSIDER("~", "Consider", "consider", "consider", new Color(192,0,255)),
	HASPART("H", "Has part", "has_part", new Color(128,0,128), Polarity.NEGATIVE, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] { 3.0f, 3.0f }, 0.0f)),
	OCCURSIN("O", "Occurs in", "occurs_in", "OI", new Color(0,128,128)),
	;

	public enum Polarity {
		POSITIVE,   // relation is unidirectional from child to parent
		NEGATIVE,   // relation is unidirectional from parent to child
		NEUTRAL,    // relation is non-directional
		BIPOLAR     // relation is bi-directional 
	}

    public String code;
    public String description;
    public String formalCode;
    public String alternativeCode;
    public Color color;
	public Polarity polarity;
	public Stroke stroke;

    RelationType(String code, String description, String formalCode, String alternativeCode, Color color, Polarity polarity, Stroke stroke) {
        this.code = code;
        this.description = description;
        this.alternativeCode = alternativeCode;
        this.color = color;
	    this.stroke = stroke;
	    this.polarity = polarity;
        this.formalCode = formalCode;
    }

	RelationType(String code, String description, String formalCode, String alternativeCode, Color color) {
		this(code, description, formalCode, alternativeCode, color, Polarity.POSITIVE, new BasicStroke(2f));
	}

    RelationType(String code, String description, String formalCode, Color color, Polarity polarity, Stroke stroke) {
	    this(code, description, formalCode, code, color, polarity, stroke);
    }

	RelationType(String code, String description, String formalCode, Color color) {
		this(code, description, formalCode, code, color, Polarity.POSITIVE, new BasicStroke(2f));
	}

    RelationType(String code, String description, String formalCode) {
	    this(code, description, formalCode, code, new Color(0, 0, 0), Polarity.POSITIVE, new BasicStroke(2f));
    }

    boolean ofType(RelationType query) {
        return (query == RelationType.UNDEFINED) || (this == IDENTITY) || (query == this) || (query == REGULATES && (this == POSITIVEREGULATES || this == NEGATIVEREGULATES));
    }

    boolean ofAnyType(EnumSet<RelationType> types) {
        for (RelationType type : types) {
            if (ofType(type)) return true;
        }
        return false;
    }

    public static RelationType byCode(String code) {
        for (RelationType rt : values()) {
            if (rt.code.equals(code) || code.equals(rt.alternativeCode)) return rt;
        }
        throw new IllegalArgumentException("No such relation type as "+code);
    }

    public static EnumSet<RelationType> forCodes(String types) {
        Set<RelationType> rt=new HashSet<RelationType>();
        for (int i=0;i<types.length();i++) {
            rt.add(byCode(""+types.charAt(i)));
        }
        return EnumSet.copyOf(rt);
    }
}
