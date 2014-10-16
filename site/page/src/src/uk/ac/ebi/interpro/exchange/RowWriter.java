package uk.ac.ebi.interpro.exchange;

public interface RowWriter {
    void write(String[] data) throws Exception;

    void open() throws Exception;

    void close() throws Exception;

    void setColumns(String[] columns) throws Exception;
}
