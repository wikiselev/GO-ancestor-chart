package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.performance.MemoryMonitor;

public class GPInformationFile extends GPDataFile {
	private static final int nCols = 11;

	// There are (currently) 11 columns in a gp_information format file, namely:
	//
	//    1  DB                     required  cardinality: 1             example: UniProtKB
	//    2  DB_Subset              optional  cardinality: 0 or 1        example: Swiss-Prot or TrEMBL
	//    3  DB_Object_ID           required  cardinality: 1             example: Q4VCS5
	//    4  DB_Object_Symbol       required  cardinality: 1             example: AMOT
	//    5  DB_Object_Name         optional  cardinality: 0 or 1        example: Angiomotin
	//    6  DB_Object_Synonym(s)   optional  cardinality: 0 or greater  example: AMOT|KIAA1071|IPI:IPI00163085|IPI:IPI00644547|UniProtKB:AMOT_HUMAN
	//    7  DB_Object_Type         required  cardinality: 1             example: protein
	//    8  Taxon                  required  cardinality: 1             example: taxon:9606
	//    9  Annotation_Target_Set  optional  cardinality: 0 or greater  example: BHF-UCL|KRUK|Reference Genome
	//   10  Annotation_Completed   optional  cardinality: 1             example: timestamp (YYYYMMDD)
	//   11  Parent_Object_ID       optional  cardinality: 0 or 1        example: UniProtKB:P21677

	private static final int COLUMN_DB = 0;
	String db;
	private static final int COLUMN_DB_SUBSET = 1;
	String dbSubset;
	private static final int COLUMN_DB_OBJECT_ID = 2;
	String dbObjectId;
	private static final int COLUMN_DB_OBJECT_SYMBOL = 3;
	String dbObjectSymbol;
	private static final int COLUMN_DB_OBJECT_NAME = 4;
	String dbObjectName;
	private static final int COLUMN_DB_OBJECT_SYNONYM = 5;
	String dbObjectSynonym;
	private static final int COLUMN_DB_OBJECT_TYPE = 6;
	String dbObjectType;
	private static final int COLUMN_TAXON = 7;
	String taxon;
	private static final int COLUMN_ANNOTATION_TARGET = 8;
	String annotationTarget;
	private static final int COLUMN_ANNOTATION_COMPLETE = 9;
	String annotationComplete;
	private static final int COLUMN_PARENT = 10;
	String parent;

	String virtualGroupingId;


	public GPInformationFile(IdMap idMap, DataLocation.NamedFile f) throws Exception {
		super(idMap, f, nCols);
	}

	boolean read() throws Exception {
	    if (reader.read(columns)) {
		    db = columns[COLUMN_DB];
			dbSubset = columns[COLUMN_DB_SUBSET];
		    dbObjectId = columns[COLUMN_DB_OBJECT_ID];

		    virtualGroupingId = getVirtualGroupingId(db, dbObjectId, false);

		    dbObjectSymbol = columns[COLUMN_DB_OBJECT_SYMBOL];
		    dbObjectName = columns[COLUMN_DB_OBJECT_NAME];
		    dbObjectSynonym = columns[COLUMN_DB_OBJECT_SYNONYM];
		    dbObjectType = columns[COLUMN_DB_OBJECT_TYPE];

		    taxon = new TaxonMatcher(columns[COLUMN_TAXON]).taxonId;
		    annotationTarget = columns[COLUMN_ANNOTATION_TARGET];
		    annotationComplete = columns[COLUMN_ANNOTATION_COMPLETE];
		    parent = columns[COLUMN_PARENT];

			return true;
	    }
		else {
		    return false;
	    }
	}

	public void load(AnnotationLoader loader) throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);
		System.out.println("\nLoad " + getName());

		while (read()) {
			loader.loadMetadata(virtualGroupingId, dbObjectName, dbObjectSymbol, dbObjectSynonym, taxon, dbObjectType);
		}

		System.out.println("Load " + getName() + " done - " + mm.end());
	}

	public String toString() {
		return "GPInformationFile{" +
				"db='" + db + '\'' +
				", dbSubset='" + dbSubset + '\'' +
				", dbObjectId='" + dbObjectId + '\'' +
				", dbObjectSymbol='" + dbObjectSymbol + '\'' +
				", dbObjectName='" + dbObjectName + '\'' +
				", dbObjectSynonym='" + dbObjectSynonym + '\'' +
				", dbObjectType='" + dbObjectType + '\'' +
				", taxon='" + taxon + '\'' +
				", annotationTarget='" + annotationTarget + '\'' +
				", annotationComplete='" + annotationComplete + '\'' +
				", parent='" + parent + '\'' +
				", virtualGroupingId='" + virtualGroupingId + '\'' +
				'}';
	}
}
