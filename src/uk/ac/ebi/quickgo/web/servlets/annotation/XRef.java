package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.interpro.exchange.compress.*;

import java.util.regex.*;
import java.io.*;

public class XRef {
    public String db;
    public String dbName;
	public String dbURL;    // generic URL for the db as a whole
    public String id;
	public String idURL;    // specific URL for an individual entry in the db
    public String text;
    public String description;
    public String proteinTaxonomy;
	public String proteinShortTaxonomy;
    public String proteinSymbol;
	public String proteinName;
	public String displayString;

	private Configuration config;
    private DataFiles df;
    private TextList dbList;
    private TextList idList;

    private IntegerTableReader.Cursor proteinRefCursor;
    private TextTableReader.Cursor proteinInfoCursor;
    private IntegerTableReader.Cursor proteinTaxonomyCursor;
    private TextTableReader.Cursor taxonomyCursor;

    private IntegerTableReader.Cursor pubmedRefCursor;
    private TextTableReader.Cursor pubmedTitleCursor;


	public XRef(Configuration config, DataFiles df, TextList dbList, TextList idList) {
	    this.config = config;
        this.df = df;
        this.dbList = dbList;
        this.idList = idList;
    }

    public void configurePubmed(IntegerTableReader.Cursor pubmedRefCursor, TextTableReader.Cursor pubmedTitleCursor) {
        this.pubmedRefCursor = pubmedRefCursor;
        this.pubmedTitleCursor = pubmedTitleCursor;
    }

    public void configureProtein(IntegerTableReader.Cursor proteinRefCursor, TextTableReader.Cursor proteinInfoCursor, IntegerTableReader.Cursor proteinTaxonomyCursor, TextTableReader.Cursor taxonomyCursor) {
        this.proteinTaxonomyCursor = proteinTaxonomyCursor;
        this.proteinRefCursor = proteinRefCursor;
        this.proteinInfoCursor = proteinInfoCursor;
        this.taxonomyCursor = taxonomyCursor;
    }

    public boolean isPubmed() {
        return this.db.equals("PUBMED") || this.db.equals("PMID");
    }

    public void load(int db,int id) throws IOException {
        this.db = dbList.read(db);
        this.id = idList.read(id);

	    StringBuilder sb = new StringBuilder("");
	    if (this.db != null && this.db.length() > 0) {
		    sb.append(this.db);
	    }
	    if (this.id != null && this.id.length() > 0) {
		    if (sb.length() > 0) {
			    sb.append(':');
		    }
		    sb.append(this.id);
	    }
	    this.displayString = sb.toString();

        CV.Item item = df.databases.get(this.db);
        if (item!=null) dbName= item.description;

	    dbURL = df.xrfAbbs.getGenericURL(this.config, this.db);
	    idURL = df.xrfAbbs.getItemURL(this.config, this.db, this.id);
	    //System.out.println("XRef.load: db = " + this.db + "  id = " + this.id + "  dbURL = " + dbURL + "  idURL = " + idURL);

        //text = this.db + ":" + this.id;
	    boolean dbEmpty = ("".equals(this.db));
	    boolean idEmpty = ("".equals(this.id));
	    text = (dbEmpty) ? (idEmpty ? "" : this.id) : (idEmpty ? "" : this.db + ":" + this.id); 
        description = "";

        if (this.db.equals("UniProtKB") && proteinRefCursor != null) {
            int proteinCode = proteinRefCursor.read(id)[0];
            if (proteinCode <= 0) {
	            return;
            }

            String[] info = proteinInfoCursor.read(proteinCode);
            if (info == null) {
	            return;
            }
	        proteinSymbol = info[0];
            description = info[1];
	        proteinName = info[2];
	        int tax = proteinTaxonomyCursor.read(proteinCode)[0];
		    proteinTaxonomy = taxonomyCursor.read(tax)[0];
	        proteinShortTaxonomy = shortTax(proteinTaxonomy);
	        if (!"".equals(proteinName)) {
		        text = proteinSymbol + " (" + proteinName + ")";
	        }
        }
        else if (isPubmed()) {
	        text = "PMID:" + this.id;
	        if (pubmedRefCursor != null) {
				int[] refRow = pubmedRefCursor.read(id);
				if (refRow == null) {
					description="-";
					return;
				}
				String[] titleRow = pubmedTitleCursor.read(refRow[0]);
				if (titleRow == null) {
					description = "=";
					return;
				}
				description = titleRow[1];
	        }
        }
        else if (this.db.equals("GO_REF")) {
            CV.Item goRef = df.goRefs.get(this.id);
            if (goRef != null) {
	            text = goRef.description;
            }
	        else {
	            text = "GO_REF:" + this.id;
            }
        }
    }

    private final static Pattern binomial = Pattern.compile("([^ ])[^ ]* ([^ ]*)");

    public static String shortTax(String full) {
        Matcher m = binomial.matcher(full);
        return m.matches() ? (m.group(1) + ". " + m.group(2)) : full;
    }
}
