package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.compress.TextTableWriter;
import uk.ac.ebi.interpro.exchange.compress.Table;
import uk.ac.ebi.interpro.exchange.compress.TextTableReader;
import uk.ac.ebi.interpro.exchange.compress.Type;

import java.util.TreeMap;
import java.io.IOException;
import java.io.PrintStream;

public class DbObjectMetadataMap extends TreeMap<String, DbObjectMetadataMap.DbObjectMetadataMapEntry> {
	public static final int ixVirtualGroupingId = 0;
	public static final int ixName = 1;
	public static final int ixSymbol = 2;
	public static final int ixSynonym = 3;
	public static final int ixTaxonId = 4;
	public static final int ixType = 5;

	public static class DbObjectMetadata {
		public String virtualGroupingId;
		public String name;
		public String symbol;
		public String synonyms;
		public String taxonId;
		public String type;

		public DbObjectMetadata() {
		}

		public void set(String virtualGroupingId, String name, String symbol, String synonyms, String taxonId, String type) {
			this.virtualGroupingId = virtualGroupingId;
			this.name = name;
			this.symbol = symbol;
			this.synonyms = synonyms;
			this.taxonId = taxonId;
			this.type = type;
		}

		@Override
		public String toString() {
			return "DbObjectMetadata{" +
					"virtualGroupingId='" + virtualGroupingId + '\'' +
					", name='" + name + '\'' +
					", symbol='" + symbol + '\'' +
					", synonyms='" + synonyms + '\'' +
					", taxonId='" + taxonId + '\'' +
					", type='" + type + '\'' +
					'}';
		}
	}

	static class DbObjectMetadataMapEntry {
		String dbObjectName;
		String dbObjectSymbol;
		String dbObjectSynonym;
		int taxonId = -1;

		int dbObjectTypeIndex = -1;

		public void setDbObjectSymbol(String dbObjectSymbol) {
			if (this.dbObjectSymbol == null) {
				this.dbObjectSymbol = dbObjectSymbol;
			}
		}

		public String getDbObjectSymbol() {
			return dbObjectSymbol;
		}

		public void setDbObjectName(String dbObjectName) {
			if (this.dbObjectName == null) {
				this.dbObjectName = dbObjectName;
			}
		}

		public String getDbObjectName() {
			return dbObjectName;
		}

		public void setDbObjectSynonym(String dbObjectSynonym) {
			if (this.dbObjectSynonym == null) {
				this.dbObjectSynonym = dbObjectSynonym;
			}
		}

		public String getDbObjectSynonym() {
			return dbObjectSynonym;
		}

		public int setDbObjectType(int dbObjectType) {
			if (this.dbObjectTypeIndex < 0) {
				this.dbObjectTypeIndex = dbObjectType;
			}
			return this.dbObjectTypeIndex;
		}

		public int getDbObjectType() {
			return dbObjectTypeIndex;
		}

		public void setTaxonId(String taxonId) {
			if (this.taxonId < 0) {
				this.taxonId = Integer.parseInt(taxonId);
			}
		}

		public int getTaxonId() {
			return taxonId;
		}

		@Override
		public String toString() {
			return "DbObjectMetadataMapEntry { Symbol: " + dbObjectSymbol + " Name: " + dbObjectName + " Type: " + dbObjectTypeIndex + " TaxonId: " + taxonId + " Synonym(s): " + dbObjectSynonym + " }";
		}
	}

	public DbObjectMetadataMapEntry getMetadata(String virtualGroupingId) {
		DbObjectMetadataMapEntry m = get(virtualGroupingId);
		if (m == null) {
			m = new DbObjectMetadataMapEntry();
			put(virtualGroupingId, m);
		}
		return m;
	}

	public int set(String virtualGroupingId, String dbObjectName, String dbObjectSymbol, String dbObjectSynonym, String taxId, int dbObjectType) {
		DbObjectMetadataMapEntry metadata = getMetadata(virtualGroupingId);
		metadata.setDbObjectSymbol(dbObjectSymbol);
		metadata.setDbObjectName(dbObjectName);
		metadata.setDbObjectSynonym(dbObjectSynonym);
		metadata.setTaxonId(taxId);
		return metadata.setDbObjectType(dbObjectType);
	}

	public void clear(String virtualGroupingId) {
		put(virtualGroupingId, null);
	}

	public void write(DataLocation directory) throws IOException {
		TextTableWriter metadataWriter = new TextTableWriter(directory.proteinMetadata.file(), 6);
		Table proteinTable = new TextTableReader(directory.virtualGroupingIds.file()).extractColumn(0);

		for (int i = 0; i < proteinTable.size(); i++) {
			String virtualGroupingId = proteinTable.read(i);
			DbObjectMetadataMapEntry m = get(virtualGroupingId);
			if (m != null) {
				metadataWriter.write(virtualGroupingId, m.getDbObjectName(), m.getDbObjectSymbol(), m.getDbObjectSynonym(), String.valueOf(m.getTaxonId()), String.valueOf(m.getDbObjectType()));
			}
			else {
				metadataWriter.write((String[])null);
			}
		}
		metadataWriter.compress(new Type("protein", proteinTable.size()));
	}

	private static DbObjectMetadata objectMetadata = new DbObjectMetadata();
	private static int lastVGI = -1;

	public static DbObjectMetadata read(TextTableReader.Cursor metadataCursor, int virtualGroupingId) throws IOException {
		if (virtualGroupingId != lastVGI) {
			String[] metaData = metadataCursor.read(virtualGroupingId);
			objectMetadata.set(metaData[ixVirtualGroupingId], metaData[ixName], metaData[ixSymbol], metaData[ixSynonym], metaData[ixTaxonId], metaData[ixType]);
			lastVGI = virtualGroupingId;
		}
		return objectMetadata;
	}

	public void dump(String fileName) throws Exception {
		PrintStream ps = (fileName == null) ? System.out : new PrintStream(fileName);

		for (String vgi : keySet()) {
			DbObjectMetadataMapEntry m = get(vgi);
			ps.println("[" + vgi + "] => " + m);
		}

		if (fileName != null) {
			ps.close();
		}
	}
}
