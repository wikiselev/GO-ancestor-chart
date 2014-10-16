package uk.ac.ebi.quickgo.web.configuration;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class UniProtAc {
	public String db;
	public String accession;
	public String splice;

/*
	private final static Pattern uniProtPrefixPattern = Pattern.compile("^(UniProt|UniProtKB(/TrEMBL|/Swiss-Prot)?)$");
	private final static Matcher uniProtPrefixMatcher = uniProtPrefixPattern.matcher("");
*/

	private final static Pattern uniProtAcPattern = Pattern.compile("^((UniProt|UniProtKB(?:/TrEMBL|/Swiss-Prot)?):)?(([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])((-([0-9]+))|:(PRO_[0-9]{10}|VAR_[0-9]{6}))?)$");
	private final static Matcher uniProtAcMatcher = uniProtAcPattern.matcher("");
	
	public UniProtAc(String db, String accession, String splice) {
		this.db = db;
		this.accession = accession;
		this.splice = (splice != null) ? splice.substring(1) : null;
	}

	public static UniProtAc parse(String db, String dbObjectId) {
		return parse(db + ":" + dbObjectId);
	}

	public static UniProtAc parse(String s) {
		uniProtAcMatcher.reset(s);
		if (uniProtAcMatcher.matches()) {
			// group(2) = db, group(3) = virtualGroupingId+splice/chain [group(4) = virtualGroupingId, group(6) = splice/chain/variant]
			return new UniProtAc(uniProtAcMatcher.group(2), uniProtAcMatcher.group(4), uniProtAcMatcher.group(6));
		}
		else {
			return null;
		}
	}

    public static String getCanonicalAccession(String s) {
        uniProtAcMatcher.reset(s);
        return uniProtAcMatcher.matches() ? uniProtAcMatcher.group(4) : "";
    }

/*
	public static boolean isUniProt(String prefix) {
		uniProtPrefixMatcher.reset(prefix);
		return uniProtPrefixMatcher.matches();
	}

	public static boolean isUniProtAc(String db, String id) {
		uniProtAcMatcher.reset(db + ":" + id);
		return uniProtAcMatcher.matches();
	}
*/

	@Override
	public String toString() {
		return "UniProtAc{" +
				"db='" + db + '\'' +
				", accession='" + accession + '\'' +
				", splice='" + splice + '\'' +
				'}';
	}

/*
	public static void main(String[] args) {
		String[] acs = {
				"UniProtKB:O14104",
				"UniProtKB/Swiss-Prot:O14104-1",
				"UniProtKB/TrEMBL:O14104-2",
				"UniProtKB/trembl:O14104",
				"TAIR:gene:123456",
				"UniProt:O14104",
				"UniProt/TrEMBL:O14104",
				"UniProtKB:P21278:PRO_1234567890",
				"P123412345",
				"P12341A234",
				"A1A123A123-1",
				"P12345:PRO_1234567890",
				"UniProtKB:A1A123A123:PRO_1234567890",
				"UniProtKB:A1A123A123:VAR_123456",
				"A1A123A123:VAR_123456"
		};
		for (String ac : acs) {
			UniProtAc uac = UniProtAc.parse(ac);
			System.out.println(ac + " => " + (uac == null ? "no match" : uac));
		}

		String[] dbs = {"UniProt", "UniProtKB", "UniProtKB/Swiss-Prot", "UniProtKB/TrEMBL", "UniProt/Swiss-Prot", "UniProt/TrEMBL"};
		for (String db : dbs) {
			boolean b = UniProtAc.isUniProt(db);
			System.out.println(db + " => " + b);
		}
	}
*/
}
