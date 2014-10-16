package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.data.Term;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.exchange.compress.find.Find;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.text.SimpleDateFormat;

public class Summary {
	public static NumberFormat nf = new DecimalFormat("#,##0.00");

    public /*static*/ class Bucket {
        public String code;
        public String name;
        public int count;
        public int pct;
        public int scale;
        public String colour;
        public String date;

	    private double percent;

        public Bucket(String code, String name, String colour, int total, int count, int width, int date) {
            this.code = code;
            this.name = name;
            this.colour = colour;
            if (total > 0) {
                pct = (int) (((long)count) * 100 / total);
                scale = (int) (((long)count) * width / total);
	            percent = ((double)count * 100.0) / (double)total;
            }
            this.count = count;
            if (date > 0) {
                this.date = dataFiles.dateTable.read(date);
            }
            else {
                this.date = "Unknown";
            }
        }

	    public boolean hasName() {
			return !(name == null || "".equals(name));		    
	    }

	    public String percentage() {
		    return nf.format(percent);
	    }

	    public void write(Writer wr) throws IOException {
		    if (count > 0) {
			    wr.write(("".equals(code) ? "(none)" : code) + tab + (hasName() ? name + tab : "") + percentage() + tab + count + newLine);
		    }
	    }
    }

    static String[] colours = {"#f00", "#fc0", "#ff0", "#0f0", "#0ff", "#0cf", "#ccf", "#00f", "#0cc", "#c0f", "#f0f", "#fcc", "#c00", "#0c0", "#00c", "#cc0", "#c0c"};

    static String colour(int i) {
        return colours[i % colours.length];
    }

    public /*static*/ class Category {
	    public String name;
	    public String contentType;
	    
        public int totalDistinctValues;
	    public boolean namedBuckets;

        public List<Bucket> buckets = new ArrayList<Bucket>();

        Category(String name, String contentType, int[] data, TextList table, int total, int width, int totalDistinctValues, int[] dates) throws IOException {
	        this(name, contentType, totalDistinctValues);

	        this.namedBuckets = false;
            for (int i = 0; i < data.length; i++) {
                buckets.add(new Bucket(table.read(i), "",  colour(i), total, data[i], width, ((dates == null) ? 0 : dates[i])));
                if (dates != null && dates[i] > lastDate) {
                    lastDate = dates[i];
                    try {
                        date = String.format("%1$tA, %1$td %1$tB %1$tY", new SimpleDateFormat("yyyyMMdd").parse(dataFiles.dateTable.read(lastDate)));
                    }
                    catch (Exception e) {
                    }
                }
            }
        }

        Category(String name, String contentType, int totalDistinctValues) {
	        this.name = name;
	        this.contentType = contentType;
            this.totalDistinctValues = totalDistinctValues;
	        this.namedBuckets = true;
        }

	    public String summary() {
		    return Integer.toString(totalDistinctValues);
	    }

	    public String scope() {
		    int n = buckets.size();
		    return (n >= totalDistinctValues) ? "all " + n + " distinct " + contentType : "the top " + n + " of " + totalDistinctValues + " distinct " + contentType;
	    }

	    public String proteinStatisticsSummary() {
		    if (hasProteinStatistics) {
			    if (annotatedProteinCount == proteinCount) {
				    return "All " + proteinCount + " proteins in the supplied list have annotations that match the filtering criteria.";
			    }
			    else {
				    return annotatedProteinCount + " of the " + proteinCount + " proteins in the supplied list have annotations that match the filtering criteria; " + (proteinCount - annotatedProteinCount) + " do not.";
			    }
		    }
		    else {
			    return "No protein list supplied";
		    }
	    }

	    private static final String defaultColumns = "Code\tName\tPercentage\tCount\n";

	    public void write(Writer wr, String columns) throws IOException {
			wr.write("[" + name + "]\n");
		    wr.write(columns);
		    for (Bucket b : buckets) {
			    b.write(wr);
		    }
		    wr.write(newLine);
	    }

	    public void write(Writer wr) throws IOException {
		    write(wr, defaultColumns);
	    }
    }

    public Category qualifierByAnnotation;
	public Category qualifierByProtein;

	public Category evidenceByAnnotation;
    public Category evidenceByProtein;

	public Category sourceByAnnotation;
    public Category sourceByProtein;

    public Category termByAnnotation;
	public Category termByProtein;

	public Category taxonByAnnotation;
    public Category taxonByProtein;

	public Category referenceByAnnotation;
    public Category referenceByProtein;

	public Category aspectByAnnotation;
    public Category aspectByProtein;

    public int totalAnnotations;
    public int totalProteins;

	public boolean hasProteinStatistics;
	public Category proteinStatistics;
	public int proteinCount = 0;
	public int annotatedProteinCount = 0;

    public String query;
    public String duration;
    public String when;
	public double microsecsPerAnnotation;

	public String timePerAnnotation() {
		return nf.format(microsecsPerAnnotation);
	}

    public int count;
    public DataFiles dataFiles;
    private int lastDate;
    public String date;
    public Find info;

    public Summary(Statistics s, DataFiles dataFiles, List<Closeable> connection, int limit) throws IOException {
        this.dataFiles = dataFiles;

        this.info = s.find;
        
        int width = 400;
        query = s.query.filter.toString();
        duration = s.time + "ms";
	    microsecsPerAnnotation = ((double)s.time / (double)s.totalAnnotations) * 1000.0;
        count = s.count;
        when = QuickGO.humanReadableDate.format(s.when);

        totalAnnotations = s.totalAnnotations;
        totalProteins = s.totalProteins;

	    evidenceByAnnotation = new Category("Evidence Codes (by annotation)", "Evidence Codes", s.evidenceAnnotationCounts, dataFiles.evidenceTable, totalAnnotations, width, summary(s.evidenceAnnotationCounts), null);
	    evidenceByProtein = new Category("Evidence Codes (by protein)", "Evidence Codes", s.evidenceProteinCounts, dataFiles.evidenceTable, totalProteins, width, summary(s.evidenceProteinCounts), null);

	    sourceByAnnotation = new Category("Sources (by annotation)", "Sources", s.sourceAnnotationCounts, dataFiles.sourceTable, totalAnnotations, width, summary(s.sourceAnnotationCounts), s.sourceDates);
	    sourceByProtein = new Category("Sources (by protein)", "Sources", s.sourceProteinCounts, dataFiles.sourceTable, totalProteins, width, summary(s.sourceProteinCounts), s.sourceDates);

	    qualifierByAnnotation = new Category("Qualifiers (by annotation)", "Qualifiers", s.qualifierAnnotationCounts, dataFiles.qualifierTable, totalAnnotations, width, summary(s.qualifierAnnotationCounts), null);
	    qualifierByProtein = new Category("Qualifiers (by protein)", "Qualifiers", s.qualifierProteinCounts, dataFiles.qualifierTable, totalProteins, width, summary(s.qualifierProteinCounts), null);

	    aspectByAnnotation = new Category("Aspects (by annotation)", "Aspects", s.aspectAnnotationCounts, dataFiles.aspectTable, totalAnnotations, width, summary(s.aspectAnnotationCounts), null);
	    aspectByProtein = new Category("Aspects (by protein)", "Aspects", s.aspectProteinCounts, dataFiles.aspectTable, totalProteins, width, summary(s.aspectProteinCounts), null);

		if (s.referenceAnnotationCounts != null) {
			int[] indices = top(s.referenceAnnotationCounts, limit);
			referenceByAnnotation = new Category("References (by annotation)", "References", summary(s.referenceAnnotationCounts));
			referenceByAnnotation.namedBuckets = false;
			for (int index : indices) {
			    if (index >= 0 && s.referenceAnnotationCounts[index] > 0) {
				    referenceByAnnotation.buckets.add(new Bucket(dataFiles.referenceTable.read(index), "", "#000", totalAnnotations, s.referenceAnnotationCounts[index], width, 0));
			    }
			}

			indices = top(s.referenceProteinCounts, limit);
			referenceByProtein = new Category("References (by protein)", "References", summary(s.referenceProteinCounts));
			referenceByProtein.namedBuckets = false;			
			for (int index : indices) {
			    if (index >= 0 && s.referenceProteinCounts[index] > 0) {
				    referenceByProtein.buckets.add(new Bucket(dataFiles.referenceTable.read(index), "", "#000", totalProteins, s.referenceProteinCounts[index], width, 0));
			    }
			}
		}

        if (s.taxAnnotationCounts != null) {
            TextTableReader.Cursor taxonomyCursor = dataFiles.taxonomy.open(connection);

            int[] indices = top(s.taxAnnotationCounts, limit);
            taxonByAnnotation = new Category("Taxon IDs (by annotation)", "Taxon IDs", summary(s.taxAnnotationCounts));
            for (int index : indices) {
                if (index >= 0 && s.taxAnnotationCounts[index] > 0) {
	                taxonByAnnotation.buckets.add(new Bucket(String.valueOf(s.taxIDs[index]), taxonomyCursor.read(s.taxIDs[index])[0], "#000", totalAnnotations, s.taxAnnotationCounts[index], width, 0));
                }
            }

	        indices = top(s.taxProteinCounts, limit);
	        taxonByProtein = new Category("Taxon IDs (by protein)", "Taxon IDs", summary(s.taxProteinCounts));
	        for (int index : indices) {
	            if (index >= 0 && s.taxProteinCounts[index] > 0) {
		            taxonByProtein.buckets.add(new Bucket(String.valueOf(s.taxIDs[index]), taxonomyCursor.read(s.taxIDs[index])[0], "#000", totalProteins, s.taxProteinCounts[index], width, 0));
	            }
	        }
        }

	    if (s.termProteinCounts != null) {
	        int[] indices = top(s.termAnnotationCounts, limit);
	        termByAnnotation = new Category("GO IDs (by annotation)", "GO IDs", summary(s.termAnnotationCounts));
	        for (int index : indices) {
	            if (index >= 0) {
	                Term t = dataFiles.ontology.terms[index];
	                termByAnnotation.buckets.add(new Bucket(t.id(), t.name(), "#000", totalAnnotations, s.termAnnotationCounts[index], width, 0));
	            }
	        }

		    indices = top(s.termProteinCounts, limit);
		    termByProtein = new Category("GO IDs (by protein)", "GO IDs", summary(s.termProteinCounts));
		    for (int index : indices) {
		        if (index >= 0) {
		            Term t = dataFiles.ontology.terms[index];
		            termByProtein.buckets.add(new Bucket(t.id(), t.name(), "#000", totalProteins, s.termProteinCounts[index], width, 0));
		        }
		    }
	    }

	    hasProteinStatistics = (s.proteinStatistics != null);
	    if (hasProteinStatistics) {
		    proteinCount = s.proteinStatistics.size();
		    proteinStatistics = new Category("Annotations (by protein)", "Proteins", proteinCount);
		    proteinStatistics.namedBuckets = false;

		    for (Integer vgi : s.proteinStatistics.keySet()) {
			    Statistics.ProteinStatistics ps = s.proteinStatistics.get(vgi);
			    proteinStatistics.buckets.add(new Bucket(ps.protein, null, "#000", totalAnnotations, ps.annotationCount, width, 0));
			    if (ps.annotationCount > 0) {
				    annotatedProteinCount++;
			    }
		    }
	    }
    }

    int summary(int[] all) {
        int count = 0;
        for (int v : all) {
	        if (v > 0) {
		        count++;
	        }
        }
        return count;
    }

    int[] top(int[] all, int count) {
        if (count == 0 || count > all.length) {
	        count = all.length;
        }

        int[] indices = new int[count];
        Arrays.fill(indices, -1);

        for (int j = 0; j < all.length; j++) {
            int v = all[j];
            if (v > 0) {
	            int index = 0;
	            while (index < count && indices[index] != -1 && v < all[indices[index]]) {
		            index++;
	            }
	            if (index != count) {
		            System.arraycopy(indices, index, indices, index + 1, count - index - 1);
		            indices[index] = j;
	            }
            }
        }
        return indices;
    }

	private final String tab = "\t";
	private final String newLine = "\n";

	public void write(Writer wr, String filterParameters) throws IOException {
		wr.write("!Annotation statistics\n");
		wr.write("!Downloaded from QuickGO at " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + newLine);
		wr.write("!URL: http://www.ebi.ac.uk/QuickGO/" + filterParameters + newLine);
		wr.write("!Contact email: goa@ebi.ac.uk\n");
		wr.write("!\n");

		wr.write("[Summary]\n");
		wr.write("Number of annotations\tNumber of distinct proteins\n");
		wr.write(totalAnnotations + tab + totalProteins + newLine + newLine);

		String columns = "Code\tPercentage\tCount\n";

		termByAnnotation.write(wr);
		termByProtein.write(wr);

		aspectByAnnotation.write(wr);
		aspectByProtein.write(wr);

		evidenceByAnnotation.write(wr, columns);
		evidenceByProtein.write(wr, columns);

		qualifierByAnnotation.write(wr, columns);
		qualifierByProtein.write(wr, columns);

		sourceByAnnotation.write(wr, columns);
		sourceByProtein.write(wr, columns);

		taxonByAnnotation.write(wr);
		taxonByProtein.write(wr);

		referenceByAnnotation.write(wr);
		referenceByProtein.write(wr);

		if (hasProteinStatistics) {
			proteinStatistics.write(wr);			
		}
	}
}
