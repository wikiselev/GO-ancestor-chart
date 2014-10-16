package uk.ac.ebi.quickgo.web.servlets.annotation;

public class AnnotationRow {
	// an enum would have been better, but we need to use these as array indices
	public static final int COLUMN_COUNT = 16;
	public static final int VIRTUAL_GROUPING_ID = 0;
	public static final int TERM = 1;
	public static final int EVIDENCE = 2;
	public static final int SOURCE = 3;
	public static final int REFERENCE = 4;
	public static final int REF_DB = 5;
	public static final int REF_ID = 6;
	public static final int WITH_STRING = 7;
	public static final int QUALIFIER = 8;
	public static final int EXTRA_TAXID = 9;
	public static final int EXTERNAL_DATE = 10;
	public static final int SPLICE = 11;
	public static final int DB = 12;
	public static final int DB_ID = 13;
	public static final int ONTOLOGY = 14;

    int count = 0;
    int virtualGroupingId;
    int splice;
    int qualifier;
    int term;
    int originalTerm;
    int evidence;
	int reference;
    int refDb;
    int refId;
    int withString;
    int extraTaxId;
    int externalDate;
    int source;
	int db;
	int dbId;
	int aspect;
	int rowNumber;
}
