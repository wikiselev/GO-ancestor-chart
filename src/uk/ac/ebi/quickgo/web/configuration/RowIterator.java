package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.*;

import java.util.*;

public class RowIterator implements Iterable<String[]>,RowReader {
    private final RowReader rr;
    private final String[] row;

    String[] columns;

    public RowIterator(RowReader rr) throws Exception {
        this.rr = rr;
        rr.open();

        columns = rr.getColumns();
        row = new String[columns.length];
    }

    public Iterator<String[]> iterator() {
        return new Iterator<String[]>() {
            boolean haveRow =false;
            boolean eof=false;
            public boolean hasNext() {
                try {
                    if (eof) return false;
                    if (haveRow) return true;
                    haveRow = rr.read(row);
                    if (!haveRow) {rr.close();eof=true;}
                    return haveRow;
                } catch (Exception e) {throw new RuntimeException("Load failure", e);}
            }

            public String[] next() {
                if (!hasNext()) throw new NoSuchElementException();
                haveRow=false;
                return row;
            }
            public void remove() {throw new UnsupportedOperationException();}
        };
    }


    public boolean read(String[] data) throws Exception {return rr.read(data);}

    public void open() throws Exception {}

    public void close() throws Exception {rr.close();}

    public String[] getColumns() throws Exception {return columns;}
}
