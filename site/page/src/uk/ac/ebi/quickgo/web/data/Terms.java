package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.render.*;

import java.io.IOException;
import java.io.Writer;
import java.text.*;
import java.util.*;

public class Terms implements JSONSerialise {
    static NumberFormat nf = new DecimalFormat("0000000");

    private TermOntology ontology;
    public String name;
    public int colour;
    public String cssColour() {
	    return ColourUtils.css(colour);
    }

    public Terms(TermOntology ontology) {
        this.ontology = ontology;
    }

    private Map<Term,String> selection = new TreeMap<Term,String>();

    public Terms(TermOntology ontology, String name, int colour) {
        this.ontology = ontology;
        this.name = name;
        this.colour = colour;
    }

    public Terms(TermOntology ontology, String name) {
        this(ontology);
        this.name = name;        
    }

    public void addRemoveAll(String[] all, boolean add) {
        if (all != null) {
	        for (String idList : all) {
	            for (String id : idList.split("[^-A-Za-z0-9#\\:]+")) {
	                addRemove(id, add);
	            }
	        }
        }
    }

    private void addRemove(String id, boolean add) {
		String[] termColour = id.split("#");
        Term t = ontology.getTerm(termColour[0]);
        if (t != null) {
	        String colour = (termColour.length > 1) ? termColour[1] : "";
            if (add) {
	            selection.put(t, colour);
            }
			else{
	            selection.remove(t);
            }
        }
    }

    public void add(Term term) {
        selection.put(term, "");
    }

    public void add(int id) {
        add("GO:" + nf.format(id));
    }

    public void add(String id) {
        addRemove(id, true);
    }

    public void addAll(String[] all) {
        addRemoveAll(all, true);
    }

    public void removeAll(String[] all) {
        addRemoveAll(all, false);        
    }

    private static final String indexTable = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz[]";
	private static final String sentinel = "64$";

	public static String convert10to64(int decimalInt) {
		String s = "";

		while (decimalInt > 0) {
			s = indexTable.charAt(decimalInt % 64) + s;
			decimalInt = (int)Math.floor(decimalInt / 64);
		}

		return s;
	}

	public static int convert64to10(String num) {
		int ret = 0;

		for (int x = 1; num.length() > 0; x *= 64) {
			ret += indexTable.indexOf(num.charAt(num.length() - 1)) * x;
			num = num.substring(0, num.length() - 1);
		}

		return ret;
	}

    public void addCompressed(String code) {
        if (code != null) {
			if (code.length() >= sentinel.length() && sentinel.equals(code.substring(0, sentinel.length()))) {
				for (int i = sentinel.length(); i < code.length(); i += 4) {
					add(convert64to10(code.substring(i, i + 4)));
				}
			}
			else {
				// old-style base 36 encoding
				for (int i = 0; i < code.length(); i += 4) {
					add(Integer.parseInt(code.substring(i, i + 4), 36));
				}
			}
        }
    }

    public String getCompressed() {
        StringBuilder compressed = new StringBuilder();
        for (Term t : selection.keySet()) {
	        String h = convert10to64(t.code);
            compressed.append("0000".substring(h.length())).append(h);
        }
        return compressed.length() > 0 ? sentinel + compressed.toString() : "";
    }

    public String getIdList() {
        StringBuilder compressed=new StringBuilder();
        for (Term t : selection.keySet()) {
            compressed.append(t.id()).append(" ");
        }
        return compressed.toString();
    }


    public List<Term> getTerms() {
        List<Term> terms = new ArrayList<Term>();
        terms.addAll(selection.keySet());

        return terms;
    }

    public void clear() {
        selection.clear();
    }

    public int count() {
        return selection.size();
    }

    public String[] getIDs() {
        String[] ids = new String[selection.size()];
        int i = 0;
        for (Term t : selection.keySet()) {
            ids[i++] = t.id();
        }
        return ids;
    }

    public Object serialise() {
        return name;
    }

    public Term[] getTermArray() {
        return selection.keySet().toArray(new Term[selection.size()]);
    }

	public String getTermInfo(Term term) {
		return selection.get(term);
	}

	public void write(Writer wr) throws IOException {
		for (Term t : selection.keySet()) {
			wr.write(t.id() + "\t" + t.aspect.abbreviation + "\t" + t.name() + "\n");
		}
	}
}
