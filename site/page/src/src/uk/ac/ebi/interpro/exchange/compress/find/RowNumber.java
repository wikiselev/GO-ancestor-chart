package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class RowNumber implements Find {
    private int rowNumber;

    public RowNumber(int rowNumber) {this.rowNumber = rowNumber;}

    public int next(int at) throws IOException {
        if (at<=rowNumber) return rowNumber; else return Integer.MAX_VALUE;
    }

    public Find[] getChildren() {
        return new Find[0];
    }

    public BitReader getBitReader() {
        return null;
    }

    @Override
    public String toString() {
        return "RowNumber rowNumber=" + rowNumber;
    }
}
