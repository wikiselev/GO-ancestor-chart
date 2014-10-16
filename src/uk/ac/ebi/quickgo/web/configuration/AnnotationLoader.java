package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.performance.MemoryMonitor;
import uk.ac.ebi.interpro.exchange.Progress;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.quickgo.web.data.Term;
import uk.ac.ebi.quickgo.web.data.TermOntology;
import uk.ac.ebi.quickgo.web.data.TermRelation;
import uk.ac.ebi.quickgo.web.servlets.annotation.AnnotationRow;
import uk.ac.ebi.quickgo.web.update.ProteinXrefs;
import uk.ac.ebi.quickgo.web.update.SyncMaster;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnnotationLoader {
	DataLocation directory;
	WriterUtils.ColumnWriter virtualGroupingIds = new WriterUtils.ColumnWriter();
	WriterUtils.ColumnWriter dbObjectType = new WriterUtils.ColumnWriter();

	class WorkingSet {
		WriterUtils.ColumnWriter dbs = new WriterUtils.ColumnWriter();
		IndexWriter dbIndex;

		WriterUtils.ColumnWriter dbIds = new WriterUtils.ColumnWriter();
		IndexWriter dbIdIndex;

		WriterUtils.ColumnWriter evidences = new WriterUtils.ColumnWriter();
		IndexWriter evidenceIndex;

		WriterUtils.ColumnWriter sources = new WriterUtils.ColumnWriter();
		IndexWriter sourceIndex;

		WriterUtils.ColumnWriter aspects = new WriterUtils.ColumnWriter();
		IndexWriter aspectIndex;

		WriterUtils.ColumnWriter reference = new WriterUtils.ColumnWriter();
		IndexWriter referenceIndex;

		WriterUtils.ColumnWriter refDB = new WriterUtils.ColumnWriter();
		IndexWriter refDBIndex;

		WriterUtils.ColumnWriter refId = new WriterUtils.ColumnWriter();
		IndexWriter refIdIndex;

		WriterUtils.ColumnWriter withString = new WriterUtils.ColumnWriter();

		WriterUtils.ColumnWriter withDB = new WriterUtils.ColumnWriter();
		IndexWriter withDBIndex;

		WriterUtils.ColumnWriter withId = new WriterUtils.ColumnWriter();
		IndexWriter withIdIndex;

		WriterUtils.ColumnWriter qualifier = new WriterUtils.ColumnWriter();
		IndexWriter qualifierIndex;

		WriterUtils.ColumnWriter extraTaxId = new WriterUtils.ColumnWriter();
		WriterUtils.ColumnWriter externalDate = new WriterUtils.ColumnWriter();
		WriterUtils.ColumnWriter splice = new WriterUtils.ColumnWriter();

		IndexWriter termIndex;
		IndexWriter ancestorIndex;

        WriterUtils.ColumnWriter ancestorRelations = new WriterUtils.ColumnWriter();
		IndexWriter ancestorRelationIndex;

		WriterUtils.VGI2RowNumberMap vgi2rowMap = new WriterUtils.VGI2RowNumberMap();

		WorkingSet() throws Exception {
			this.dbIndex = new IndexWriter(directory.annotationDbIndex.file());
			this.dbIdIndex = new IndexWriter(directory.annotationDbIdIndex.file());
			this.evidenceIndex = new IndexWriter(directory.annotationEvidenceIndex.file());
			this.aspectIndex = new IndexWriter(directory.annotationAspectIndex.file());
			this.qualifierIndex = new IndexWriter(directory.annotationQualifierIndex.file());
			this.sourceIndex = new IndexWriter(directory.annotationSourceIndex.file());
			this.referenceIndex = new IndexWriter(directory.annotationReferenceIndex.file());
			this.refDBIndex = new IndexWriter(directory.annotationRefDBIndex.file());
			this.refIdIndex = new IndexWriter(directory.annotationRefIdIndex.file());
			this.withDBIndex = new IndexWriter(directory.annotationWithDBIndex.file());
			this.withIdIndex = new IndexWriter(directory.annotationWithIdIndex.file());
			this.termIndex = new IndexWriter(directory.annotationGOIndex.file());
			this.ancestorIndex = new IndexWriter(directory.annotationAncestorIndex.file());
            this.ancestorRelationIndex = new IndexWriter(directory.annotationAncestorRelationIndex.file());
		}
	}

	WorkingSet workingSet;
	GpCodeSet gpCodeSet;
	IdMap idMap;
	DbObjectMetadataMap dbObjectMetadataMap = new DbObjectMetadataMap();
	TermOntology ontology;
	int[][] ancestry;

	int[] goaRow = new int[AnnotationRow.COLUMN_COUNT];

	int rowNumber;
	WriterUtils.RepeatWriter repeatWriter = new WriterUtils.RepeatWriter();
	DataLocation.IntegerTable annotationFragmented;
	WriterUtils.FragmentedIntegerTableWriter goa;

	public AnnotationLoader(DataLocation directory) throws Exception {
		this.directory = directory;

		this.workingSet = new WorkingSet();
		this.gpCodeSet = new GpCodeSet(directory);
		this.idMap = new IdMap(gpCodeSet);

		this.ontology = new TermOntology(directory);

		this.ancestry = new int[ontology.terms.length][];
		for (int i = 0; i < ontology.terms.length; i++) {
		    List<Term> ancestors = ontology.terms[i].getSlimAncestors();
		    ancestry[i] = new int[ancestors.size()];
		    int c = 0;
		    for (Term v : ancestors) ancestry[i][c++] = v.index();
		}

		this.annotationFragmented = directory.integerTable("annotation");
		this.goa = new WriterUtils.FragmentedIntegerTableWriter(annotationFragmented.file(), AnnotationRow.COLUMN_COUNT);
		this.rowNumber = 0;
	}

	public void compress() throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);
		if (rowNumber > 0) {
			Type annotation = new Type("Annotation", rowNumber);

			repeatWriter.complete(directory.proteinRepeats.file(), virtualGroupingIds.getType(), annotation, virtualGroupingIds.getIndexesSorted());

			workingSet.evidences.write(directory.evidence.file(), "evidence");
			workingSet.sources.write(directory.source.file(), "source");
			workingSet.reference.write(directory.reference.file(), "refrence");
			workingSet.refDB.write(directory.refDB.file(), "ref-db");
			workingSet.refId.write(directory.refId.file(), "ref-id");
			workingSet.withString.write(directory.withString.file(), "with-string");
			workingSet.withDB.write(directory.withDB.file(), "with-db");
			workingSet.withId.write(directory.withId.file(), "with-id");
			workingSet.qualifier.write(directory.qualifier.file(), "qualifier");
			workingSet.aspects.write(directory.aspect.file(), "aspect");
			workingSet.extraTaxId.write(directory.extraTaxId.file(), "extra-tax-id");
			workingSet.externalDate.write(directory.externalDate.file(), "external-date");
			workingSet.splice.write(directory.splice.file(), "splice");
			workingSet.dbs.write(directory.db.file(), "db");
			workingSet.dbIds.write(directory.dbId.file(), "db-id");

			System.out.println("Compressing fragmented annotation file (" + rowNumber + " rows)");
			goa.setValueTranslateMap(virtualGroupingIds.getIndexesTranslated(), null,
				workingSet.evidences.getIndexesTranslated(), workingSet.sources.getIndexesTranslated(), workingSet.reference.getIndexesTranslated(), workingSet.refDB.getIndexesTranslated(), workingSet.refId.getIndexesTranslated(), workingSet.withString.getIndexesTranslated(),
				workingSet.qualifier.getIndexesTranslated(), workingSet.extraTaxId.getIndexesTranslated(), workingSet.externalDate.getIndexesTranslated(), workingSet.splice.getIndexesTranslated(), workingSet.dbs.getIndexesTranslated(), workingSet.dbIds.getIndexesTranslated(), workingSet.aspects.getIndexesTranslated(),
				dbObjectType.getIndexesTranslated()
			);

			goa.compress(annotation, workingSet.vgi2rowMap, virtualGroupingIds.getType(), ontology.termTable.type, workingSet.evidences.getType(), workingSet.sources.getType(), workingSet.reference.getType(), workingSet.refDB.getType(), workingSet.refId.getType(),
				workingSet.withString.getType(), workingSet.qualifier.getType(), workingSet.extraTaxId.getType(), workingSet.externalDate.getType(), workingSet.splice.getType(), workingSet.dbs.getType(), workingSet.dbIds.getType(), workingSet.aspects.getType(),
				dbObjectType.getType()
			);

			Table withStringTable = directory.withString.read().extractColumn(0);
		    Table withDBTable = directory.withDB.read().extractColumn(0);
		    Table withIDTable = directory.withId.read().extractColumn(0);

			IntegerTableReader itr = new IntegerTableReader(annotationFragmented.file());
			IntegerTableReader.Cursor cursor = itr.open();

			int[] row = new int[AnnotationRow.COLUMN_COUNT];
			int rowNumber = 0;

			System.out.println("Defragmenting annotation file");
			while (cursor.read(row)) {
				//System.out.print("row " + rowNumber + ": "); for (int v : row) System.out.print(v + " "); System.out.println();

				workingSet.termIndex.write(rowNumber, row[AnnotationRow.TERM]);
				for (int i : ancestry[row[AnnotationRow.TERM]]) {
					workingSet.ancestorIndex.write(rowNumber, i);
				}

                for (TermRelation ancestor : ontology.terms[row[AnnotationRow.TERM]].getAncestors()) {
                    workingSet.ancestorRelationIndex.write(rowNumber, workingSet.ancestorRelations.add(ancestor.getParentCode()));
                }

				workingSet.evidenceIndex.write(rowNumber, row[AnnotationRow.EVIDENCE]);
				workingSet.sourceIndex.write(rowNumber, row[AnnotationRow.SOURCE]);

				workingSet.referenceIndex.write(rowNumber, row[AnnotationRow.REFERENCE]);
				workingSet.refDBIndex.write(rowNumber, row[AnnotationRow.REF_DB]);
				workingSet.refIdIndex.write(rowNumber, row[AnnotationRow.REF_ID]);

				String ws = withStringTable.read(row[AnnotationRow.WITH_STRING]);
				if (ws != null && !"".equals(ws)) {
					for (String wc: ws.split("\\|")) {
						String[] sa = wc.split(":", 2);
						int ixDB = withDBTable.search(sa[0]);
						if (ixDB >= 0) {
							workingSet.withDBIndex.write(rowNumber, ixDB);
						}
						int ixID = withIDTable.search(sa[1]);
						if (ixID >= 0) {
							workingSet.withIdIndex.write(rowNumber, ixID);
						}
					}
				}

				workingSet.qualifierIndex.write(rowNumber, row[AnnotationRow.QUALIFIER]);
				workingSet.aspectIndex.write(rowNumber, row[AnnotationRow.ONTOLOGY]);

				workingSet.dbIndex.write(rowNumber, row[AnnotationRow.DB]);
				workingSet.dbIdIndex.write(rowNumber, row[AnnotationRow.DB_ID]);

				rowNumber++;
			}
			cursor.close();

			withStringTable = null;
		    withDBTable = null;
		    withIDTable = null;

			System.out.print("Compressing indexes - 1...");
            workingSet.ancestorRelations.write(directory.ancestorRelation.file(),"related");
            workingSet.ancestorRelationIndex.compress(workingSet.ancestorRelations.getType(), annotation, workingSet.ancestorRelations.getIndexesSorted());
			workingSet.ancestorRelations = null;
			workingSet.ancestorRelationIndex = null;

			System.out.print("2...");
			workingSet.termIndex.compress(ontology.termTable.type, annotation);
			workingSet.termIndex = null;
			workingSet.ancestorIndex.compress(ontology.termTable.type, annotation);
			workingSet.ancestorIndex = null;

			System.out.print("3...");
			workingSet.evidenceIndex.compress(workingSet.evidences.getType(), annotation);
			workingSet.evidenceIndex = null;

			System.out.print("4...");
			workingSet.sourceIndex.compress(workingSet.sources.getType(), annotation);
			workingSet.sourceIndex = null;

			System.out.print("5...");
			workingSet.referenceIndex.compress(workingSet.reference.getType(), annotation);

			System.out.print("6...");
			workingSet.refDBIndex.compress(workingSet.refDB.getType(), annotation);
			workingSet.refDBIndex = null;

			System.out.print("7...");
			workingSet.refIdIndex.compress(workingSet.refId.getType(), annotation);
			workingSet.refIdIndex = null;

			System.out.print("8...");
			workingSet.withDBIndex.compress(workingSet.withDB.getType(), annotation);
			workingSet.withDBIndex = null;

			System.out.print("9...");
			workingSet.withIdIndex.compress(workingSet.withId.getType(), annotation);
			workingSet.withIdIndex = null;

			System.out.print("10...");
			workingSet.qualifierIndex.compress(workingSet.qualifier.getType(), annotation);
			workingSet.qualifierIndex = null;

			System.out.print("11...");
			workingSet.aspectIndex.compress(workingSet.aspects.getType(), annotation);
			workingSet.aspectIndex = null;

			System.out.print("12...");
			workingSet.dbIndex.compress(workingSet.dbs.getType(), annotation);
			workingSet.dbIndex = null;

			System.out.print("13...");
			workingSet.dbIdIndex.compress(workingSet.dbIds.getType(), annotation);
			workingSet.dbIdIndex = null;
			System.out.println("done");
		}

		workingSet = null;
		ontology = null;

		System.out.println("compress done: " + mm.end());
	}

	protected Type taxonomyMerge() throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);

		System.out.println("taxonomyMerge started");

	    WriterUtils.TextIndexWriter taxonomyIndex = new WriterUtils.TextIndexWriter(directory.taxonomyIndex);

	    TextTableWriter taxWriter = new TextTableWriter(directory.taxonomy.file(), 2);
	    IntegerTableWriter treeWriter = new IntegerTableWriter(directory.taxonomyTree.file(), 2);

	    IntList rows = new IntList();

	    int[] treeRow = new int[2];
	    for (String[] row : directory.taxonomySource.reader(DataLocation.Taxonomy.TAX_ID, DataLocation.Taxonomy.TREE_LEFT, DataLocation.Taxonomy.TREE_RIGHT, DataLocation.Taxonomy.SCIENTIFIC, DataLocation.Taxonomy.COMMON)) {
	        int taxid = Integer.parseInt(row[0]);
	        taxonomyIndex.index(taxid, row[3] + " " + row[4]);
	        //taxWriter.seek(taxid);
	        rows.set(taxid, taxWriter.write(row[3], row[4]));
	        treeWriter.seek(taxid);
	        treeRow[0] = Integer.parseInt(row[1]);
	        treeRow[1] = Integer.parseInt(row[2]);
	        treeWriter.write(treeRow);
	    }

	    Type taxId = new Type("tax-id", rows.size());
	    Type tree = new Type("tax-tree", (int)(treeWriter.columns[1].max + 1));

	    taxWriter.compress(taxId,rows.getArray(), rows.size());
	    treeWriter.compress(taxId, tree, tree);
	    taxonomyIndex.close(taxId);

		System.out.println("taxonomyMerge done: " + mm.end());
		return taxId;
	}

	protected void idMerge() throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);

		System.out.println("idMerge - load mapping files");
		for (DataLocation.NamedFile nf : directory.getMappingFiles()) {
			File f = nf.file();
			if (f.exists()) {
				idMap.load(f, true);
			}
		}
		System.out.println("idMerge - load protein set files");
		for (DataLocation.NamedFile nf : directory.getProteinSetFiles()) {
			File f = nf.file();
			if (f.exists()) {
				idMap.load(f, false);
			}
		}
		gpCodeSet.write();

		System.out.println("Invert idMap");
		IdMap.UniProt2IdentifierMap inverse = idMap.invert();
		System.out.println("Discard idMap");
		idMap = null;

		Table proteinTable = new TextTableReader(directory.virtualGroupingIds.file()).extractColumn(0);
		ProteinXrefs pxr = new ProteinXrefs(proteinTable, directory);
		proteinTable = null;

		for (String uniProtAc : inverse.keySet()) {
			ArrayList<String> proteins = inverse.get(uniProtAc);
			for (String protein : proteins) {
				pxr.set(uniProtAc, protein);
			}
		}

		inverse = null;
		System.out.println("Compress inverse map");
		pxr.compress();
		System.out.println("idMerge done: " + mm.end());
	}

	protected void indexMetadata(Type taxonomy) throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);

		System.out.println("indexMetadata started");
		Progress p = new Progress("indexMetadata");

		virtualGroupingIds.write(directory.virtualGroupingIds.file(), "protein");
		dbObjectMetadataMap.write(directory);
		dbObjectType.write(directory.dbObjectType.file(), "db-object-type");

        TextTableReader proteinTable = new TextTableReader(directory.virtualGroupingIds.file());
        SyncMaster sync = new SyncMaster(proteinTable.extractColumn(0));

        Type proteins = new Type("protein", proteinTable.size());

        WriterUtils.TextIndexWriter proteinNames = new WriterUtils.TextIndexWriter(directory.dbObjectNameIndex);
		WriterUtils.IndexColumnWriter proteinSymbols = new WriterUtils.IndexColumnWriter(directory.proteinSymbols, directory.proteinSymbolIndex);
		WriterUtils.IndexColumnWriter proteinSynonyms = new WriterUtils.IndexColumnWriter(directory.proteinSynonyms, directory.proteinSynonymIndex);
		IntegerTableWriter taxTab = new IntegerTableWriter(directory.proteinTaxonomy.file(), 1);

		System.out.println("dbObjectMetadataMap keySet size = " + dbObjectMetadataMap.keySet().size());
		int[] taxRow = new int[1];
		for (String vgi : dbObjectMetadataMap.keySet()) {
			p.next();
			DbObjectMetadataMap.DbObjectMetadataMapEntry m = dbObjectMetadataMap.get(vgi);
			int protein = sync.find(vgi);
			if (protein >= 0) {
				proteinNames.index(protein, m.getDbObjectName());
				proteinSymbols.index(protein, m.getDbObjectSymbol());
				proteinSynonyms.index(protein, m.getDbObjectSynonym());

				taxTab.seek(protein);
				int taxonId = m.getTaxonId();
				taxRow[0] = (taxonId >= taxonomy.cardinality) ? 0 : taxonId;
				taxTab.write(taxRow);
			}
			dbObjectMetadataMap.clear(vgi);
		}

		dbObjectMetadataMap = null;
		//virtualGroupingIds = null;

		System.out.println("Compress proteinNames");
        proteinNames.close(proteins);
		System.out.println("Compress proteinSymbols");
		proteinSymbols.close(proteins);
		System.out.println("Compress proteinSynonyms");
		proteinSynonyms.close(proteins);
		taxTab.seek(proteins.cardinality);
	    taxTab.compress(proteins, taxonomy);

		p.end();
		System.out.println("indexMetadata done: " + mm.end());
	}

	public boolean loadAnnotation(AnnotationFile af) throws Exception {
		Term term = ontology.getTerm(af.goId);
		if (term != null) {
			goaRow[AnnotationRow.VIRTUAL_GROUPING_ID] = virtualGroupingIds.add(af.virtualGroupingId);
			repeatWriter.write(goaRow[AnnotationRow.VIRTUAL_GROUPING_ID]);

			goaRow[AnnotationRow.TERM] = term.index();
			goaRow[AnnotationRow.ONTOLOGY] = workingSet.aspects.add(term.aspect.abbreviation);

			goaRow[AnnotationRow.EVIDENCE] = workingSet.evidences.add(af.evidence);
			goaRow[AnnotationRow.SOURCE] = workingSet.sources.add(af.assignedBy);
			goaRow[AnnotationRow.REFERENCE] = workingSet.reference.add(af.reference);
			goaRow[AnnotationRow.REF_DB] = workingSet.refDB.add(af.refDBCode);
			goaRow[AnnotationRow.REF_ID] = workingSet.refId.add(af.refDBId);

			goaRow[AnnotationRow.WITH_STRING] = workingSet.withString.add(af.withString);
			if (af.withString != null && !"".equals(af.withString)) {
				for (String wc : af.withString.split("\\|")) {
					String[] sa = wc.split(":", 2);
					workingSet.withDB.add(sa[0]);
					workingSet.withId.add(sa[1]);
				}
			}

			goaRow[AnnotationRow.QUALIFIER] = workingSet.qualifier.add(af.qualifier);

			goaRow[AnnotationRow.EXTRA_TAXID] = workingSet.extraTaxId.add(af.extraTaxId);
			goaRow[AnnotationRow.EXTERNAL_DATE] = workingSet.externalDate.add(af.date);
			goaRow[AnnotationRow.SPLICE] = workingSet.splice.add(af.spliceformId);

			gpCodeSet.register(af.db, true);
			goaRow[AnnotationRow.DB] = workingSet.dbs.add(af.db);
			goaRow[AnnotationRow.DB_ID] = workingSet.dbIds.add(af.dbObjectId);

		    goa.write(goaRow);

			workingSet.vgi2rowMap.insert(af.virtualGroupingId, goaRow[AnnotationRow.VIRTUAL_GROUPING_ID], rowNumber++);
			return true;
		}
		else {
			return false;
		}
	}

	public void loadMetadata(String virtualGroupingId, String dbObjectName, String dbObjectSymbol, String dbObjectSynonym, String taxId, String dbObjectType) {
		virtualGroupingIds.add(virtualGroupingId);
		dbObjectMetadataMap.set(virtualGroupingId, dbObjectName, dbObjectSymbol, dbObjectSynonym, taxId, this.dbObjectType.add(dbObjectType));
	}

	public static int loadData(DataLocation directory) throws Exception {
		MemoryMonitor mm = new MemoryMonitor(true);

		ArrayList<DataLocation.NamedFile> gafList = new ArrayList<DataLocation.NamedFile>();
		ArrayList<DataLocation.NamedFile> gpaList = new ArrayList<DataLocation.NamedFile>();
		ArrayList<DataLocation.NamedFile> gpiList = new ArrayList<DataLocation.NamedFile>();
		ArrayList<DataLocation.NamedFile> gp2List = new ArrayList<DataLocation.NamedFile>();

		for (DataLocation.NamedFile f : directory.getGPDataFiles()) {
			String fn = f.getName();
			if (fn.startsWith("gene_association")) {
				gafList.add(f);
			}
			else if (fn.startsWith("gp_association")) {
				gpaList.add(f);
			}
			else if (fn.startsWith("gp_information")) {
				gpiList.add(f);
			}
			else if (fn.startsWith("gp2protein")) {
				gp2List.add(f);
			}
		}

		AnnotationLoader al = new AnnotationLoader(directory);

		for (DataLocation.NamedFile f : gpiList) {
			new GPInformationFile(al.idMap, f).load(al);
		}

		al.indexMetadata(al.taxonomyMerge());

		int count = 0;

		for (DataLocation.NamedFile f : gp2List) {
			al.idMap.load(f.file(), true);
		}
		for (DataLocation.NamedFile f : gafList) {
			count += new GeneAssociationFile(al.idMap, f).load(al);
		}
		for (DataLocation.NamedFile f : gpaList) {
			count += new GPAssociationFile(al.idMap, f).load(al);
		}

		if (count > 0) {
			al.compress();
		}

		al.idMerge();

		System.out.println("loadData done: " + mm.end());
		return count;
	}
}
