package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;
import uk.ac.ebi.quickgo.web.data.RelationType;

public class FieldFilter implements Filter {
    enum FieldName { ancestor, term, evidence, source, ref, with, tax, protein, qualifier, db, aspect }

    final FieldName field;
    final String value;


    public FieldFilter(FieldName field, String value) {
        this.field = field;
        this.value = value;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldFilter that = (FieldFilter) o;

        if (field != that.field) return false;
        return value.equals(that.value);

    }

    public int hashCode() {
        int result;
        result = field==null?0:field.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    public String toString() {
        if (field==FieldName.ancestor && value.substring(1).startsWith("GO:"))
            return RelationType.byCode(value.substring(0,1)).name().toLowerCase()+"="+value.substring(1);
        return (field==null?"":field.name()+"=")+value;
    }

    public Find open(DataFiles df, List<Closeable> connection) throws IOException {
        if (field==null) {
            for (FieldName f : FieldName.values()) {
                Find test=searchField(df, connection, f);
                if (!(test instanceof None)) return test;
            }
            throw new IOException(value+" is not found");
        }
        return searchField(df, connection, field);
    }

    private Find searchField(DataFiles df, List<Closeable> connection, FieldName f) throws IOException {
	    Find find;

        switch (f) {        
        case ancestor:
	        find = indexRead(df.ancestorRelationTable, df.ancestorRelationIndex, connection);
	        break;
        case evidence:
	        find = indexRead(df.evidenceTable, df.evidenceIndex, connection);
	        break;
        case ref:
	        find = refRead(df.refDBcodeTable, df.refDBIndex, df.refDBidTable, df.refIdIndex, connection);
	        break;
        case source:
	        find = indexRead(df.sourceTable, df.sourceIndex, connection);
	        break;
        case term:
	        find = indexRead(df.termIDs, df.termIndex, connection);
	        break;
        case with:
	        find = refRead(df.withDBcodeTable, df.withDBIndex, df.withDBidTable, df.withIdIndex, connection);
	        break;
        case db:
	        find = new RepeatedKeyFilter(df.proteinAnnotationCounts, null, null, indexFind(connection, df.proteinDatabaseTable.search(value), df.proteinDBIndex));
	        break;
        case tax:
	        find = new RepeatedKeyFilter(df.proteinAnnotationCounts, null, null, taxFilter(df, connection));
	        break;
        case protein:
	        find = new RepeatedKeyFilter(df.proteinAnnotationCounts, null, null, proteinFilter(df, connection));
	        break;
        case qualifier:
	        find = indexRead(df.qualifierTable, df.qualifierIndex, connection);
	        break;
		case aspect:
			find = indexRead(df.aspectTable, df.aspectIndex, connection);
			break;
        default:
	        find = new None();
	        break;
        }

	    return find;
    }

    private Find proteinFilter(DataFiles df,List<Closeable> connection) throws IOException {
        int databaseCode=df.proteinDatabaseTable.search(value);
        if (databaseCode>=0) return indexFind(connection, databaseCode,df.proteinDBIndex);
        return indexFind(connection, df.proteinIDs.use().search(new String[]{value}), df.proteinIDIndex);
    }

    private Find indexFind(List<Closeable> connection, int code, IndexReader index) throws IOException {
        if (code<0) return new None();
        return index.open(connection, code);
    }

    public Find refRead(Table codeTable,IndexReader codeIndex,Table idTable,IndexReader idIndex,List<Closeable> connection) throws IOException {
	    String db = null;
	    String id = value;

        int p = value.indexOf(":");
        if (p > 0) {
            db = value.substring(0, p);
            //if (!db.equals("GO")) id=value.substring(p+1);
            id = value.substring(p + 1);
        }

        Find dbFind = (db == null) ? null : indexRead(codeTable, codeIndex, db, connection);
        id = id.trim();
        return ("*".equals(id) || id.length() == 0) ? dbFind : And.and(dbFind, indexRead(idTable, idIndex, id, connection));
    }

    public Find indexRead(Table table,IndexReader index,List<Closeable> connection) throws IOException {
        return indexRead(table, index, value,connection);
    }

    public Find indexRead(Table table,IndexReader index,String value,List<Closeable> connection) throws IOException {
        return indexFind(connection,table.search(value),index);
    }

    private Find taxFilter(DataFiles df,List<Closeable> connection) throws IOException {


        int leftNumber;
        int rightNumber;


        int id;

        try {
            id= Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return new None();
        }


        leftNumber= df.treeLeft[id];
        rightNumber=df.treeRight[id];

        BitmaskFilter filter;
        filter = new BitmaskFilter(df.proteinInfo.size());
        IntegerTableReader.Cursor cursor=df.proteinTaxonomy.use();
        int i=0;
        int[] row;
        while ((row = cursor.read())!=null) {
            int tax= row[0];

            if (tax>=df.treeLeft.length) continue;
            int n = df.treeLeft[tax];
            if ((leftNumber <= n) && (n <= rightNumber)) filter.add(i);
            i++;
        }
        return new BitmapFind(filter);

    }
}
