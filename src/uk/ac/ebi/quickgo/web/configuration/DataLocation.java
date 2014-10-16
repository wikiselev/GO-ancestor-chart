package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.compress.TextTableReader;
import uk.ac.ebi.interpro.exchange.compress.IntegerTableReader;
import uk.ac.ebi.interpro.exchange.compress.IndexReader;
import uk.ac.ebi.interpro.exchange.Progress;
import uk.ac.ebi.interpro.exchange.TSVRowReader;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class DataLocation {
	// Version 48 adds DEFINITION_XREFS
	public static final String VERSION = "48";

	File base;

	public DataLocation(File directory) {
		this.base = directory;
	}

	public File getBase() {
		return base;
	}

	public NamedFile[] holder(NamedFile... files) {
	    return files;
	}

	public NamedFile[] holder(NamedFile[]... files) {
	    List<NamedFile> list = new ArrayList<NamedFile>();
	    for (NamedFile[] f : files) list.addAll(Arrays.asList(f));
	    return list.toArray(new NamedFile[list.size()]);
	}

	public NamedFile[] holder(File... files) {
	    List<NamedFile> list = new ArrayList<NamedFile>();
	    for (File f : files) list.add(new NamedFile(f.getName()));
	    return list.toArray(new NamedFile[list.size()]);
	}

	public class NamedFile {
	    String name;

	    public NamedFile(String name) {
	        this.name = name;
	    }

	    public File file() {
	        return new File(base, name);
	    }

	    public String getName() {
	        return name;
	    }
	}

 	public class ExtensionFile extends NamedFile {
 	    public ExtensionFile(String name, String extension) {
	        super(name + extension);
	    }

	}

	public class TextTable extends ExtensionFile {
	    public TextTable(String name) {
	        super(name, ".tls");
	    }

	    public TextTableReader read() throws IOException {
	        return new TextTableReader(file());
	    }
	}

	public class IntegerTable extends ExtensionFile {
	    public IntegerTable(String name) {
	        super(name, ".ils");
	    }

	    public IntegerTableReader read() throws IOException {
	        return new IntegerTableReader(file());
	    }
	}

	public class Index extends ExtensionFile {
	    public Index(String name) {
	        super(name, ".ind");
	    }

	    public IndexReader read() throws IOException {
	        return new IndexReader(file());
	    }
	}

	public class TextIndexColumn  {
	    public TextTable words;
	    public IntegerTable pairs;
	    public Index wordIndex;
	    public Index pairIndex;

	    TextIndexColumn(String prefix) {
	        words = new TextTable(prefix + "-words");
	        pairs = new IntegerTable( prefix + "-pairs");
	        wordIndex = new Index( prefix + "-words");
	        pairIndex = new Index( prefix + "-pairs");
	    }

	    public NamedFile[] all() {return new NamedFile[]{words,pairs,wordIndex,pairIndex};}
	}

	public class TSVTable<X extends Enum<X>> extends ExtensionFile {
	    boolean gz;

	    public TSVTable(boolean gz, String name) {
	        super(name,gz?".dat.gz":".dat");
	        this.gz = gz;
	    }

	    public RowIterator reader(X... columns) throws Exception {
	        String[] names = new String[columns.length];
	        for (int i = 0; i < columns.length; i++) {names[i] = columns[i].name();}
	        return new RowIterator(Progress.monitor(name, new TSVRowReaderEx(file(), names, true, gz, null)));
	    }
	}

	public IntegerTable integerTable(String name) {
		return new IntegerTable(name);
	}
	
	// Term information

	// Term: Source data
	public enum GOXref { TERM_ID, DB_CODE, DB_ID }
	public TSVTable<GOXref> goXrefs = new TSVTable<GOXref>(true, "ALL_XREFS");
   	public TSVTable<GOXref> goDefinitionXrefs = new TSVTable<GOXref>(true, "DEFINITION_XREFS");

	public enum GOComments { TERM_ID, COMMENT_TEXT }
	public TSVTable<GOComments> goComments = new TSVTable<GOComments>(true, "COMMENTS");

	public enum GOTermNames { GO_ID, CATEGORY, NAME, IS_OBSOLETE }
	public TSVTable<GOTermNames> goTermNames = new TSVTable<GOTermNames>(true, "TERMS");

	public enum GOTermDefinitions { TERM_ID, DEFINITION }
	public TSVTable<GOTermDefinitions> goTermDefinitions = new TSVTable<GOTermDefinitions>(true, "DEFINITIONS");

	public enum GOTermSynonyms { TERM_ID, NAME, TYPE }
	public TSVTable<GOTermSynonyms> goTermSynonyms = new TSVTable<GOTermSynonyms>(true,"SYNONYMS");

	public enum GORelation { CHILD_ID, PARENT_ID, RELATION_TYPE }
	public TSVTable<GORelation> goRelations = new TSVTable<GORelation>(true, "RELATIONS");

    public enum GOCrossOntologyRelation { TERM_ID, RELATION, FOREIGN_NAMESPACE, FOREIGN_ID, FOREIGN_TERM, URL }
   	public TSVTable<GOCrossOntologyRelation> goCrossOntologyRelations = new TSVTable<GOCrossOntologyRelation>(true, "CROSS_ONTOLOGY_RELATIONS");

	public enum Subset { TERM_ID, SUBSET, TYPE }
	public TSVTable<Subset> subsets = new TSVTable<Subset>(true, "SUBSETS");

	public enum Version { VERSION, TIMESTAMP, URL }
	public TSVTable<Version> version = new TSVTable<Version>(true,"VERSION");

	public enum ProteinComplexes { GO_ID, DB, DB_OBJECT_ID, DB_OBJECT_SYMBOL, DB_OBJECT_NAME }
	public TSVTable<ProteinComplexes> proteinComplexes = new TSVTable<ProteinComplexes>(true, "PROTEIN_COMPLEXES");

	public enum TaxonUnions { UNION_ID, NAME, TAXA }
	public TSVTable<TaxonUnions> taxonUnions = new TSVTable<TaxonUnions>(true, "TAXON_UNIONS");

	public enum TaxonConstraints { RULE_ID, GO_ID, NAME, RELATIONSHIP, TAX_ID_TYPE, TAX_ID, TAXON_NAME, SOURCES }
	public TSVTable<TaxonConstraints> taxonConstraints = new TSVTable<TaxonConstraints>(true, "TAXON_CONSTRAINTS");

	public enum TermTaxonConstraints { GO_ID, RULE_ID }
	public TSVTable<TermTaxonConstraints> termTaxonConstraints = new TSVTable<TermTaxonConstraints>(true, "TERM_TAXON_CONSTRAINTS");

	public enum GOTermHistory { TERM_ID, TIMESTAMP, ACTION, NAME, CATEGORY, TEXT }
	public TSVTable<GOTermHistory> goTermHistory = new TSVTable<GOTermHistory>(true, "TERM_HISTORY");

	public enum GOTermCredits { TERM_ID, CREDIT_CODE }
	public TSVTable<GOTermCredits> goTermCredits = new TSVTable<GOTermCredits>(true, "TERM_CREDITS");

	public enum FundingBodies { CODE, DESCRIPTION, URL }
	public TSVTable<FundingBodies> fundingBodies = new TSVTable<FundingBodies>(true, "FUNDING_BODIES");

	public enum AnnExtRelations { RELATION, USAGE, DOMAIN }
	public TSVTable<AnnExtRelations> annExtRelations = new TSVTable<AnnExtRelations>(true, "ANNOTATION_EXTENSION_RELATIONS");

	public enum AnnExtRelRelations { CHILD, PARENT, RELATION_TYPE }
	public TSVTable<AnnExtRelRelations> aerRelations = new TSVTable<AnnExtRelRelations>(true, "AER_RELATIONS");

	public enum AnnExtRelSecondaries { RELATION, SECONDARY_ID }
	public TSVTable<AnnExtRelSecondaries> aerSecondaries = new TSVTable<AnnExtRelSecondaries>(true, "AER_SECONDARIES");

	public enum AnnExtRelSubsets { RELATION, SUBSET }
	public TSVTable<AnnExtRelSubsets> aerSubsets = new TSVTable<AnnExtRelSubsets>(true, "AER_SUBSETS");

	public enum AnnExtRelDomains { RELATION, ENTITY, ENTITY_TYPE }
	public TSVTable<AnnExtRelDomains> aerDomains = new TSVTable<AnnExtRelDomains>(true, "AER_DOMAINS");

	public enum AnnExtRelRanges { RELATION, ENTITY, ENTITY_TYPE }
	public TSVTable<AnnExtRelRanges> aerRanges = new TSVTable<AnnExtRelRanges>(true, "AER_RANGES");

	public enum AnnExtRelRangeDefaults { NAMESPACE, ID_SYNTAX, ENTITY_TYPE }
	public TSVTable<AnnExtRelRangeDefaults> aerRangeDefaults = new TSVTable<AnnExtRelRangeDefaults>(true, "AER_RANGE_DEFAULTS");

	public enum AnnExtRelEntitySyntax { ENTITY, ENTITY_TYPE, NAMESPACE, ID_SYNTAX }
	public TSVTable<AnnExtRelEntitySyntax> aerEntitySyntax = new TSVTable<AnnExtRelEntitySyntax>(true, "AER_ENTITY_SYNTAX");

	NamedFile[] termSource = holder(
		goXrefs, goDefinitionXrefs, goComments, goTermNames, goTermDefinitions, goTermSynonyms, goRelations, goCrossOntologyRelations, subsets, version, proteinComplexes, taxonUnions, taxonConstraints, termTaxonConstraints, goTermHistory, goTermCredits, fundingBodies,
		annExtRelations, aerRelations, aerSecondaries, aerSubsets, aerDomains, aerRanges, aerRangeDefaults, aerEntitySyntax
	);

	// Term: Derived data
	public TextTable termIDs = new TextTable("term-ids");
	public TextIndexColumn termNames = new TextIndexColumn("term-name");
	public TextIndexColumn termDefinitions = new TextIndexColumn("term-definitions");
	public TextIndexColumn termComments = new TextIndexColumn("term-comments");
	public TextIndexColumn termSynonyms = new TextIndexColumn("term-synonyms");

	public Index termXrefIndex = new Index("term-xref-index");
	public TextTable termXrefs = new TextTable("term-xref-ids");

	NamedFile[] termDerived = holder(
			termNames.all(), termDefinitions.all(), termComments.all(), termSynonyms.all(), holder(
					termIDs, termXrefIndex, termXrefs
			)
	);

	// Reference information

	// Reference: Source data
	public enum Pubmed { PUBMED_ID, TITLE }
	public TSVTable<Pubmed> pubmed = new TSVTable<Pubmed>(true, "PUBLICATIONS");
	public enum InterPro { ENTRY_AC, NAME }
	public TSVTable<InterPro> interpro = new TSVTable<InterPro>(true, "ENTRY");

	NamedFile[] referenceSource = holder(pubmed, interpro);

	// Reference: derived data
	public TextTable pubmedInfo = new TextTable("pubmed-info");
	public IntegerTable pubmedRef = new IntegerTable("pubmed-ref");
	public IntegerTable proteinWith = new IntegerTable("protein-with");
	public TextIndexColumn pubmedTitle = new TextIndexColumn("pubmed-titles");

	public TextTable interproInfo = new TextTable("interpro-info");
	public IntegerTable interproWith = new IntegerTable("interpro-with");
	public TextIndexColumn interproIndex = new TextIndexColumn("interpro-index");

	NamedFile[] referenceDerived = holder(
			pubmedTitle.all(), interproIndex.all(), holder(
					pubmedInfo, pubmedRef, proteinWith, interproInfo, interproWith
			)
	);

	// Protein information

	// Protein: Source data
	public enum Taxonomy { TAX_ID, TREE_LEFT, TREE_RIGHT, SCIENTIFIC, COMMON }
	public TSVTable<Taxonomy> taxonomySource = new TSVTable<Taxonomy>(true,"NODE");

	public enum Sequence { protein, sequence }
	public TSVTable<Sequence> sequenceSource = new TSVTable<Sequence>(true,"sequences");

	NamedFile[] proteinSource = holder(taxonomySource, sequenceSource);

	// Protein: Derived data
	public TextTable virtualGroupingIds = new TextTable("protein-virtual-grouping-ids");
	public TextTable proteinIDs = new TextTable("protein-ids");
	public TextTable proteinInfo = new TextTable("protein-info");
	public IntegerTable proteinIDMap = new IntegerTable("protein-id-map");
	public TextIndexColumn proteinDescriptions = new TextIndexColumn("protein-descriptions");
	public TextIndexColumn proteinNames = new TextIndexColumn("protein-names");
	public Index proteinIDIndex = new Index("protein-id-index");
	public Index proteinDBIndex = new Index("protein-db-index");
	public IntegerTable proteinTaxonomy = new IntegerTable("protein-tax");
	public IntegerTable proteinRepeats = new IntegerTable("protein-repeats");
	public TextTable taxonomy = new TextTable("taxonomy");
	public IntegerTable taxonomyTree = new IntegerTable("taxonomy");
	public TextIndexColumn taxonomyIndex = new TextIndexColumn("taxonomy-index");
	public TextTable proteinGenes = new TextTable("protein-genes");
	public Index proteinGeneIndex = new Index("protein-gene-index");
	public TextTable proteinSequences = new TextTable("protein-sequences");
	public TextTable proteinMetadata = new TextTable("protein-metadata");
	public TextTable proteinSymbols = new TextTable("protein-symbols");
	public Index proteinSymbolIndex = new Index("protein-symbol-index");
	public TextTable proteinSynonyms = new TextTable("protein-synonyms");
	public Index proteinSynonymIndex = new Index("protein-synonym-index");

	NamedFile[] proteinDerived = holder(
			proteinDescriptions.all(), proteinNames.all(), taxonomyIndex.all(), holder(
					virtualGroupingIds, proteinInfo, proteinTaxonomy, proteinIDMap, proteinIDs, proteinIDIndex, proteinDBIndex,
					proteinRepeats, taxonomy, taxonomyTree, proteinGenes, proteinGeneIndex, proteinSequences, proteinMetadata,
					proteinSymbols, proteinSymbolIndex, proteinSynonyms, proteinSynonymIndex
			)
	);

	// Controlled vocabularies
	public enum EvidenceInfo { CODE, NAME }
	public TSVTable<EvidenceInfo> evidenceInfo = new TSVTable<EvidenceInfo>(true, "CV_EVIDENCES");

	public enum QualifierInfo { QUALIFIER, DESCRIPTION }
	public TSVTable<QualifierInfo> qualifierInfo = new TSVTable<QualifierInfo>(true, "CV_QUALIFIERS");

	public enum GORefInfo { NAME, GO_REF }
	public TSVTable<GORefInfo> goRefInfo = new TSVTable<GORefInfo>(true, "CV_GO_REFS");

	public enum XrfAbbsInfo { ABBREVIATION, DATABASE, GENERIC_URL, URL_SYNTAX }
	public TSVTable<XrfAbbsInfo> xrfAbbsInfo = new TSVTable<XrfAbbsInfo>(true, "XRF_ABBS");

	public enum ProteinSetsInfo { NAME, DESCRIPTION, PROJECT_URL }
	public TSVTable<ProteinSetsInfo> proteinSetsInfo = new TSVTable<ProteinSetsInfo>(true, "CV_PROTEIN_SETS");

	public enum Evidence2ECOInfo { CODE, GO_REF, ECO_ID }
	public TSVTable<Evidence2ECOInfo> evidence2ECO = new TSVTable<Evidence2ECOInfo>(true, "EVIDENCE2ECO");

	public enum AnnotationBlacklistInfo { PROTEIN_AC, TAXON_ID, GO_ID, REASON, METHOD_ID, CATEGORY, ENTRY_TYPE }
	public TSVTable<AnnotationBlacklistInfo> annotationBlacklist = new TSVTable<AnnotationBlacklistInfo>(true, "ANNOTATION_BLACKLIST");

	public enum AnnotationGuidelinesInfo { GO_ID, TITLE, URL }
	public TSVTable<AnnotationGuidelinesInfo> annotationGuidelines = new TSVTable<AnnotationGuidelinesInfo>(true, "ANNOTATION_GUIDELINES");

	public enum PlannedGOChangesInfo { GO_ID, TITLE, URL }
	public TSVTable<PlannedGOChangesInfo> plannedGOChanges = new TSVTable<PlannedGOChangesInfo>(true, "PLANNED_GO_CHANGES");

	public enum PostProcessingRulesInfo { RULE_ID, ANCESTOR_GO_ID, ANCESTOR_TERM, RELATIONSHIP, TAXON_NAME, ORIGINAL_GO_ID, ORIGINAL_TERM, CLEANUP_ACTION, AFFECTED_TAX_GROUP, SUBSTITUTED_GO_ID, SUBSTITUTED_TERM, CURATOR_NOTES }
	public TSVTable<PostProcessingRulesInfo> postProcessingRules = new TSVTable<PostProcessingRulesInfo>(true, "POST_PROCESSING_RULES");

	NamedFile[] controlledVocabs = holder(evidenceInfo, qualifierInfo, goRefInfo, xrfAbbsInfo, proteinSetsInfo, evidence2ECO, annotationBlacklist, annotationGuidelines, plannedGOChanges, postProcessingRules);

	// Controlled vocabularies: derived data
	public enum GP2ProteinDB {CODE, IS_DB}
	public TSVTable<GP2ProteinDB> gp2proteinDb = new TSVTable<GP2ProteinDB>(true, "GP2PROTEIN_DB");

	NamedFile[] controlledVocabsDerived = holder(gp2proteinDb);

	// Annotation: source data
	NamedFile gpDataFileList = new NamedFile("GP_DATA_FILES");
	NamedFile mappingFileList = new NamedFile("MAPPING_FILES");
	NamedFile proteinSetFileList = new NamedFile("PROTEIN_SET_FILES");
	NamedFile[] annotationSource = holder(gpDataFileList, mappingFileList, proteinSetFileList);

	// Annotation: Derived data
    public TextTable ancestorRelation = new TextTable("annotation-ancestor-relation");
	public TextTable source = new TextTable("annotation-source");
	public TextTable evidence = new TextTable("annotation-evidence");
	public TextTable reference = new TextTable("annotation-reference");
	public TextTable refDB = new TextTable("annotation-refdb");
	public TextTable refId = new TextTable("annotation-refid");
	public TextTable withString = new TextTable("annotation-with-string");
	public TextTable withDB = new TextTable("annotation-withdb");
	public TextTable withId = new TextTable("annotation-withid");
	public TextTable qualifier = new TextTable("annotation-qualifier");
	public TextTable aspect = new TextTable("annotation-aspect");
	public TextTable extraTaxId = new TextTable("annotation-extrataxid");
	public TextTable externalDate = new TextTable("annotation-externaldate");
	public TextTable splice = new TextTable("annotation-splice");
	public TextTable db = new TextTable("annotation-db");
	public TextTable dbId = new TextTable("annotation-dbid");
	public TextTable dbObjectType = new TextTable("db-object-type");

	public TextIndexColumn dbObjectNameIndex = new TextIndexColumn("db-object-name");

	public Index annotationGOIndex = new Index("annotation-go-index");
	public Index annotationAncestorIndex = new Index("annotation-ancestor-index");
    public Index annotationAncestorRelationIndex = new Index("annotation-ancestor-relation-index");
	public Index annotationEvidenceIndex = new Index("annotation-evidence-index");
	public Index annotationQualifierIndex = new Index("annotation-qualifier-index");
	public Index annotationReferenceIndex = new Index("annotation-reference-index");
	public Index annotationRefDBIndex = new Index("annotation-refdb-index");
	public Index annotationRefIdIndex = new Index("annotation-refid-index");
	public Index annotationWithDBIndex = new Index("annotation-withdb-index");
	public Index annotationWithIdIndex = new Index("annotation-withid-index");
	public Index annotationSourceIndex = new Index("annotation-source-index");
	public Index annotationDbIndex = new Index("annotation-db-index");
	public Index annotationDbIdIndex = new Index("annotation-dbid-index");
	public Index annotationAspectIndex = new Index("annotation-aspect-index");

	public IntegerTable annotation = new IntegerTable("annotation");

	NamedFile[] annotationDerived = holder(
			holder(
                    ancestorRelation,annotationAncestorRelationIndex,
					annotationGOIndex, annotationEvidenceIndex, annotationAncestorIndex, annotationQualifierIndex, annotationAspectIndex,
					annotationReferenceIndex, annotationRefDBIndex, annotationRefIdIndex, annotationWithDBIndex,annotationWithIdIndex, annotationSourceIndex,
					source, evidence, reference, refDB, refId, withString, withDB, withId, qualifier, aspect, extraTaxId, externalDate, splice, annotation,
					db, dbId, dbObjectType, annotationDbIndex, annotationDbIdIndex
			), dbObjectNameIndex.all() 
	);

	// Prerequisite file set
	protected NamedFile[] prerequisite = holder(annotationSource, referenceSource, proteinSource, termSource, controlledVocabs);

	// Archive file set
	protected NamedFile[] archive = holder(termDerived, annotationDerived, proteinDerived, referenceDerived, termSource, controlledVocabs, controlledVocabsDerived);

	public NamedFile[] requiredFiles() {
		return prerequisite;
	}

	public NamedFile[] archiveFiles() {
		return archive;
	}

	public NamedFile[] getMappingFiles() {
		return getFiles(mappingFileList, "gp2protein");
	}

	public NamedFile[] getProteinSetFiles() {
		return getFiles(proteinSetFileList, "gp2protein");
	}

	public NamedFile[] getGPDataFiles() {
		return getFiles(gpDataFileList, null);
	}

	public NamedFile[] getFiles(NamedFile listFile, final String prefix) {
		List<NamedFile> list = new ArrayList<NamedFile>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(listFile.file()));
			String fileName;
			while ((fileName = reader.readLine()) != null) {
				if (!"".equals(fileName) && (prefix == null || fileName.startsWith(prefix))) {
					list.add(new NamedFile(fileName));
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return list.toArray(new NamedFile[list.size()]);
	}

	// Download files
	public final static String stampName = "quickgo-stamp-v" + VERSION + ".txt";
}
