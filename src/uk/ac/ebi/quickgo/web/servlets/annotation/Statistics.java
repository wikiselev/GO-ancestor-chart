package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.exchange.compress.find.Find;

import java.util.Map;

public class Statistics {
	public static class ProteinStatistics {
		public int vgi;
		public String protein;
		public int annotationCount;

		public ProteinStatistics(int vgi, String protein) {
			this.vgi = vgi;
			this.protein = protein;
			this.annotationCount = 0;
		}
	}

    public Find find;
    AnnotationQuery query;
    long when;
    long time;
    int count;

    public Statistics(AnnotationQuery query) {
        this.query = query;
        when = System.currentTimeMillis();
    }

    public void finish() {
        time = System.currentTimeMillis() - when;
    }

    public void increment() {
        count++;
    }

    int totalAnnotations;
    int totalProteins;

    int[] sourceAnnotationCounts;
    int[] sourceDates;
	int[] sourceProteinCounts;

    int[] evidenceAnnotationCounts;
	int[] evidenceProteinCounts;

    int[] qualifierAnnotationCounts;
	int[] qualifierProteinCounts;

	int[] referenceAnnotationCounts;
	int[] referenceProteinCounts;

	int[] aspectAnnotationCounts;
	int[] aspectProteinCounts;

    int[] termAnnotationCounts;
    int[] termProteinCounts;
    public int[] ancestorCounts;

    int[] taxIDs;
    int[] taxAnnotationCounts;
	int[] taxProteinCounts;

	Map<Integer, ProteinStatistics> proteinStatistics;
}
