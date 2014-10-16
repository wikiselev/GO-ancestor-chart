package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.interpro.common.performance.MemoryMonitor;

public class GPAssociationFile extends AnnotationFile {
	private static final int nCols = 12;

	// There are (currently) 12 columns in a gp_association format file, namely:
	//
	//    1  DB                    required  cardinality: 1
	//    2  DB_Object_ID          required  cardinality: 1
	//    3  Qualifier             optional  cardinality: 0 or greater
	//    4  GO ID                 required  cardinality: 1
	//    5  DB:Reference(s)       required  cardinality: 1 or greater
	//    6  Evidence code         required  cardinality: 1
	//    7  With                  optional  cardinality: 0 or greater
	//    8  Extra taxon ID        optional  cardinality: 0 or 1
	//    9  Date                  required  cardinality: 1
	//   10  Assigned_by           required  cardinality: 1
	//   11  Annotation Extension  optional  cardinality: 0 or greater
	//   12  Spliceform ID         optional  cardinality: 0 or 1

	private static final int COLUMN_DB = 0;
	private static final int COLUMN_DB_OBJECT_ID = 1;
	private static final int COLUMN_QUALIFIER = 2;
	private static final int COLUMN_GO_ID = 3;
	private static final int COLUMN_REFERENCE = 4;
	private static final int COLUMN_EVIDENCE = 5;
	private static final int COLUMN_WITH = 6;
	private static final int COLUMN_EXTRA_TAXID = 7;
	private static final int COLUMN_DATE = 8;
	private static final int COLUMN_ASSIGNED_BY = 9;
	private static final int COLUMN_SPLICEFORM = 11;


	public GPAssociationFile(IdMap idMap, DataLocation.NamedFile f) throws Exception {
		super(idMap, f, nCols);
	}

	boolean read() throws Exception {
		while (true) {
			if (reader.read(columns)) {
				db = columns[COLUMN_DB];
				dbObjectId = columns[COLUMN_DB_OBJECT_ID];

				virtualGroupingId = getVirtualGroupingId(db, dbObjectId, true);
				if (virtualGroupingId != null) {
					spliceformId = columns[COLUMN_SPLICEFORM];

					goId = columns[COLUMN_GO_ID];
					evidence = columns[COLUMN_EVIDENCE];
					assignedBy = columns[COLUMN_ASSIGNED_BY];

					String tokens[] = getReference(columns[COLUMN_REFERENCE], "DOI", "PMID");
					if (tokens != null) {
						refDBCode = tokens[0];
						refDBId = (tokens.length > 1) ? tokens[1] : "";
						reference = refDBCode + ":" + refDBId;
					}
					else {
						refDBCode = "";
						refDBId = "";
						reference = "";
					}

					withString = columns[COLUMN_WITH];

					qualifier = StringUtils.nvl(columns[COLUMN_QUALIFIER]);

					extraTaxId = new TaxonMatcher(columns[COLUMN_EXTRA_TAXID]).taxonId;

					date = StringUtils.nvl(columns[COLUMN_DATE]);
					return true;
				}
			}
			else {
				return false;
			}
		}
	}

	public int load(AnnotationLoader loader) throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);
		System.out.println("\nLoad " + getName());

		int count = 0;

		while (read()) {
			if (loader.loadAnnotation(this)) {
				count++;
			}
		}

		System.out.println("Load " + getName() + " done - " + mm.end());
		return count;
	}

	public String toString() {
		return "GPAssociationFile{" +
				"db='" + db + '\'' +
				", dbObjectId='" + dbObjectId + '\'' +
				", qualifier='" + qualifier + '\'' +
				", goId='" + goId + '\'' +
				", refDBCode='" + refDBCode + '\'' +
				", refDBId='" + refDBId + '\'' +
				", evidence='" + evidence + '\'' +
				", withString='" + withString + '\'' +
				", extraTaxId='" + extraTaxId + '\'' +
				", date='" + date + '\'' +
				", assignedBy='" + assignedBy + '\'' +
				", spliceformId='" + spliceformId + '\'' +
				", virtualGroupingId='" + virtualGroupingId + '\'' +
				'}';
	}
}
