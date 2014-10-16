package uk.ac.ebi.quickgo.web.update;

import uk.ac.ebi.interpro.exchange.compress.*;

public class SyncMaster /*implements Column, Output<String>*/ {
    //TableSorter sorter;
    Type type;
    Table table;
    int rownumber;    

    public SyncMaster(Table table) {
        this.table = table;
        this.type=table.type;
    }

    public int find(String s) {
        while (rownumber<table.values.length && table.values[rownumber].compareTo(s)<0) rownumber++;
        if (rownumber<table.values.length && table.values[rownumber].compareTo(s)==0) return rownumber;
        return -1;
    }

    public void reset() {
        rownumber=0;
    }

    /*Column slave(final Column c) {
        return new Column() {
            public Output<String> next(int stage) throws IOException {
                Output<String> underlying = c.next(stage);
                if (underlying==null) return null;
                return new SyncSlave(SyncMaster.this, underlying);
            }

            public int stages() {
                return c.stages();
            }
        };
    }

    public Output<String> next(int stage) throws IOException {
        rownumber=0;
        return this;

    }

    public int stages() {
        return 0;
    }

    public void write(String s) throws IOException {
        while (rownumber<table.values.length && table.values[rownumber].compareTo(s)<0) rownumber++;
        found=rownumber<table.values.length && table.values[rownumber].compareTo(s)==0;

    }

    public void close() throws Exception {
        rownumber=table.values.length;
    }*/


}
