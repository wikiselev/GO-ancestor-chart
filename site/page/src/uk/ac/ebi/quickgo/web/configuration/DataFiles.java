package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import static uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.quickgo.web.servlets.annotation.*;

import java.io.*;
import java.util.*;

/**
 * Access to underlying data files.
 *
 * There is one instance of this class for each version of the data files.
 */

public class DataFiles implements Closeable {
    private static Location me = new Location();

    public DataLocation directory;

    public String stamp;

    public List<Closeable> connection = new ArrayList<Closeable>();

    // Main key tables
    public Table termIDs;

    // Controlled vocabularies
    public CV databases;
    public CV proteinIDCodes;
    public CVExt proteinIDSets;
    public CV proteinDatabases;
    public CV referenceCodes;
    public CV goRefs;
    public CV withCodes;
    public CV sources;
    public CV evidences;
	//public CV relations;
	//public OptionList relations;
    public CV qualifiers;
    public Table proteinDatabaseTable;
	public GpCodeSet gpCodeSet;
	public XrfAbbs xrfAbbs;

    // Annotation

    public IntegerTableReader annotations;

	public TextTableReader.Cache virtualGroupingIdTable;
    public Table ancestorRelationTable;
    public Table evidenceTable;
    public Table sourceTable;
    public Table qualifierTable;
	public Table aspectTable;
    public Table dateTable;
    public Table extraTaxTable;
	public Table referenceTable;
    public Table refDBcodeTable;
    public Table refDBidTable;
	public Table withStringTable;
    public Table withDBcodeTable;
    public Table withDBidTable;
    public Table spliceTable;
	public Table dbTable;
	public TextTableReader.Cache dbIdTable;
	public Table dbObjectTypeTable;

    public IndexReader ancestorRelationIndex;
    public IndexReader evidenceIndex;
    public IndexReader sourceIndex;
    public IndexReader termIndex;
    public IndexReader ancestorIndex;
	public IndexReader referenceIndex;
    public IndexReader refDBIndex;
    public IndexReader refIdIndex;
    public IndexReader withDBIndex;
    public IndexReader withIdIndex;
    public IndexReader qualifierIndex;
	public IndexReader aspectIndex;
	public IndexReader dbIndex;
	public IndexReader dbIdIndex;

    public StatisticsCache cache;
    public Summary stats;

    // Searching
    // search: protein
    public TextSearch proteinIDSearch;
    public TextSearch synonymSearch;
    public TextSearch geneSearch;
	public TextSearch proteinNameSearch;
	public TextSearch descriptionSearch;
	public TextSearch dbObjectNameSearch;
	public TextSearch dbObjectSymbolSearch;
	public TextSearch dbObjectSynonymSearch;
    // search: GO
    public TextSearch commentSearch;
    public TextSearch nameSearch;
    public TextSearch definitionSearch;
    public TextSearch termXrefSearch;
    // search: pubmed
    public TextSearch pubmedTitleSearch;
    public TextSearch interproSearch;
    public TextSearch taxonomySearch;

    // Protein information
    public TextTableReader taxonomy;
    public IntegerTableReader taxonomyTree;
    public IntegerTableReader.Cache proteinTaxonomy;
    public TextTableReader proteinInfo;
    public TextTableReader.Cache proteinIDs;
    public IntegerTableReader proteinIDMap;
    public int[] proteinAnnotationCounts;
    public int[] treeLeft;
    public int[] treeRight;
    public IndexReader proteinDBIndex;
    public IndexReader proteinIDIndex;
    public TextTableReader proteinSequences;
	public TextTableReader proteinMetadata;

    // Term & other ontological information
    public TermOntology ontology;
	public EvidenceCodeMap evidenceCodeMap;
	public AnnotationExtensionRelationSet annotationExtensionRelations;

	// miscellaneous information
	public AnnotationBlacklist blacklist = new AnnotationBlacklist();
	public List<PostProcessingRule> postProcessingRules = new ArrayList<PostProcessingRule>();

    // Reference information
    public TextTableReader pubmedInfo;
    public IntegerTableReader pubmedRef;
    public IntegerTableReader proteinWith;

    public TextTableReader interproInfo;
    public IntegerTableReader interproWith;

    DataFiles(File base, String stamp, Configuration cfg) throws Exception {
        boolean ok = false;
        try {
            this.stamp = stamp;
            this.directory = new DataLocation(base);

            Action action = me.start("Initialising datafiles");
            MemoryMonitor mm = new MemoryMonitor(true);

            initCV();
            initGOA();
            initGO();
            initProtein();
            initRef();
            initSearch();
            initCache(cfg);

            me.note(mm.end(), mm);
            me.stop(action);

            ok = true;
        }
        finally {
            // in the event of an out of memory condition, close all files, otherwise we'll run out handles
            if (!ok) {
	            close();            
            }
        }
        System.out.println("Loaded");
    }

    private void initCV() throws Exception {
        Action action = me.start("Init CV");
        MemoryMonitor mm = new MemoryMonitor(true);

        termIDs = directory.termIDs.read().extractColumn(0);

	    xrfAbbs = new XrfAbbs(directory);

		databases = new CV(directory.xrfAbbsInfo.reader(XrfAbbsInfo.ABBREVIATION, XrfAbbsInfo.DATABASE));

	    gpCodeSet = new GpCodeSet(directory);
	    gpCodeSet.load();

	    ArrayList<String> dbCodeList = gpCodeSet.allCodes();

        String[] dbCodes = dbCodeList.toArray(new String[dbCodeList.size()]);
        proteinDatabaseTable = new Table(dbCodes, new Type("database", dbCodes.length));

        System.out.println("GPT"+CollectionUtils.dump(proteinDatabaseTable.values));

	    proteinIDCodes = new CV();
	    for (String dbCode : gpCodeSet.dbCodes()) {
			String description = xrfAbbs.getDatabase(dbCode);
		    proteinIDCodes.add(dbCode, (description != null) ? description : dbCode);
	    }

	    proteinIDSets = new CVExt(directory.proteinSetsInfo.reader(ProteinSetsInfo.NAME, ProteinSetsInfo.DESCRIPTION, ProteinSetsInfo.PROJECT_URL));
	    proteinDatabases = new CV(dbCodeList);

	    goRefs = new CV(directory.goRefInfo.reader(GORefInfo.GO_REF, GORefInfo.NAME));

        evidences = new CV(directory.evidenceInfo.reader(EvidenceInfo.CODE, EvidenceInfo.NAME));

	    qualifiers = new CV(directory.qualifierInfo.reader(QualifierInfo.QUALIFIER, QualifierInfo.DESCRIPTION));
	    if (qualifiers.get("") == null) {
		    qualifiers.add("", new CV.Item("", "(none)"));
	    }

        me.note(mm.end(), mm);
        me.stop(action);
    }

    private void initGOA() throws Exception {
        Action action = me.start("Loading GOA");
        MemoryMonitor mm = new MemoryMonitor(true);

        annotations = directory.annotation.read();

	    virtualGroupingIdTable = directory.virtualGroupingIds.read().cache();
        ancestorRelationTable = directory.ancestorRelation.read().extractColumn(0);
        evidenceTable = directory.evidence.read().extractColumn(0);
        sourceTable = directory.source.read().extractColumn(0);
        qualifierTable = directory.qualifier.read().extractColumn(0);
	    aspectTable = directory.aspect.read().extractColumn(0);
        dateTable = directory.externalDate.read().extractColumn(0);
        extraTaxTable = directory.extraTaxId.read().extractColumn(0);
        referenceTable = directory.reference.read().extractColumn(0);
        refDBcodeTable = directory.refDB.read().extractColumn(0);
        refDBidTable = directory.refId.read().extractColumn(0);
	    withStringTable = directory.withString.read().extractColumn(0);
        withDBcodeTable = directory.withDB.read().extractColumn(0);
        withDBidTable = directory.withId.read().extractColumn(0);
        spliceTable = directory.splice.read().extractColumn(0);
	    dbTable = directory.db.read().extractColumn(0);
	    dbIdTable = directory.dbId.read().cache();
	    dbObjectTypeTable = directory.dbObjectType.read().extractColumn(0);

        ancestorRelationIndex = directory.annotationAncestorRelationIndex.read();
        evidenceIndex = directory.annotationEvidenceIndex.read();
        sourceIndex = directory.annotationSourceIndex.read();
        ancestorIndex = directory.annotationAncestorIndex.read();
        termIndex = directory.annotationGOIndex.read();
        refIdIndex = directory.annotationRefIdIndex.read();
        referenceIndex = directory.annotationReferenceIndex.read();
        refDBIndex = directory.annotationRefDBIndex.read();
        withIdIndex = directory.annotationWithIdIndex.read();
        withDBIndex = directory.annotationWithDBIndex.read();
        qualifierIndex = directory.annotationQualifierIndex.read();
	    aspectIndex = directory.annotationAspectIndex.read();
		dbIndex = directory.annotationDbIndex.read();
	    dbIdIndex = directory.annotationDbIdIndex.read();

        sources = new CV(databases, sourceTable.values);
        referenceCodes = new CV(databases, refDBcodeTable.values);
        withCodes = new CV(databases, withDBcodeTable.values);
        goRefs = new CV(goRefs, refDBidTable.values);

	    for (String[] row : directory.annotationBlacklist.reader(DataLocation.AnnotationBlacklistInfo.PROTEIN_AC, DataLocation.AnnotationBlacklistInfo.TAXON_ID, DataLocation.AnnotationBlacklistInfo.GO_ID, DataLocation.AnnotationBlacklistInfo.REASON, DataLocation.AnnotationBlacklistInfo.METHOD_ID, DataLocation.AnnotationBlacklistInfo.CATEGORY)) {
		    blacklist.add(row[5], row[0], row[1], row[2], row[3], row[4]);
	    }

	    for (String[] row : directory.postProcessingRules.reader(PostProcessingRulesInfo.RULE_ID, PostProcessingRulesInfo.ANCESTOR_GO_ID, PostProcessingRulesInfo.ANCESTOR_TERM, PostProcessingRulesInfo.RELATIONSHIP, PostProcessingRulesInfo.TAXON_NAME, PostProcessingRulesInfo.ORIGINAL_GO_ID, PostProcessingRulesInfo.ORIGINAL_TERM, PostProcessingRulesInfo.CLEANUP_ACTION, PostProcessingRulesInfo.AFFECTED_TAX_GROUP, PostProcessingRulesInfo.SUBSTITUTED_GO_ID, PostProcessingRulesInfo.SUBSTITUTED_TERM, PostProcessingRulesInfo.CURATOR_NOTES)) {
		    postProcessingRules.add(new PostProcessingRule(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11]));
	    }

        me.note(mm.end(), mm);
        me.stop(action);
    }

    private void initGO() throws Exception {
        Action action = me.start("Loading Ontologies");
        MemoryMonitor mm = new MemoryMonitor(true);

        ontology = new TermOntology(directory);
	    evidenceCodeMap = new EvidenceCodeMap(directory);
	    annotationExtensionRelations = new AnnotationExtensionRelationSet(ontology, directory);

        me.note(mm.end(), mm);
        me.stop(action);
    }

    private void initProtein() throws Exception {
        Action action = me.start("Loading Protein Info");
        MemoryMonitor mm = new MemoryMonitor(true);

        taxonomy = directory.taxonomy.read();
        taxonomyTree = directory.taxonomyTree.read();

        treeLeft = taxonomyTree.extractColumn(0);
        treeRight = taxonomyTree.extractColumn(1);

        proteinTaxonomy = directory.proteinTaxonomy.read().cache();

        proteinInfo = directory.proteinInfo.read();

        proteinIDs = directory.proteinIDs.read().cache();

        proteinAnnotationCounts = directory.proteinRepeats.read().extractColumn(0);

        proteinIDMap = directory.proteinIDMap.read();

        proteinDBIndex = directory.proteinDBIndex.read();

        proteinSequences = directory.proteinSequences.read();

	    proteinMetadata = directory.proteinMetadata.read();

        me.note(mm.end(),mm);
        me.stop(action);
    }

    void initRef() throws Exception {
        pubmedInfo = directory.pubmedInfo.read();
        pubmedRef = directory.pubmedRef.read();
        proteinWith = directory.proteinWith.read();
        interproInfo = directory.interproInfo.read();
        interproWith = directory.interproWith.read();
    }

    void initSearch() throws Exception {
        Action action = me.start("Loading Search");
        MemoryMonitor mm = new MemoryMonitor(true);

        nameSearch = new TextSearch(directory.termNames);
        definitionSearch = new TextSearch(directory.termDefinitions);
        commentSearch = new TextSearch(directory.termComments);

        termXrefSearch = new TextSearch(directory.termXrefs.read().cache(),directory.termXrefIndex);
        synonymSearch = new TextSearch(directory.termSynonyms);

        proteinIDIndex = directory.proteinIDIndex.read();
        proteinIDSearch = new TextSearch(proteinIDs, proteinIDIndex);

	    dbObjectNameSearch = new TextSearch(directory.dbObjectNameIndex);
	    dbObjectSymbolSearch = new TextSearch(directory.proteinSymbols.read().cache(), directory.proteinSymbolIndex);
	    dbObjectSynonymSearch = new TextSearch(directory.proteinSynonyms.read().cache(), directory.proteinSynonymIndex);

        geneSearch = new TextSearch(directory.proteinGenes.read().cache(),directory.proteinGeneIndex);

        descriptionSearch = new TextSearch(directory.proteinDescriptions);
	    proteinNameSearch = new TextSearch(directory.proteinNames);

        pubmedTitleSearch = new TextSearch(directory.pubmedTitle);
        interproSearch = new TextSearch(directory.interproIndex);
        taxonomySearch = new TextSearch(directory.taxonomyIndex);

        me.note(mm.end(),mm);
        me.stop(action);
    }

    public void initCache(Configuration cfg) throws Exception {
        cache = new StatisticsCache(this);
        List<Closeable> connection = new ArrayList<Closeable>();
        stats = new Summary(cache.global, this, connection, cfg.statsControl.defaultLimit);
        IOUtils.closeAll(connection);
    }

    public boolean delete() {
        boolean succeeded = true;
        File[] files = directory.base.listFiles();
        for (File f : files) {
            if (!f.delete()) {
	            succeeded = false;
            }
        }
        if (!directory.base.delete()) {
	        succeeded = false;
        }
        return succeeded;
    }

    public void close() {
        IOUtils.closeAll(connection);
    }
}