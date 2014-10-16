package uk.ac.ebi.quickgo.web.update;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.quickgo.web.configuration.DataLocation;
import uk.ac.ebi.quickgo.web.configuration.GpCodeSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

public class ProteinXrefs {
	IndexWriter idIndex;
	IndexWriter dbIndex;

	SyncMaster sync;
	int[] proteinXrefs;
	int prev = -1;
	IndexSort.CollatingStringComparator collator = IndexSort.caseless;
	IndexSort<String> ids = new IndexSort<String>(collator);

	IntegerTableWriter idMap;
	TextTableWriter idWriter;
	private Type proteinType;
	private ArrayList<String> gpCodes;

	public ProteinXrefs(Table proteinTable, DataLocation directory) throws Exception {
		this.proteinType = proteinTable.type;

		GpCodeSet gpCodeSet = new GpCodeSet(directory);
		gpCodeSet.load();

		this.gpCodes = gpCodeSet.allCodes();
		int size = gpCodes.size();

		sync = new SyncMaster(proteinTable);
		ids.add("");
		idMap = new IntegerTableWriter(directory.proteinIDMap.file(), size);
		idWriter = new TextTableWriter(directory.proteinIDs.file(), 1);
		dbIndex = new IndexWriter(directory.proteinDBIndex.file());
		idIndex = new IndexWriter(directory.proteinIDIndex.file());
		proteinXrefs = new int[size];
	}

	public void set(String protein, String ac) throws IOException {
		String[] dbId = ac.split(":", 2);
		int gpIndex = gpCodes.indexOf(dbId[0]);
		//System.out.println("pxr.set: ac = " + ac + "  db = " + dbId[0] + "  id = " + dbId[1] + "  gpIndex = " + gpIndex);
		if (gpIndex >= 0) {
			int proteinCode = sync.find(protein);
			if (proteinCode >= 0) {
				//System.out.println("pxr.set - gpIndex: " + gpIndex + " protein: " + protein + " (" + proteinCode + ") ac: " + ac);
				if (proteinCode != prev) {
					//System.out.println("> "+protein+" "+proteinCode);
					write();
					Arrays.fill(proteinXrefs, 0);
					prev = proteinCode;
				}
				proteinXrefs[gpIndex] = ids.add(dbId[1]);
			}
		}
	}

	void write() throws IOException {
		if (prev == -1) return;
		idMap.seek(prev);
		idMap.write(proteinXrefs);
		for (int i = 0; i < proteinXrefs.length; i++) {
			if (proteinXrefs[i] != 0) {
				idIndex.write(prev, proteinXrefs[i]);
				dbIndex.write(prev, i);
			}

		}
	}

	public void compress() throws IOException {
		write();

		int size = gpCodes.size();
		Type dbType = new Type("protein-database", size);

		dbIndex.compress(dbType, proteinType);
		Type idType = new Type("protein-id", ids.size());
		Type[] idTypes = new Type[size];
		int[][] idremap = new int[size][];
		for (int i = 0; i < size; i++) {
			idTypes[i] = idType;
			idremap[i] = ids.indexesTranslate();
		}
		idMap.setValueTranslateMap(idremap);
		idMap.seek(proteinType.cardinality);
		idMap.compress(proteinType, idTypes);
		idIndex.compress(idType, proteinType, ids.sortedIndexes());
		idWriter.compressSortedSingle(idType, ids.sortedValues(), collator.collation);
	}
}
