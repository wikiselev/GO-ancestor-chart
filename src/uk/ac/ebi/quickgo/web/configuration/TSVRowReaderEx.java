package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.TSVRowReader;

import java.io.File;
import java.io.PrintWriter;

public class TSVRowReaderEx extends TSVRowReader {
	public TSVRowReaderEx(File from, String[] columns,boolean postgres,boolean compress, PrintWriter monitor) {
		super(from, columns, postgres, compress, monitor);
	}

	// override the base class implementation of directRead to make it handle \\ correctly
	public void directRead() throws Exception {
	    String line;
	    data.clear();
	    boolean isNull = false;

	    int count = 0;
	    line = rd.readLine();
	    if (line == null) {
		    eof = true;
		    return;
	    }

	    field.setLength(0);
	    boolean escape = false;

	    while (count < line.length()) {
	        char ch = line.charAt(count);
	        if (escape) {
	            if (ch == 't') {
		            field.append('\t');
	            }
	            else if (ch == 'n') {
		            field.append('\n');
	            }
	            else if (ch == 'N') {
		            isNull = true;
	            }
	            else if (ch == '\\') {
		            field.append('\\');
	            }
	            escape = false;
	        }
	        else {
	            if (ch == '\\' && postgres) {
	                escape = true;
	            }
	            else {
	                if (ch == '\t') {
	                    if (isNull) {
	                        data.add(null);
	                    }
	                    else {
	                        data.add(field.toString());
	                    }
	                    field.setLength(0);
	                    isNull = false;
	                }
	                else {
	                    field.append(ch);
	                }
	            }

	        }
	        count++;
	        if (count == line.length() && escape) {
	            line = rd.readLine();
	            if (line == null) {
		            break;
	            }
	            count = 0;
	            escape = false;
	        }
	    }

	    if (isNull){
		    data.add(null);
	    }
	    else {
		    data.add(field.toString());
	    }
	}
}
