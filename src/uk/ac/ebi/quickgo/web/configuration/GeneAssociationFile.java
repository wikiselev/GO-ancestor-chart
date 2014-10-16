package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.performance.MemoryMonitor;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import uk.ac.ebi.interpro.common.StringUtils;

public class GeneAssociationFile extends AnnotationFile {
	private static final int nCols = 15;

	// There are (currently) 15 columns in a GAF format file, namely:
	//
	//  1   DB                              required    cardinality: 1              example: SGD
	//  2   DB_Object_ID                    required    cardinality: 1              example: S000000296
	//  3   DB_Object_Symbol                required    cardinality: 1              example: PHO3
	//  4   Qualifier                       optional    cardinality: 0 or greater   example: NOT
	//  5   GO ID                           required    cardinality: 1              example: GO:0003993
	//  6   DB:Reference (|DB:Reference)    required    cardinality: 1 or greater   example: SGD_REF:S000047763|PMID:2676709
	//  7   Evidence code                   required    cardinality: 1              example: IMP
	//  8   With (or) From                  optional    cardinality: 0 or greater   example: GO:0000346
	//  9   Aspect                          required    cardinality: 1              example: F
	// 10   DB_Object_Name                  optional    cardinality: 0 or 1         example: acid phosphatase
	// 11   DB_Object_Synonym (|Synonym)    optional    cardinality: 0 or greater   example: YBR092C
	// 12   DB_Object_Type                  required    cardinality: 1              example: gene
	// 13   taxon(|taxon)                   required    cardinality: 1 or 2         example: taxon:4932
	// 14   Date                            required    cardinality: 1              example: 20010118
	// 15   Assigned_by                     required    cardinality: 1              example: SGD
	//
	// We discard column 9 as its value can be inferred from elsewhere (i.e., the data is not normalised)

	// The quantities that we want are these:-
	//String db;              // col. 1
	//String dbObjectId;      // ) obtained from col. 2
	String splice;          // )
	//String evidence;        // col. 7
	String term;            // col. 5
	//String qualifier;       // col. 4
	String externalDate;    // col. 14
	String taxId;           // the first component of col. 13
	String extraTaxId;      // the second component (if any) of col. 13
	//String reference;       // )
	//String refDBCode;       // > obtained from col. 6
	//String refDBId;         // )
	String withDBCode;      // ) obtained from col. 8
	String withDBId;        // )
	//String assignedBy;      // col. 15
	String dbObjectSymbol;  // col. 3
	String dbObjectName;    // col. 10
	String dbObjectSynonym; // col. 11
	String dbObjectType;    // col. 12
	// plus
	//String virtualGroupingId;

	public GeneAssociationFile(IdMap idMap, NamedFile f) throws Exception {
		super(idMap, f, nCols);
	}

	boolean read() throws Exception {
		while (true) {
			if (reader.read(columns)) {
				//db = data[0];
				db = "UniProt".equalsIgnoreCase(columns[0]) ? "UniProtKB" : columns[0];
				dbObjectId = columns[1];

				virtualGroupingId = getVirtualGroupingId(db, dbObjectId, true);
				if (virtualGroupingId != null) {
					String[] parts = dbObjectId.split("-");
					splice = (parts.length > 1) ? parts[1] : "";

					term = columns[4];
					evidence = columns[6];
					assignedBy = columns[14];

					String tokens[] = getReference(columns[5], "DOI", "PMID");
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

					tokens = getReference(columns[7]);
					if (tokens != null) {
						withDBCode = tokens[0];
						withDBId = (tokens.length > 1) ? tokens[1] : "";
					}
					else {
						withDBCode = "";
						withDBId = "";
					}

					qualifier = StringUtils.nvl(columns[3]);

					TaxonMatcher tm = new TaxonMatcher(columns[12]);
					taxId = tm.taxonId;
					extraTaxId = tm.extraTaxonId;
		
					externalDate = StringUtils.nvl(columns[13]);

					dbObjectSymbol = columns[2];
					dbObjectName = columns[9];
					dbObjectSynonym = columns[10];
					dbObjectType = columns[11];

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
				loader.loadMetadata(virtualGroupingId, dbObjectName, dbObjectSymbol, dbObjectSynonym, taxId, dbObjectType);
				count++;
			}
		}

		System.out.println("Load " + getName() + " done - " + mm.end());
		return count;
	}

	public String toString() {
		return "GeneAssociationFile {" +
				"db='" + db + '\'' +
				", dbObjectId='" + dbObjectId + '\'' +
				", splice='" + splice + '\'' +
				", evidence='" + evidence + '\'' +
				", term='" + term + '\'' +
				", qualifier='" + qualifier + '\'' +
				", externalDate='" + externalDate + '\'' +
				", taxId='" + taxId + '\'' +
				", extraTaxId='" + extraTaxId + '\'' +
				", refDBCode='" + refDBCode + '\'' +
				", refDBId='" + refDBId + '\'' +
				", withDBCode='" + withDBCode + '\'' +
				", withDBId='" + withDBId + '\'' +
				", assignedBy='" + assignedBy + '\'' +
				", dbObjectSymbolIndex='" + dbObjectSymbol + '\'' +
				", dbObjectNameIndex='" + dbObjectName + '\'' +
				", dbObjectSynonym='" + dbObjectSynonym + '\'' +
				", dbObjectTypeIndex='" + dbObjectType + '\'' +
				", virtualGroupingId='" + virtualGroupingId + '\'' +
				'}';
	}
}

