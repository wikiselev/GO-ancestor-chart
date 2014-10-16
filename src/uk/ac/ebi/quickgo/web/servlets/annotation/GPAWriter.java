package uk.ac.ebi.quickgo.web.servlets.annotation;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import uk.ac.ebi.quickgo.web.configuration.Configuration;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;
import uk.ac.ebi.quickgo.web.servlets.annotation.GAnnotationServlet.DataTranslate;

public class GPAWriter implements DataAction {
	Writer wr;
	private int limit;

	DataFiles df;
	DataTranslate translate;
	Annotation annotation;
	String filterParms;

	private final String tab = "\t";
	private final String newLine = "\n";
	private final String pipe = "|";

	private HashMap<String, String> defaultQualifiers = new HashMap<String, String>();

	GPAWriter(Configuration config, DataFiles df, List<Closeable> connection, GAnnotationServlet.AnnotationRequest annRQ, GAnnotationServlet.AnnotationParameters parameters) throws Exception {
		this.df = df;
		translate = new DataTranslate(df, connection, annRQ.db);

		this.limit = annRQ.limit;
		this.filterParms = parameters.parameters(true);

		annotation = translate.makeAnnotation(config);

		defaultQualifiers.put("P", "involved_in");
		defaultQualifiers.put("F", "enables");
		defaultQualifiers.put("C", "part_of");
	}

	public void open(Writer wr) throws IOException {
		this.wr = wr;
		wr.write("!gpa-version: 1.1\n");
		wr.write("!Project_name: UniProt GO Annotation (UniProt-GOA)\n");
		wr.write("!URL: http://www.ebi.ac.uk/GOA\n");
		wr.write("!Contact Email: goa@ebi.ac.uk\n");
		wr.write("!Date downloaded from the QuickGO browser: " + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "\n");
		wr.write("!Filtering parameters selected to generate file: " + filterParms + newLine);
	}
	
	private void writeColumn(String s, String appendage) throws IOException {
		wr.write(s);
		if (appendage != null) {
			wr.write(appendage);
		}
	}

	/**
	 * See http://wiki.geneontology.org/index.php/Final_GPAD_and_GPI_file_format
	 */
	public boolean act(AnnotationRow row) throws IOException {
		if (limit == 0) return false;
		limit--;

		if (!annotation.load(row)) return true;

		// DB
		writeColumn(annotation.db, tab);
		// DB Object ID
		writeColumn(annotation.proteinAc, tab);
		// Qualifier		
		writeColumn(calculateQualifier(), tab);		
		// GO ID
		writeColumn(annotation.term.id(), tab);		
		// DB Reference/s
		writeColumn(annotation.ref.db + ":" + annotation.ref.id, tab);
		// Evidence Code
		writeColumn(df.evidenceCodeMap.translate(annotation.evidence, "GO_REF".equals(annotation.ref.db) ? annotation.ref.id : null), tab);
		// With
		if ("".equals(annotation.with.withString)) {
			wr.write(tab);
		}
		else {
			writeColumn(annotation.with.withString, tab);
		}

		// Interacting tax Id
		writeColumn("".equals(annotation.extraTaxId) ? "" : "taxon:" + annotation.extraTaxId, tab);
		// Date
		writeColumn(annotation.date, tab);
		// Assigned by
		writeColumn(annotation.source, tab);
		wr.write(tab); // column 16 (annotation extension) not yet supported
		wr.write("go_evidence=" + annotation.evidence); // Annotation properties. Now just printing GO evidence code
		wr.write(newLine);
		
		return true;
	}
	
	/**
	 * Calculates the qualifier value
	 * @return Qualifier value
	 */
	private String calculateQualifier(){
		String qualifier = "";
		if (annotation.qualifier.trim().length() == 0) {// Empty
			qualifier = defaultQualifiers.get(annotation.term.aspect.abbreviation);
		} else if("NOT".equals(annotation.qualifier)){// NOT
			qualifier = "NOT" + pipe + defaultQualifiers.get(annotation.term.aspect.abbreviation);
		} else {
			qualifier = annotation.qualifier;
		}
		return qualifier;
	}
}