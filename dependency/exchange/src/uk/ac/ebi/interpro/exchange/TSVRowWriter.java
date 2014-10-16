package uk.ac.ebi.interpro.exchange;

import uk.ac.ebi.interpro.common.*;

import java.io.*;
import java.util.zip.*;

public class TSVRowWriter implements RowWriter {


    public String toString() {
        return to.toString();
    }

    Writer wr;
    private File to;
    private String[] columns;
    private boolean postgres;
    private boolean compress;


    public void setColumns(String[] columns) {
        if (this.columns==null)
            this.columns = columns;
    }

    public TSVRowWriter(File to, String[] columns,boolean postgres,boolean compress,PrintWriter out) throws Exception {
        this.to = to;
        this.columns = columns;
        this.postgres = postgres;
        this.compress = compress;
        if (out!=null) out.println("Write TSV to "+to);

    }

    public void open() throws Exception {
        OutputStream outputStream = new FileOutputStream(this.to);
        if (this.compress)
            outputStream=new GZIPOutputStream(outputStream);
        wr = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF8"));
        write(this.columns);
    }


    public void write(String[] data) throws Exception {

        boolean first=true;
        for (String s : data) {
            if (!first) wr.write("\t");
            first=false;
            if (postgres) {
                if (s == null) wr.write("\\N");
                else
                    wr.write(StringUtils.quoteEscape(s, "\t\n\\", '\\'));
            } else {
                if (s==null) s="";
                wr.write(s.replace('\t',' ').replace('\n','\r'));
            }

        }
        wr.write("\n");

    }

    public void close() throws Exception {
        wr.close();
    }
}
