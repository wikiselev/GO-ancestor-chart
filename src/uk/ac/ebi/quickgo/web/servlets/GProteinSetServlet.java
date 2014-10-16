package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.exchange.compress.IndexReader;
import uk.ac.ebi.interpro.exchange.compress.TextTableReader;
import uk.ac.ebi.quickgo.web.Request;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.data.CVExt;

import java.io.Closeable;
import java.util.*;

public class GProteinSetServlet implements Dispatchable {
	public static class ProteinSetEntry implements Comparable<ProteinSetEntry> {
		public String accession;
		public String name;
		public String symbol;
		public int taxon;
		public String taxName;

		public ProteinSetEntry(String accession, String name, String symbol, int taxon, String taxName) {
			this.accession = accession;
			this.name = name;
			this.symbol = symbol;
			this.taxon = taxon;
			this.taxName = taxName.intern();
		}

		public int compareTo(ProteinSetEntry other) {
			return symbol.compareToIgnoreCase(other.symbol);
		}
	}

	public static class ProteinSetEntryGroup implements Comparable<ProteinSetEntryGroup> {
		public String name;
		List<ProteinSetEntry> contents = new ArrayList<ProteinSetEntry>();

		public ProteinSetEntryGroup(String name) {
			this.name = name;
		}

		public void add(ProteinSetEntry entry) {
			contents.add(entry);
		}

		public List<ProteinSetEntry> proteinList() {
			Collections.sort(contents);
			return contents;
		}

		public int compareTo(ProteinSetEntryGroup other) {
			return name.compareTo(other.name);
		}
	}

	public static class ProteinSet {
		Map<String, ProteinSetEntryGroup> groups = new HashMap<String, ProteinSetEntryGroup>();
		public String name;
		public String description;
		public String url;

		public ProteinSet(String name, String description, String url) {
			this.name = name;
			this.description = description;
			this.url = url;
		}

		public void add(String accession, String name, String symbol, int taxon, String taxName) {
			char c = (symbol != null && !"".equals(symbol)) ? Character.toUpperCase(symbol.charAt(0)) : '?';

			String groupName = Character.isLetter(c) ? String.valueOf(c) : "[other]";
			ProteinSetEntryGroup group = groups.get(groupName);
			if (group == null) {
				group = new ProteinSetEntryGroup(groupName);
				groups.put(groupName, group);
			}

			group.add(new ProteinSetEntry(accession, name, symbol, taxon, taxName));
		}

		public List<ProteinSetEntryGroup> groupList() {
			List<ProteinSetEntryGroup> gl = new ArrayList<ProteinSetEntryGroup>();

			for (String key : groups.keySet()) {
				gl.add(groups.get(key));
			}

			Collections.sort(gl);
			return gl;
		}
	}

	public void process(Request r) throws Exception {
	    DataFiles files = r.getDataFiles();
	    if (files == null) {
		    return;
	    }

	    String id = r.getParameter("id");
		if (id != null && !"".equals(id)) {
			CVExt.Item item = files.proteinIDSets.get(id);
			if (item != null) {
				int ixSet = files.gpCodeSet.indexOf(id);
				if (ixSet >= 0) {
					ProteinSet proteinSet = new ProteinSet(id, item.description, item.extra);

					List<Closeable> connection = new ArrayList<Closeable>();

					TextTableReader.Cursor metadataCursor = files.proteinMetadata.open(connection);
					TextTableReader.Cursor taxonomyCursor = files.taxonomy.open(connection);


					IndexReader rd = files.proteinDBIndex;
					IndexReader.ValueRead vr = rd.open(connection, ixSet);
					int at = 0;
					while ((at = vr.next(at)) != Integer.MAX_VALUE) {
						// proteinMetadata has the following columns: virtualGroupingId, db_object_name, db_object_symbol, db_object_synonym, taxon, db_object_type
						String[] metadata = metadataCursor.read(at);
						int taxId = Integer.parseInt(metadata[4]);
						String[] taxName = taxonomyCursor.read(taxId);
						proteinSet.add(metadata[0], metadata[1], metadata[2], taxId, taxName[0]);
					    at++;
					}

					r.write(r.outputHTML(true, "page/GProteinSet.xhtml").render(proteinSet));
				}
			}
		}
	}
}
