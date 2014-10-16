package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;

public class StatisticsCache {

    DataFiles dataFiles;

    public final Statistics global;

    public StatisticsCache(DataFiles dataFiles) throws Exception {
        this.dataFiles = dataFiles;
        global = make(new AnnotationQuery(), true);
    }

    private final LinkedHashMap<AnnotationQuery,Statistics> statsCache = new LinkedHashMap<AnnotationQuery, Statistics>() {
        protected boolean removeEldestEntry(Map.Entry<AnnotationQuery, Statistics> entry) {
            return size() > 100;
        }
    };

    public Statistics make(AnnotationQuery query, boolean useCache) throws Exception {
        List<Closeable> connection = new ArrayList<Closeable>();
        Statistics statistics = null;
        if (useCache) {
	        statistics = get(query);
        }
        if (statistics == null) {
            Scanner dr = new Scanner(dataFiles, connection, false);

            Summarize summarize = new Summarize(dataFiles, query);
            Slimmer slimmer = new Slimmer(dataFiles.ontology, query.slimIDs, summarize, query.slimTypes);
            dr.scan(query, slimmer);

            statistics = summarize.getStatistics(dr.getFinder());
            statistics.finish();
        }
        if (useCache) {
	        put(query, statistics);
        }
        IOUtils.closeAll(connection);
        return statistics;
    }

    public synchronized Statistics get(AnnotationQuery query) {
        return (global != null && query.equals(global.query)) ? global : statsCache.get(query);
    }

    public synchronized void put(AnnotationQuery query, Statistics statistics) {
        statistics.increment();
        statsCache.remove(query);
        statsCache.put(query, statistics);
    }
}
