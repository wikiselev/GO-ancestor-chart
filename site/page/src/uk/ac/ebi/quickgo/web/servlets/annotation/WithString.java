package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.IntegerTableReader;
import uk.ac.ebi.interpro.exchange.compress.TextList;
import uk.ac.ebi.interpro.exchange.compress.TextTableReader;
import uk.ac.ebi.quickgo.web.configuration.Configuration;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.data.CV;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WithString {
	public class WithComponent {
		public String db;
		public String dbName;
		public String dbURL;    // generic URL for the db as a whole
		public String id;
		public String idURL;    // specific URL for an individual entry in the db

		public String description;

		int ixDB, ixID;

		public String proteinTaxonomy;
		public String proteinShortTaxonomy;
		public String proteinSymbol;
		public String proteinName;

		public WithComponent(String wc) throws IOException {
			String[] sa = wc.split(":", 2);
			db = sa[0];
			id = sa[1];

			ixDB = dbList.search(db);
			ixID = idList.search(id);

			CV.Item item = df.databases.get(db);
			if (item != null) {
				dbName = item.description;
			}

			dbURL = df.xrfAbbs.getGenericURL(config, db);
			idURL = df.xrfAbbs.getItemURL(config, db, id);

		    description = "";

		    if (db.equals("UniProtKB") && proteinRefCursor != null) {
		        int proteinCode = proteinRefCursor.read(ixID)[0];
		        if (proteinCode > 0) {
					String[] info = proteinInfoCursor.read(proteinCode);
					if (info != null) {
						proteinSymbol = info[0];
						description = info[1];
						proteinName = info[2];
						proteinTaxonomy = taxonomyCursor.read(proteinTaxonomyCursor.read(proteinCode)[0])[0];
						proteinShortTaxonomy = shortTax(proteinTaxonomy);
					}
		        }
		    }
		}
	}

	private Configuration config;
    private DataFiles df;
	private TextList withStringList;
    private TextList dbList;
    private TextList idList;

	private IntegerTableReader.Cursor proteinRefCursor;
	private TextTableReader.Cursor proteinInfoCursor;
	private IntegerTableReader.Cursor proteinTaxonomyCursor;
	private TextTableReader.Cursor taxonomyCursor;

	public String withString;
	public List<WithComponent> components;

	public WithString(Configuration config, DataFiles df, TextList withStringList, TextList dbList, TextList idList) {
	    this.config = config;
        this.df = df;
		this.withStringList = withStringList;
        this.dbList = dbList;
        this.idList = idList;
    }

	public void configureProtein(IntegerTableReader.Cursor proteinRefCursor, TextTableReader.Cursor proteinInfoCursor, IntegerTableReader.Cursor proteinTaxonomyCursor, TextTableReader.Cursor taxonomyCursor) {
	    this.proteinTaxonomyCursor = proteinTaxonomyCursor;
	    this.proteinRefCursor = proteinRefCursor;
	    this.proteinInfoCursor = proteinInfoCursor;
	    this.taxonomyCursor = taxonomyCursor;
	}

	public List<WithComponent> decompose(String s) throws Exception {
		if (s == null || "".equals(s)) {
			return null;
		}
		else {
			List<WithComponent> wcl = new ArrayList<WithComponent>();
			for (String wc : s.split("\\|")) {
				wcl.add(new WithComponent(wc));
			}
			return wcl;
		}
	}

	public void load(int ixString) throws IOException {
		withString = withStringList.read(ixString);
		try {
			components = decompose(withString);
		}
		catch (Exception e) {
			components = null;
		}
	}

	private final static Pattern binomial = Pattern.compile("([^ ])[^ ]* ([^ ]*)");

	public static String shortTax(String full) {
	    Matcher m = binomial.matcher(full);
	    return m.matches() ? (m.group(1) + ". " + m.group(2)) : full;
	}
}
