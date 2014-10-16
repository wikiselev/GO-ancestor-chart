package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.exchange.compress.find.Find;

import java.io.*;
import java.util.*;

class Summarize implements DataAction {
    private Statistics statistics;
    private Slimmer slimmer;

    int lastProtein = -1;

	int[] evidenceLastProtein;
	int[] sourceLastProtein;
	int[] qualifierLastProtein;
	int[] referenceLastProtein;
	int[] aspectLastProtein;
    int[] termLastProtein;

    int[] taxAnnotationCounts;
	int[] taxProteinCounts;
	int[] taxLastProtein;

    DataFiles dataFiles;
    private IntegerTableReader.Cursor proteinTaxonomyCursor;

    Summarize(DataFiles dataFiles, AnnotationQuery query) throws IOException {
        this.dataFiles = dataFiles;
        statistics = new Statistics(query);
        slimmer = new Slimmer(dataFiles.ontology, query.slimIDs, query.slimTypes);

	    int size = dataFiles.evidenceTable.size();
        statistics.evidenceAnnotationCounts = new int[size];
	    statistics.evidenceProteinCounts = new int[size];
	    evidenceLastProtein = new int[size];
	    Arrays.fill(evidenceLastProtein, -1);

	    size = dataFiles.sourceTable.size();
	    statistics.sourceDates = new int[size];
        statistics.sourceAnnotationCounts = new int[size];
	    statistics.sourceProteinCounts = new int[size];
	    sourceLastProtein = new int[size];
	    Arrays.fill(sourceLastProtein, -1);

        size = dataFiles.qualifierTable.size();
	    statistics.qualifierAnnotationCounts = new int[size];
	    statistics.qualifierProteinCounts = new int[size];
	    qualifierLastProtein = new int[size];
	    Arrays.fill(qualifierLastProtein, -1);

	    size = dataFiles.refDBidTable.size();
	    statistics.referenceAnnotationCounts = new int[size];
	    statistics.referenceProteinCounts = new int[size];
	    referenceLastProtein = new int[size];
	    Arrays.fill(referenceLastProtein, -1);

	    size = dataFiles.aspectTable.size();
	    statistics.aspectAnnotationCounts = new int[size];
	    statistics.aspectProteinCounts = new int[size];
	    aspectLastProtein = new int[size];

	    size = dataFiles.termIDs.size();
		statistics.termAnnotationCounts = new int[size];
		statistics.termProteinCounts = new int[size];
		statistics.ancestorCounts = new int[size];
		termLastProtein = new int[size];
		Arrays.fill(termLastProtein, -1);

	    size = dataFiles.treeLeft.length;
		taxAnnotationCounts = new int[size];
	    taxProteinCounts = new int[size];
	    taxLastProtein = new int[size];
	    Arrays.fill(taxLastProtein, -1);
		proteinTaxonomyCursor = dataFiles.proteinTaxonomy.use();

	    if (query.proteins != null && query.proteins.length > 0) {
		    statistics.proteinStatistics = new LinkedHashMap<Integer, Statistics.ProteinStatistics>();
		    TextTableReader.Cursor cursor = dataFiles.virtualGroupingIdTable.use();
		    for (String protein : query.proteins) {
			    int vgi = cursor.search(protein);
			    if (vgi >= 0) {
			        statistics.proteinStatistics.put(vgi, new Statistics.ProteinStatistics(vgi, protein));
			    }
		    }
	    }
    }

    public boolean act(AnnotationRow row) throws IOException {
        statistics.totalAnnotations++;

	    int vgi = row.virtualGroupingId;

        if (vgi != lastProtein) {
            statistics.totalProteins++;
            lastProtein = vgi;
        }

	    if (statistics.evidenceAnnotationCounts != null) {
		    int ev = row.evidence;

		    if (evidenceLastProtein[ev] != vgi) {
		        statistics.evidenceProteinCounts[ev]++;
		        evidenceLastProtein[ev] = vgi;
		    }

		    statistics.evidenceAnnotationCounts[ev]++;
	    }

	    if (statistics.sourceAnnotationCounts != null) {
		    int src = row.source;

		    if (sourceLastProtein[src] != vgi) {
		        statistics.sourceProteinCounts[src]++;
		        sourceLastProtein[src] = vgi;
		    }

		    statistics.sourceAnnotationCounts[src]++;

		    if (statistics.sourceDates != null && row.externalDate > statistics.sourceDates[src]) {
		        statistics.sourceDates[src] = row.externalDate;
		    }
	    }

	    if (statistics.qualifierAnnotationCounts != null) {
		    int qual = row.qualifier;

		    if (qualifierLastProtein[qual] != vgi) {
		        statistics.qualifierProteinCounts[qual]++;
		        qualifierLastProtein[qual] = vgi;
		    }

		    statistics.qualifierAnnotationCounts[qual]++;
	    }

	    if (statistics.referenceAnnotationCounts != null) {
		    int ref = row.reference;
		    if (referenceLastProtein[ref] != vgi) {
			    statistics.referenceProteinCounts[ref]++;
				referenceLastProtein[ref] = vgi;
		    }

		    statistics.referenceAnnotationCounts[ref]++;
	    }

	    if (statistics.aspectAnnotationCounts != null) {
		    int asp = row.aspect;
		    if (aspectLastProtein[asp] != vgi) {
			    statistics.aspectProteinCounts[asp]++;
			    aspectLastProtein[asp] = vgi;
		    }

		    statistics.aspectAnnotationCounts[asp]++;
	    }

        if (taxAnnotationCounts != null) {
            int[] taxRow = proteinTaxonomyCursor.read(vgi);
            if (taxRow != null && taxRow[0] > 0) {
	            int tax = taxRow[0];
	            if (taxLastProtein[tax] != vgi) {
	                taxProteinCounts[tax]++;
	                taxLastProtein[tax] = vgi;
	            }

	            taxAnnotationCounts[tax]++;
            }
        }

        if (statistics.termAnnotationCounts != null) {
            int term = row.term;

            if (termLastProtein[term] != vgi) {
                statistics.termProteinCounts[term]++;
                termLastProtein[term] = vgi;
            }

            statistics.termAnnotationCounts[term]++;

            int[] ancestors = slimmer.ancestorTranslate[term];
            for (int ancestor : ancestors) {
	            statistics.ancestorCounts[ancestor]++;
            }
        }

	    if (statistics.proteinStatistics != null) {
			Statistics.ProteinStatistics proteinStatistics = statistics.proteinStatistics.get(vgi);
		    if (proteinStatistics != null) {
			    proteinStatistics.annotationCount++;
		    }
	    }

        return true;
    }

    Statistics getStatistics(Find finder) {
        statistics.find = finder;
        if (taxAnnotationCounts != null) {
            int index = 0;
            for (int taxCount : taxAnnotationCounts) {
	            if (taxCount > 0) {
		            index++;
	            }
            }

            statistics.taxIDs = new int[index];
            statistics.taxAnnotationCounts = new int[index];
	        statistics.taxProteinCounts = new int[index];
            index = 0;
            for (int id = 0; id < taxAnnotationCounts.length; id++) {
                int tc = taxAnnotationCounts[id];
                if (tc > 0) {
	                statistics.taxIDs[index] = id;
	                statistics.taxAnnotationCounts[index] = tc;
	                statistics.taxProteinCounts[index] = taxProteinCounts[id];
	                index++;
                }
            }
        }
        return statistics;
    }
}
