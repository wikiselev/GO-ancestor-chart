package uk.ac.ebi.quickgo.web.configuration;

import java.util.*;
import java.io.PrintStream;

import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;

public class Gp2ProteinMap extends HashMap<String, Gp2ProteinMap.ProteinMapping> {
	ProteinMapping uniProtKBMap;

	public static class UniProt2ProteinMap extends TreeMap<String, ArrayList<String>> {
		public void insert(String uniProtAc, String protein) {
			ArrayList<String> proteins = get(uniProtAc);
			if (proteins == null) {
				proteins = new ArrayList<String>();
				put(uniProtAc, proteins);
			}
			proteins.add(protein);
		}
	}

	public static class ProteinMapping extends TreeMap<String, String> {
		public UniProt2ProteinMap invert() {
			UniProt2ProteinMap inverse = new UniProt2ProteinMap();

			for (String protein : keySet()) {
				inverse.insert(get(protein), protein);
			}

			return inverse;
		}
	}

	public Gp2ProteinMap() {
		uniProtKBMap = getProteinMapping("UniProtKB");
	}

	ProteinMapping getProteinMapping(String db) {
		ProteinMapping pm = get(db);
		if (pm == null) {
			pm = new ProteinMapping();
			put(db, pm);
		}
		return pm;
	}

	public void loadAll(NamedFile[] gpFiles) throws Exception {
		for (NamedFile f : gpFiles) {
			load(f);
		}
	}

	public void load(NamedFile gpFile) throws Exception {
		String lastDb = "";
		ProteinMapping pm = null;

		System.out.println("Processing gp2protein file " + gpFile.getName());
		TSVFile gpf = new TSVFile(gpFile, 2);

		int cntTotal = 0;
		int cntMapped = 0;

		for (String[] cols : gpf.reader()) {
			cntTotal++;
			String[] dbId = cols[0].split(":", 2);

			if (cols[1] != null && cols[1].length() > 0) {
				// one local virtualGroupingId can be mapped to a (pipe- or semi-colon-separated) list of other accessions
				//
				// at present, we are only interested in mappings to UniProt accessions, and only the first one that we come across
				String[] accessions = cols[1].split("[\\|;]");
				String uniProtAc = null;

				for (String ac : accessions) {
					UniProtAc uac = UniProtAc.parse(ac);
					if (uac != null) {
						uniProtAc = uac.accession;
						break;
					}
				}
				// at this point it is possible that we didn't find a mapping that we liked
				if (uniProtAc != null) {
					if (!lastDb.equals(dbId[0])) {
						pm = getProteinMapping(dbId[0]);
						lastDb = dbId[0];
					}
					pm.put(dbId[1], uniProtAc);
					uniProtKBMap.put(uniProtAc, uniProtAc);
					cntMapped++;
				}
			}
		}
		System.out.println(gpFile.getName() + ": total number of mappings: " + cntTotal + "  mappings to UniProt: " + cntMapped);
	}

	public String getVirtualGroupingId(String db, String dbObjectId) {
		ProteinMapping pm = getProteinMapping(db);
		String vgi = pm.get(dbObjectId);
		if (vgi == null) {
			UniProtAc uac = UniProtAc.parse(db, dbObjectId);
			if (uac != null) {
				vgi = uac.accession;
				pm.put(uac.accession, uac.accession);
				uniProtKBMap.put(uac.accession, uac.accession);
			}
			else {
				vgi = db + ":" + dbObjectId;
				pm.put(dbObjectId, vgi);
			}
		}
		return vgi;
	}

	public UniProt2ProteinMap invert() {
		UniProt2ProteinMap inverse = new UniProt2ProteinMap();

		for (String db  : keySet()) {
			ProteinMapping pm = get(db);
			for (String protein : pm.keySet()) {
				inverse.insert(pm.get(protein), db + ":" + protein);
			}
		}

		return inverse;
	}

	public void dump(String fileName) throws Exception {
		PrintStream ps = (fileName == null) ? System.out : new PrintStream(fileName);

		for (String db  : keySet()) {
			ps.println("*** GP2PROTEIN MAPPING FOR DB " + db);
			ProteinMapping pm = get(db);
			for (String id : pm.keySet()) {
				ps.println("[" + db + ":" + id + "] => [" + pm.get(id) + "]");
			}
		}

		if (fileName != null) {
			ps.close();
		}
	}
}

