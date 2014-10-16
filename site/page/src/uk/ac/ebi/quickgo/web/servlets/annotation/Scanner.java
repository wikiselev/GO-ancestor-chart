package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;
import java.util.*;

public class Scanner {

    private static Location me = new Location();

    Find finder;
    DataFiles dataFiles;
    private List<Closeable> connection;
    IntegerTableReader.Cursor annotations;

    boolean proteinGroup;

    public Scanner(DataFiles dataFiles, List<Closeable> connection,boolean proteinGroup) throws Exception {
        this.proteinGroup = proteinGroup;
        this.dataFiles = dataFiles;
        this.connection = connection;

        annotations = dataFiles.annotations.open(connection);
    }

    void scan(AnnotationQuery query, DataAction result) throws Exception {
        Action scanAction = me.start("Scanning");

        finder = query.filter.open(dataFiles, connection);

        Action seekAction = me.start("seek");

        AnnotationRow dataRow = new AnnotationRow();
        int[] row = new int[annotations.columnCount()];

        //Progress p = new Progress(goa.dk,"Index scan");
        if (finder == null) finder = new All(dataFiles.annotations.rowCount);

        int rownumber = 0;
        while (true) {
	        //System.out.println("Scanner.scan: rownumber = " + rownumber);
            rownumber = finder.next(rownumber);
            if (!annotations.seek(rownumber)) {
	            break;
            }

            //System.out.println("Row: "+rowNumber+" "+goa.dk.data.bitCount());
            annotations.read(row);

            if (proteinGroup) {
                if (dataRow.count!=0 && dataRow.virtualGroupingId != row[AnnotationRow.VIRTUAL_GROUPING_ID]) {
                    if (!result.act(dataRow)) {
	                    //System.out.println("result.act failed for " + row[AnnotationRow.VIRTUAL_GROUPING_ID]);
	                    break;
                    }
                    dataRow.count=0;
                }
            }

            dataRow.virtualGroupingId = row[AnnotationRow.VIRTUAL_GROUPING_ID];
	        dataRow.db = row[AnnotationRow.DB];
	        dataRow.dbId = row[AnnotationRow.DB_ID];
	        dataRow.term = row[AnnotationRow.TERM];
            dataRow.originalTerm = row[AnnotationRow.TERM];
			dataRow.aspect = row[AnnotationRow.ONTOLOGY];

            dataRow.count++;

            if (!proteinGroup) {
                dataRow.evidence = row[AnnotationRow.EVIDENCE];
                dataRow.source = row[AnnotationRow.SOURCE];
	            dataRow.reference = row[AnnotationRow.REFERENCE];
                dataRow.refDb = row[AnnotationRow.REF_DB];
                dataRow.refId = row[AnnotationRow.REF_ID];
                dataRow.withString = row[AnnotationRow.WITH_STRING];
                dataRow.qualifier = row[AnnotationRow.QUALIFIER];
                dataRow.extraTaxId = row[AnnotationRow.EXTRA_TAXID];
                dataRow.externalDate = row[AnnotationRow.EXTERNAL_DATE];
                dataRow.splice = row[AnnotationRow.SPLICE];
                dataRow.rowNumber = rownumber;

                if (!result.act(dataRow)) {
	                break;
                }
            }

            rownumber++;
        }

        if (proteinGroup && dataRow.count != 0) {
	        result.act(dataRow);
        }

        me.stop(seekAction);
        me.stop(scanAction);
    }

    public Find getFinder() {
        return finder;
    }
}
