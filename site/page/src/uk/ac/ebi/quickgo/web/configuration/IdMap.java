package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.compress.IndexSort;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.File;

public class IdMap extends HashMap<String, IdMap.IdMapping> {
	public static class IdentifierList {
		IndexSort.CollatingStringComparator collator = IndexSort.caseless;
		IndexSort<String> identifiersSorted = new IndexSort<String>(collator);
		ArrayList<String> identifiersIndexed = new ArrayList<String>();

		public int add(String identifier) {
			int ixIdentifier = identifiersSorted.add(identifier);
			if (ixIdentifier >= identifiersIndexed.size()) {
				identifiersIndexed.add(ixIdentifier, identifier);
			}
			return ixIdentifier;
		}

		public String get(int ixIdentifier) {
			return (ixIdentifier >= 0 && ixIdentifier < identifiersIndexed.size()) ? identifiersIndexed.get(ixIdentifier) : null;
		}
	}

	IdentifierList identifiers = new IdentifierList();
	GpCodeSet gpCodeSet;
	IdMapping uniProtKBMap;

	public class IdMapping extends TreeMap<String, Integer> {
		public String translate(String identifier) {
			return identifiers.get(get(identifier));
		}
	}

	public IdMap(GpCodeSet gpCodeSet) {
		this.gpCodeSet = gpCodeSet;
		uniProtKBMap = getIdMapping("UniProtKB", true);
	}

	IdMapping getIdMapping(String db, boolean isDb) {
		IdMapping map = get(db);
		if (map == null) {
			gpCodeSet.register(db, isDb);
			map = new IdMapping();
			put(db, map);
		}
		return map;
	}

	public void load(File gpFile, boolean isDb) throws Exception {
		String lastDb = "";
		IdMapping map = null;

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
						map = getIdMapping(dbId[0], isDb);
						lastDb = dbId[0];
					}
					int ixAccession = identifiers.add(uniProtAc);
					map.put(dbId[1], ixAccession);
					uniProtKBMap.put(uniProtAc, ixAccession);
					cntMapped++;
				}
			}
		}
		System.out.println(gpFile.getName() + ": total number of mappings: " + cntTotal + "  mappings to UniProt: " + cntMapped);
	}

	public String getVirtualGroupingId(String db, String dbObjectId, boolean mustExist) {
		String vgi;

		IdMapping map = getIdMapping(db, true);
		Integer ixIdentifier = map.get(dbObjectId);
		if (ixIdentifier == null) {
			if (mustExist) {
				vgi = null;
			}
			else {
				UniProtAc uac = UniProtAc.parse(db, dbObjectId);
				if (uac != null) {
					vgi = uac.accession;
					ixIdentifier = identifiers.add(vgi);
					map.put(uac.accession, ixIdentifier);
					uniProtKBMap.put(uac.accession, ixIdentifier);
				}
				else {
					vgi = db + ":" + dbObjectId;
					ixIdentifier = identifiers.add(vgi);
					map.put(dbObjectId, ixIdentifier);
				}
			}
		}
		else {
			vgi = identifiers.get(ixIdentifier);
		}
		return vgi;
	}

	public void dump(String fileName) throws Exception {
		PrintStream ps = (fileName == null) ? System.out : new PrintStream(fileName);

		for (String db  : keySet()) {
			ps.println("*** IDENTIFIER MAPPING FOR DB " + db);
			IdMapping map = get(db);
			for (String id : map.keySet()) {
				ps.println("[" + db + ":" + id + "] => [" + map.translate(id) + "]");
			}
		}

		if (fileName != null) {
			ps.close();
		}
	}

	public static class UniProt2IdentifierMap extends TreeMap<String, ArrayList<String>> {
		public void insert(String uniProtAc, String identifier) {
			ArrayList<String> identifiers = get(uniProtAc);
			if (identifiers == null) {
				identifiers = new ArrayList<String>();
				put(uniProtAc, identifiers);
			}
			identifiers.add(identifier);
		}

		public void dump(String fileName) throws Exception {
			PrintStream ps = (fileName == null) ? System.out : new PrintStream(fileName);

			ps.println("*** UNIPROT2IDENTIFIER MAP");
			for (String uniProtAc  : keySet()) {
				ps.print(uniProtAc + " => ");
				ArrayList<String> identifiers = get(uniProtAc);
				for (String id : identifiers) {
					ps.print("[" + id + "] ");
				}
				ps.println();
			}

			if (fileName != null) {
				ps.close();
			}
		}
	}

	public UniProt2IdentifierMap invert() {
		UniProt2IdentifierMap inverse = new UniProt2IdentifierMap();

		for (String db  : keySet()) {
			IdMapping map = get(db);
			for (String protein : map.keySet()) {
				inverse.insert(map.translate(protein), db + ":" + protein);
			}
		}

		return inverse;
	}
}
