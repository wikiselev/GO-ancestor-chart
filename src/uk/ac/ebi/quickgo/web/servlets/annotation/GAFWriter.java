package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.UniProtAc;
import uk.ac.ebi.quickgo.web.servlets.annotation.GAnnotationServlet.*;
import uk.ac.ebi.quickgo.web.configuration.Configuration;
import uk.ac.ebi.quickgo.web.configuration.DataFiles;

import java.io.IOException;
import java.io.Writer;
import java.io.Closeable;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

public class GAFWriter implements DataAction {
	Writer wr;
	private int limit;

	DataTranslate translate;
	Annotation annotation;
	String filterParms;

	private final String newLine = "\n";

	GAFWriter(Configuration config, DataFiles df, List<Closeable> connection, AnnotationRequest annRQ, AnnotationParameters parameters) throws Exception {
		translate = new DataTranslate(df, connection, annRQ.db);

		this.limit = annRQ.limit;
		this.filterParms = parameters.parameters(true);

		annotation = translate.makeAnnotation(config);
	}

	public void open(Writer wr) throws IOException {
		this.wr = wr;
		wr.write("!gaf-version: 2.0\n");
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

	public boolean act(AnnotationRow row) throws IOException {
		if (limit == 0) return false;
		limit--;

		if (!annotation.load(row)) return true;

		boolean isSpliceform = !("".equals(annotation.splice));

        final String tab = "\t";
		writeColumn(annotation.db, tab);
		if (isSpliceform && "UniProtKB".equals(annotation.db)) {
			// if annotation.db is not UniProtKB, then annotation.proteinAc contains the translated db_object_id, *not* the UniProt accession
			writeColumn(UniProtAc.getCanonicalAccession(annotation.proteinAc), tab);
		}
		else {
			writeColumn(annotation.proteinAc, tab);
		}
		writeColumn(annotation.symbol, tab);
		writeColumn(annotation.qualifier, tab);
		writeColumn(annotation.term.id(), tab);
		writeColumn(annotation.ref.db + ":" + annotation.ref.id, tab);
		writeColumn(annotation.evidence, tab);
		if ("".equals(annotation.with.withString)) {
			wr.write(tab);
		}
		else {
			writeColumn(annotation.with.withString, tab);
		}
		writeColumn(annotation.term.aspect.abbreviation, tab);
		writeColumn(annotation.name, tab);
		writeColumn(annotation.synonym, tab);
		writeColumn(annotation.type, tab);
		writeColumn("taxon:" + annotation.taxId + (!"".equals(annotation.extraTaxId) ? "|taxon:" + annotation.extraTaxId : ""), tab);
		writeColumn(annotation.date, tab);
		writeColumn(annotation.source, tab);
		wr.write(tab); // column 16 (annotation extension) not yet supported

		if (isSpliceform) {
			//writeColumn("UniProtKB:" + annotation.proteinAc, newLine);
			writeColumn(annotation.splice, newLine);
		}
		else {
			writeColumn("", newLine);
		}

		return true;
	}
}
