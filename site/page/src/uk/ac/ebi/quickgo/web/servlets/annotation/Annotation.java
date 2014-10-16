package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.configuration.DbObjectMetadataMap.*;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.interpro.exchange.compress.*;

import java.util.*;
import java.io.*;

public class Annotation {
	private Configuration config;
    private DataFiles dataFiles;

	public String virtualGroupingId;
	
    public String db;
	public String dbDescription;
	public String dbURL;

    public String proteinAc;
	public String proteinURL;

    public Term term;
    public Term originalTerm;
    public String taxId;
    public String pubTitle;
    public String evidence;
    public String date;
    public String source;
    public String qualifier;
	public String qualifierDesc;
    public Reference ref;
    public WithString with;
    public String rowId;
    public String taxName;
    public String extraTaxName;
    public String splice;
    public String synonym;
    public String name;
	public String type;
    public String sequence;
	public String sourceDatabase;
	public String sourceURL;

    public String extraTaxId;
    public String symbol;
    public String tax;
    private IntegerTableReader.Cursor proteinTaxonomyCursor;
    private IntegerTableReader.Cursor idMapCursor;
    private int[] idMapIndex;
    private TextTableReader.Cursor taxonomyCursor;
    private TextTableReader.Cursor proteinInfoCursor;
	private TextTableReader.Cursor proteinMetadataCursor;
    private TextTableReader.Cursor sequenceCursor;
    private TextTableReader.Cursor idCursor;


    public int sequenceLength() {
        return sequence.length();
    }

	public String evidenceDescription() {
		return (evidence != null) ? dataFiles.evidences.get(evidence).description : null;
	}

	public String evidenceDocURL() {
		return (evidence != null) ? "http://www.geneontology.org/GO.evidence.shtml#" + evidence.toLowerCase() : null;
	}

    public Annotation(Configuration config, DataFiles df) {
	    this.config = config;
        this.dataFiles = df;
        ref = new Reference(config, dataFiles, dataFiles.refDBcodeTable, dataFiles.refDBidTable);
        with = new WithString(config, dataFiles, dataFiles.withStringTable, dataFiles.withDBcodeTable, dataFiles.withDBidTable);
    }

    public void configureProteinInfo(IntegerTableReader.Cursor proteinTaxonomyCursor,
                      IntegerTableReader.Cursor idMapCursor, int[] idMapIndex,
                      TextTableReader.Cursor proteinInfoCursor, TextTableReader.Cursor idCursor,
                      TextTableReader.Cursor proteinMetadataCursor) {
        this.proteinTaxonomyCursor = proteinTaxonomyCursor;
        this.idMapCursor = idMapCursor;
        this.idMapIndex = idMapIndex;
        this.idCursor = idCursor;
        this.proteinInfoCursor = proteinInfoCursor;
	    this.proteinMetadataCursor = proteinMetadataCursor;
    }

    public void configureProteinRefInfo(IntegerTableReader.Cursor proteinWithCursor) {
        with.configureProtein(proteinWithCursor, proteinInfoCursor, proteinTaxonomyCursor, taxonomyCursor);
    }

    public void configureTaxonomy(TextTableReader.Cursor taxonomyCursor) {
        this.taxonomyCursor = taxonomyCursor;
    }

    public void configureSequences(TextTableReader.Cursor sequenceCursor) {
        this.sequenceCursor = sequenceCursor;
    }

    public void configurePubmed(IntegerTableReader.Cursor pubmedRefCursor, TextTableReader.Cursor pubmedTitleCursor) {
        ref.configurePubmed(pubmedRefCursor,pubmedTitleCursor);
    }

    public boolean hasPub() {return pubTitle!=null;}
    public boolean hasExtraTax() {return extraTaxId!=null && extraTaxId.length()>0;}
        
    public boolean isProtein(Map<String,String> what) {
        return db!=null && db.equals(what.get("db"));
    }

    public boolean load(AnnotationRow row) throws IOException {
        if (!loadProtein(row)) return false;

	    dbDescription = dataFiles.xrfAbbs.getDatabase(db);

        source = dataFiles.sourceTable.read(row.source);
        evidence = dataFiles.evidenceTable.read(row.evidence);
	    CV.Item q = dataFiles.qualifiers.get(dataFiles.qualifierTable.read(row.qualifier));
	    qualifier = q.code;
	    qualifierDesc = q.description;
        date = dataFiles.dateTable.read(row.externalDate);
        splice = dataFiles.spliceTable.read(row.splice);

        term = dataFiles.ontology.terms[row.term];
        originalTerm=dataFiles.ontology.terms[row.originalTerm];
        rowId = String.valueOf(row.rowNumber);

        ref.load(row.refDb, row.refId);
	    with.load(row.withString);

	    sourceURL = dataFiles.xrfAbbs.getGenericURL(source);
	    sourceDatabase = dataFiles.xrfAbbs.getDatabase(source);

	    dbURL = dataFiles.xrfAbbs.getGenericURL(config, db);
	    proteinURL = dataFiles.xrfAbbs.getItemURL(config, db, proteinAc);

        return true;
    }

    public boolean loadProtein(AnnotationRow row) throws IOException {
	    boolean found = false;

	    int[] idRow = idMapCursor.read(row.virtualGroupingId);
	    if (idRow != null && idRow.length > 0) {
			int idIndex = 0;
			while (idIndex < idMapIndex.length && idRow[idMapIndex[idIndex]] == 0) idIndex++;
		    //System.out.println("idIndex = " + idIndex + " idMapIndex.length = " + idMapIndex.length);
		    if (idIndex < idMapIndex.length) {
				int idCode = idMapIndex[idIndex];
				proteinAc = idCursor.read(idRow[idCode])[0];
				db = dataFiles.proteinDatabaseTable.read(idCode);
			    //System.out.println("proteinAc = " + proteinAc + "  db = " + db + " found = true");
			    found = true;
	        }
	    }
		if (!found) {
		    proteinAc = dataFiles.dbIdTable.use().read(row.dbId)[0];
		    db = dataFiles.dbTable.read(row.db);
			//System.out.println("proteinAc = " + proteinAc + "  db = " + db + " found = false");
		}
	    //System.out.println("loadProtein: db = " + db + "  proteinAc = " + proteinAc);
		DbObjectMetadata metaData = DbObjectMetadataMap.read(proteinMetadataCursor, row.virtualGroupingId);

	    virtualGroupingId = metaData.virtualGroupingId;

	    taxId = metaData.taxonId;
	    if ("".equals(taxId)) {
		    if (proteinTaxonomyCursor != null) {
			    taxId = String.valueOf(proteinTaxonomyCursor.read(row.virtualGroupingId)[0]);
		    }
	    }

	    if (taxonomyCursor != null) {
		    String[] ta = taxonomyCursor.read(Integer.parseInt(taxId));
		    taxName = (ta != null) ? ta[0] : "";
	    }
	    else {
		    taxName = "";
	    }

	    extraTaxId = dataFiles.extraTaxTable.read(row.extraTaxId);
	    if (!("".equals(extraTaxId)) && taxonomyCursor != null) {
			extraTaxName = taxonomyCursor.read(Integer.parseInt(extraTaxId))[0];
	    }

	    if (sequenceCursor != null) {
	        String[] sequenceRow = sequenceCursor.read(row.virtualGroupingId);
	        sequence = sequenceRow == null ? "" : sequenceRow[0];
	    }

	    symbol = metaData.symbol;
	    name = metaData.name;
	    if (("".equals(symbol) || "".equals(name)) && proteinInfoCursor != null) {
	        String[] proteinInfo = proteinInfoCursor.read(row.virtualGroupingId);
		    if (proteinInfo != null && proteinInfo.length > 1) {
	            if ("".equals(symbol)) {
		            symbol = proteinInfo[0];
	            }
	            if ("".equals(name)) {
		            name = proteinInfo[1];
	            }
		    }
	    }

	    type = dataFiles.dbObjectTypeTable.read(Integer.parseInt(metaData.type));
	    synonym = metaData.synonyms;

        return true;
    }
}
