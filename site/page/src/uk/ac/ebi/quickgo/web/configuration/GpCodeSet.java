package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.TSVRowWriter;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import java.util.ArrayList;
import java.util.Collections;

public class GpCodeSet {
	static class GpCode implements Comparable<GpCode> {
		public String code;
		public boolean isDb;

		GpCode(String code, boolean isDb) {
			this.code = code;
			this.isDb = isDb;
		}

		public int compareTo(GpCode other) {
			return this.code.compareToIgnoreCase(other.code);
		}
	}

	private DataLocation directory;
	private ArrayList<GpCode> codeSet = new ArrayList<GpCode>();
	private String lastCodeRegistered = "";

	public GpCodeSet(DataLocation directory) {
		this.directory = directory;
	}

	public void write() throws Exception {
		TSVRowWriter writer = new TSVRowWriter(directory.gp2proteinDb.file(), new String[] { "CODE", "IS_DB" }, true, true, null);
		writer.open();
		String[] data = new String[2];

		Collections.sort(codeSet);
		
		// hack to ensure that UniProt always comes first...
		for (GpCode gpCode : codeSet) {
			if (gpCode.code.startsWith("UniProtKB")) {
				data[0] = gpCode.code;
				data[1] = gpCode.isDb ? "Y" : "N";
				writer.write(data);
			}
		}
		for (GpCode gpCode : codeSet) {
			if (!gpCode.code.startsWith("UniProtKB")) {
				data[0] = gpCode.code;
				data[1] = gpCode.isDb ? "Y" : "N";
				writer.write(data);
			}
		}
		writer.close();
	}

	public void load() throws Exception {
		for (String[] r : directory.gp2proteinDb.reader(GP2ProteinDB.CODE, GP2ProteinDB.IS_DB)) {
			register(r[0], "Y".equals(r[1]));
		}
	}

	public void register(String code, boolean isDb) {
		if (!lastCodeRegistered.equals(code)) {
			for (GpCode gpCode : codeSet) {
				if (gpCode.code.equals(code)) {
					return;
				}
			}
			codeSet.add(new GpCode(code, isDb));
			lastCodeRegistered = code;
		}
	}

	public ArrayList<String> dbCodes() {
		ArrayList<String> codes = new ArrayList<String>();
		for (GpCode gpCode : codeSet) {
			if (gpCode.isDb) {
				codes.add(gpCode.code);
			}
		}
		return codes;
	}

	public ArrayList<String> allCodes() {
		ArrayList<String> codes = new ArrayList<String>();
		for (GpCode gpCode : codeSet) {
			codes.add(gpCode.code);
		}
		return codes;
	}

	public int indexOf(String code) {
		for (int ix = 0; ix < codeSet.size(); ix++) {
			GpCode gpCode = codeSet.get(ix);
			if (gpCode.code.equals(code)) {
				return ix;
			}
		}
		return -1;
	}
}
