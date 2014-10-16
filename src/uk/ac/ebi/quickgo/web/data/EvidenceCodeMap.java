package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;

import java.util.HashMap;

public class EvidenceCodeMap {
	static class GORef2ECOMap {
		private HashMap<String, String> map = new HashMap<String, String>();
		private static final String defaultKey = "default";

		public void add(String goRef, String ecoId) {
			map.put((goRef == null || "".equals(goRef)) ? defaultKey : goRef, ecoId);
		}

		public String find(String goRef) {
			if (goRef == null) {
				return map.get(defaultKey);
			}
			else {
				String s = map.get(goRef);
				if (s == null) {
					s = map.get(defaultKey);
				}
				return s;
			}
		}

/*
		public void dump() {
			for (String r : map.keySet()) {
				System.out.println("     " + r + " => " + map.get(r));
			}
		}
*/
	}

	private HashMap<String, GORef2ECOMap> map = new HashMap<String, GORef2ECOMap>();

	public GORef2ECOMap get(String code) {
		GORef2ECOMap m = map.get(code);
		if (m == null) {
			m = new GORef2ECOMap();
			map.put(code, m);
		}
		return m;
	}

	public EvidenceCodeMap(DataLocation directory) throws Exception {
		for (String[] row : directory.evidence2ECO.reader(Evidence2ECOInfo.CODE, Evidence2ECOInfo.GO_REF, Evidence2ECOInfo.ECO_ID)) {
			get(row[0]).add(row[1], row[2]);
		}
	}

	public String translate(String code, String goRef) {
		GORef2ECOMap m = get(code);
		return (m != null) ? m.find(goRef) : null;
	}

/*
	public void dump() {
		System.out.println("EvidenceCodeMap: Begin");
		for (String e : map.keySet()) {
			System.out.println("-- " + e);
			map.get(e).dump();
		}
		System.out.println("EvidenceCodeMap: End");
	}
*/
}
