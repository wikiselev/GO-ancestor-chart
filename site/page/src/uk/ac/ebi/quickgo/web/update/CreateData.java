package uk.ac.ebi.quickgo.web.update;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.MemoryMonitor;
import uk.ac.ebi.interpro.exchange.*;
import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;
import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.quickgo.web.configuration.WriterUtils.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.*;
import java.net.*;
import java.text.*;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

public class CreateData {
    private static String newVersion() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    }

    private static String latestVersion(File base) {
        String version = "";
        for (File f : base.listFiles()) {
            if (f.isDirectory() && f.getName().compareTo(version) > 0) {
	            version = f.getName();
            }
        }
        return version;
    }

    public static void main(String[] args) throws Exception {
        String command = args[0];

        String baseName = args[1];
        File base = new File(baseName);
        String version = args[2];
        String origin = args[3];
        String archiveBase = args[4];

        if (version.equals("new")) {
	        version = newVersion();
        }
        else if (version.equals("latest")) {
	        version = latestVersion(base);
        }
	    File dataDir = new File(base, version);

        System.out.println("Action    : " + command);
        System.out.println("Temp Files: " + base);
        System.out.println("Version   : " + version);
        System.out.println("Data from : " + origin);
        System.out.println("Archive   : " + archiveBase);

	    DataLocation dataLocation = new DataLocation(dataDir);
        if (command.equals("archive")) {
	        archive(dataLocation, archiveBase);
        }
        else if (command.equals("download")) {
	        download(dataLocation, origin);
        }
        else if (command.equals("update")) {
	        update(dataLocation, origin, archiveBase);
        }
        else if (command.equals("calculate")) {
	        calculate(dataLocation);
        }
        else if (command.equals("initialise")) {
	        initialise(dataLocation);
        }
        else if (command.equals("index_annotation")) {
	        indexAnnotation(dataLocation);
        }
        else if (command.equals("index_term")) {
	        indexTerms(dataLocation);
        }
        else if (command.equals("index_protein")) {
	        indexProtein(dataLocation);
        }
        else if (command.equals("merge_sequences")) {
	        sequenceMerge(dataLocation);
        }
        else if (command.equals("merge_ref_pubmed")) {
	        mergeRefPubmed(dataLocation);
        }
        else if (command.equals("merge_with_interpro")) {
	        mergeWithInterPro(dataLocation);
        }
        else if (command.equals("merge_with_protein")) {
	        mergeWithProtein(dataLocation);
        }
/*
        else if (command.equals("get_gonuts_status")) {
	        getTermGONUTSStatus(dataLocation);
        }
*/
    }

    private static void zipCopy(File file,String name,ZipOutputStream target) throws IOException {
        System.out.println("zipCopy: Writing " + name);
        ZipEntry ze = new ZipEntry(name);
        target.putNextEntry(ze);
        InputStream is = new FileInputStream(file);
        IOUtils.copy(is,target);
        is.close();
        target.closeEntry();
    }

	private static void zipCopy(ZipInputStream srcZip, ZipOutputStream targetZip) throws IOException {
	    ZipEntry ze;
	    while ((ze = srcZip.getNextEntry()) != null) {
	        System.out.println("zipCopy: Copying " + ze.getName());
	        if (!ze.isDirectory()) {
		        targetZip.putNextEntry(ze);
		        IOUtils.copy(srcZip, targetZip);
		        targetZip.closeEntry();
	        }
	    }
	}

    private static void archive(DataLocation directory, String archiveBase) throws Exception {
        System.out.println("archive: Starting");
        
        NamedFile[] archiveSource = directory.archiveFiles();
        File base = new File(archiveBase);
        String version = directory.getBase().getName();

        ZipOutputStream dataZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(base, version + ".zip"))));
        long size = 0;
        for (NamedFile f : archiveSource) {
            size += f.file().length();
            zipCopy(f.file(), f.getName(), dataZip);
        }
        dataZip.close();

        File stampFile = new File(base, DataLocation.stampName);
        new Stamp(version,size).write(stampFile);
        
        File srcZipFile = new File(base, "QuickGO5.1.zip");
        if (srcZipFile.exists()) {
            ZipInputStream srcZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(srcZipFile)));
            ZipOutputStream fullZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(base, "QuickGO5.1-" + version + ".zip"))));
            zipCopy(srcZip, fullZip);
            for (NamedFile f : archiveSource) {
	            zipCopy(f.file(), archiveBase + "/" + version + "/" + f.getName(), fullZip);
            }
            zipCopy(stampFile, archiveBase + "/" + DataLocation.stampName, fullZip);
            srcZip.close();
            fullZip.close();
        }

        System.out.println("archive: Completed");
    }

	private static void update(DataLocation directory, String from, String archiveBase) throws Exception {
		download(directory, from);
		calculate(directory);
		archive(directory, archiveBase);
	}

	private static void calculate(DataLocation directory) throws Exception {
	    initialise(directory);
        indexAnnotation(directory);
		indexProtein(directory);
		indexTerms(directory);
		mergeRefPubmed(directory);
		mergeWithInterPro(directory);
		mergeWithProtein(directory);
		sequenceMerge(directory);
	}

    private static void initialise(DataLocation directory) throws Exception {
        System.out.println("initialise: Term scan");

        ColumnWriter terms = new ColumnWriter();

        for (String[] id : directory.goTermNames.reader(GOTermNames.GO_ID)) {
	        terms.add(id[0]);
        }

        System.out.println("initialise: Term table write");

        terms.write(directory.termIDs.file(), "term");

	    System.out.println("initialise: Term processing complete");
    }

	private static void downloadFiles(NamedFile[] fileList, String from, boolean mustExist) throws IOException {
		if (fileList != null) {
			URL base = from.contains(":") ? new URL(from) : new File(from).toURI().toURL();
			for (NamedFile f : fileList) {
				String text = f.getName();
				System.out.print(text);
				try {
					IOUtils.copy(new URL(base, text), f.file());
				}
				catch (IOException e) {
					if (mustExist) {
						throw e;
					}
					else {
						System.out.print(" - SOURCE FILE NOT FOUND");
					}
				}
				System.out.println();
			}
		}
	}

	private static void download(DataLocation directory, String from) throws Exception {
		System.out.println("download: from " + from);
		if (!directory.getBase().mkdirs()) {
			throw new IOException("Unable to download files");
		}

		downloadFiles(directory.requiredFiles(), from, true);
		downloadFiles(directory.getGPDataFiles(), from, true);
		downloadFiles(directory.getMappingFiles(), from, false);
		downloadFiles(directory.getProteinSetFiles(), from, false);

		//getTermGONUTSStatus(directory);

		System.out.println("download: Completed");
	}

    static int getLimit(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("+")) {
                return Integer.parseInt(arg.substring(1));
            }
        }
        return Integer.MAX_VALUE;

    }

    static RowReader.MonitorRowReader limit(String[] args,RowReader.MonitorRowReader source) {
        int limit = getLimit(args);
        return (limit < Integer.MAX_VALUE) ? new CodeList.Head(getLimit(args),source) : source;
    }

    private static void indexProtein(DataLocation directory) throws Exception {
	    System.out.println("indexProtein: Started");
	    MemoryMonitor mm = new MemoryMonitor(true);

        TextTableReader proteinTable = new TextTableReader(directory.virtualGroupingIds.file());
        SyncMaster sync = new SyncMaster(proteinTable.extractColumn(0));

        Type proteins = new Type("protein", proteinTable.size());

		TextTableReader.Cursor metadataReader = new TextTableReader(directory.proteinMetadata.file()).open();

		TextTableWriter proteinInfoTable = new TextTableWriter(directory.proteinInfo.file(), 3);
		TextIndexWriter proteinDescriptions = new TextIndexWriter(directory.proteinDescriptions);
		DataIndex genes = new DataIndex("protein-gene", directory.proteinGenes, directory.proteinGeneIndex);
		TextIndexWriter proteinNames = new TextIndexWriter(directory.proteinNames);

		int[] rows = new int[sync.type.cardinality];

		while (true) {
			String[] row  = metadataReader.read();
			if (row == null) {
				break;
			}
			else {
				int ixProtein = sync.find(row[DbObjectMetadataMap.ixVirtualGroupingId]);
				if (ixProtein >= 0) {
					String gene = StringUtils.nvl(row[DbObjectMetadataMap.ixSymbol]);
					String description = StringUtils.nvl(row[DbObjectMetadataMap.ixName]);
					// assume that the first element in the list of synonyms is the protein's UniProt name
					String name = row[DbObjectMetadataMap.ixSynonym].split("\\|", 2)[0];

					rows[ixProtein] = proteinInfoTable.write(gene, description, name);

					genes.write(ixProtein, gene);
					proteinDescriptions.index(ixProtein, description);
					if (name != null) {
						proteinNames.index(ixProtein, name);
					}
				}
			}
		}

		metadataReader = null;

		genes.compress(proteins);
		proteinInfoTable.compress(proteins, rows, rows.length);
		proteinDescriptions.close(proteins);
		proteinNames.close(proteins);

	    System.out.println("indexProtein: Done - " + mm.end());
    }

	private static void sequenceMerge(DataLocation directory) throws Exception {
		System.out.println("sequenceMerge: Started");
		MemoryMonitor mm = new MemoryMonitor(true);

        RowIterator sequences = directory.sequenceSource.reader(Sequence.protein, Sequence.sequence);

        Table proteinTable = new TextTableReader(directory.virtualGroupingIds.file()).extractColumn(0);

        SyncMaster sync = new SyncMaster(proteinTable);

        TextTableWriter sequenceWriter = new TextTableWriter(directory.proteinSequences.file(), 1);

        int[] rows=new int[proteinTable.size()];

        if (sequenceWriter.write("") != 0) {
	        throw new IOException("Not at start of spool file");
        }

        for (String[] row : sequences) {
            int protein = sync.find(row[0]);
            if (protein >= 0) {
	            rows[protein] = sequenceWriter.write(row[1]);
            }
        }
        sequenceWriter.compress(sync.type, rows, rows.length);

		System.out.println("sequenceMerge: Done - " + mm.end());
    }

    static class DataIndex {
        IndexWriter index;
        private IndexSort<String> symbols;
        private TextTableWriter table;
        private String name;
        IndexSort.CollatingStringComparator collator;

        DataIndex(String name, DataLocation.TextTable symbols, DataLocation.Index index) throws IOException {
            this.name = name;

            this.index = new IndexWriter(index.file());
            this.table = new TextTableWriter(symbols.file(), 1);

            collator = IndexSort.caseless;
            this.symbols = new IndexSort<String>(collator);
        }

        void write(int rownum,String value) throws IOException {
            if (rownum >= 0) {
	            index.write(rownum, symbols.add(value));
            }
        }

        void compress(Type to) throws IOException {
            Type symbolType = new Type(name, symbols.size());
            table.compressSortedSingle(symbolType, symbols.sortedValues(), collator.collation);
            index.compress(symbolType, to, symbols.sortedIndexes());
        }
    }

    public static void indexAnnotation(DataLocation directory) throws Exception {
        if (AnnotationLoader.loadData(directory) == 0) {
            throw new Exception("No annotation source files in " + directory.getBase().getAbsolutePath());
        }
    }

    private static void indexTerms(DataLocation directory) throws Exception {
	    System.out.println("indexTerms");
	    MemoryMonitor mm = new MemoryMonitor(true);

        Table termTable = new TextTableReader(directory.termIDs.file()).extractColumn(0);

        SyncMaster sync = new SyncMaster(termTable);
	    TextIndexWriter tiw = new TextIndexWriter(directory.termNames,true);
        for (String[] row : directory.goTermNames.reader(GOTermNames.GO_ID, GOTermNames.NAME)) {
	        tiw.index(sync.find(row[0]), row[1]);
        }
	    tiw.close(termTable.type);

        sync.reset();
	    tiw = new TextIndexWriter(directory.termComments,true);
        for (String[] row : directory.goComments.reader(GOComments.TERM_ID, GOComments.COMMENT_TEXT)) {
	        tiw.index(sync.find(row[0]), row[1]);
        }
	    tiw.close(termTable.type);

        sync.reset();
	    DataIndex xref = new DataIndex("term-xref",directory.termXrefs,directory.termXrefIndex);
        for (String[] row : directory.goXrefs.reader(GOXref.TERM_ID, GOXref.DB_ID)) {
	        xref.write(sync.find(row[0]), row[1]);
        }
	    xref.compress(termTable.type);

        sync.reset();
	    tiw = new TextIndexWriter(directory.termSynonyms,true);
        for (String[] row : directory.goTermSynonyms.reader(GOTermSynonyms.TERM_ID, GOTermSynonyms.NAME)) {
	        tiw.index(sync.find(row[0]), row[1]);
        }
	    tiw.close(termTable.type);

	    sync.reset();
	    tiw = new TextIndexWriter(directory.termDefinitions,true);
	    for (String[] row : directory.goTermDefinitions.reader(GOTermDefinitions.TERM_ID, GOTermDefinitions.DEFINITION)) {
		    tiw.index(sync.find(row[0]), row[1]);
	    }
		tiw.close(termTable.type);

	    System.out.println("indexTerms done - " + mm.end());
    }

    private static void mergeRefPubmed(DataLocation directory) throws Exception {
	    System.out.println("mergeRefPubmed: Started");
	    MemoryMonitor mm = new MemoryMonitor(true);

        SyncMaster sync = new SyncMaster(new TextTableReader(directory.refId.file()).extractColumn(0));

        RowIterator pubmed = directory.pubmed.reader(Pubmed.PUBMED_ID, Pubmed.TITLE);

        IntegerTableWriter refIDpubmed = new IntegerTableWriter(directory.pubmedRef.file(), 1);
        TextTableWriter pubmedWriter = new TextTableWriter(directory.pubmedInfo.file(), 2);
        TextIndexWriter pubmedTitle = new TextIndexWriter(directory.pubmedTitle);

        // A null entry, referred to by references which didn't match any listed publication
        int rowNumber = pubmedWriter.write("","");
        for (String[] row: pubmed) {
            int refID = sync.find(row[0]);
            if (refID >= 0) {
	            refIDpubmed.seek(refID);
	            rowNumber = pubmedWriter.write(row);
	            refIDpubmed.write(rowNumber);
	            pubmedTitle.index(rowNumber, row[0] + " " + row[1]);
            }
        }
		refIDpubmed.seek(sync.type.cardinality);
        Type pubmedType = new Type("pubmed", rowNumber + 1); // rowNumber is 0-based

        refIDpubmed.compress(sync.type, pubmedType);
        pubmedWriter.compress(pubmedType);

        pubmedTitle.close(pubmedType);

	    System.out.println("mergeRefPubmed: Done - " + mm.end());
    }

    private static void mergeWithInterPro(DataLocation directory) throws Exception {
	    System.out.println("mergeWithInterPro: Started");
	    MemoryMonitor mm = new MemoryMonitor(true);

        SyncMaster sync = new SyncMaster(new TextTableReader(directory.withId.file()).extractColumn(0));

        RowIterator interpro = directory.interpro.reader(InterPro.ENTRY_AC, InterPro.NAME);

        IntegerTableWriter withIDinterpro = new IntegerTableWriter(directory.interproWith.file(), 1);
        TextTableWriter interproWriter = new TextTableWriter(directory.interproInfo.file(), 2);
        TextIndexWriter interproIndex = new TextIndexWriter(directory.interproIndex);

        // A null entry, referred to by with strings which didn't match any InterPro accessions
        int rowNumber = interproWriter.write("", "");

        for (String[] row: interpro) {
            int withID = sync.find(row[0]);
            if (withID >= 0) {
	            withIDinterpro.seek(withID);

	            rowNumber = interproWriter.write(row);

	            withIDinterpro.write(rowNumber);
	            interproIndex.index(rowNumber, row[0] + " " + row[1]);
            }
        }
		withIDinterpro.seek(sync.type.cardinality);

        Type interproType = new Type("interpro", rowNumber + 1); // rowNumber is 0-based

        withIDinterpro.compress(sync.type, interproType);
        interproWriter.compress(interproType);

        interproIndex.close(interproType);

	    System.out.println("mergeWithInterPro: Done - " + mm.end());
    }

    private static void mergeWithProtein(DataLocation directory) throws Exception {
	    System.out.println("mergeWithProtein: Started");
	    MemoryMonitor mm = new MemoryMonitor(true);

        List<Closeable> connection = new ArrayList<Closeable>();

        TextTableReader withID = new TextTableReader(directory.withId.file());
        TextTableReader.Cursor cursor = withID.open(connection);

        IntegerTableWriter withIDprotein = new IntegerTableWriter(directory.proteinWith.file(), 1);

        Table proteinTable = new TextTableReader(directory.virtualGroupingIds.file()).extractColumn(0);
        String[] row;
        while ((row = cursor.read()) != null) {
            int proteinCode = proteinTable.search(row[0]);
            if (proteinCode < 0) {
	            proteinCode = 0;
            }
            withIDprotein.write(proteinCode);
        }

        withIDprotein.compress(withID.rowType, proteinTable.type);

        IOUtils.closeAll(connection);

	    System.out.println("mergeWithProtein: Done - " + mm.end());
    }

/*
	private static void getTermGONUTSStatus(DataLocation directory) throws Exception {
		System.out.println("getTermGONUTSStatus: Started");

		// get a list of those GO terms (and slims) whose GONUTS wiki pages have been modified
		URL u = new URL("http://gowiki.tamu.edu/rest/is_edited.php?page=GO:%&exclude=0");

		String[] columns = new String[] { "GO_ID" };
		TSVRowWriter termWriter = new TSVRowWriter(directory.gonutsEditedTerms.file(), columns, true, true, null);
		termWriter.open();
		TSVRowWriter slimWriter = new TSVRowWriter(directory.gonutsEditedSlims.file(), columns, true, true, null);
		slimWriter.open();

		// the GONUTS server might be unavailable, or it might return something that is not valid XML, so...
		try {
			// connect
			URLConnection urlConnection = u.openConnection();

			// parse the XML document returned by the connection
			InputStream inputStream = urlConnection.getInputStream();
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
			inputStream.close();

			Pattern goTermPattern = Pattern.compile("Category:(GO:[0-9]{7})(_!_.*)?");
			Matcher goTermMatcher = goTermPattern.matcher("");

			Pattern goSlimPattern = Pattern.compile("Category:GO:(goslim_(.*))");
			Matcher goSlimMatcher = goSlimPattern.matcher("");


			// use XPath to find all page_title nodes in the document, as these contain the name of the GO term
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList)xpath.evaluate("//revision_data/page_title/text()", xml, XPathConstants.NODESET);

			String[] sa = new String[1];
			for (int i = 0; i < nodes.getLength(); i++) {
				String text = nodes.item(i).getNodeValue();
				goTermMatcher.reset(text);
				if (goTermMatcher.matches()) {
					sa[0] = goTermMatcher.group(1);
					termWriter.write(sa);
				}
				else {
					goSlimMatcher.reset(text);
					if (goSlimMatcher.matches()) {
						sa[0] = goSlimMatcher.group(1);
						slimWriter.write(sa);
					}
					else {
						//System.out.println(text + " => No Match");
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println("GONUTS update failed");
		}

		termWriter.close();
		slimWriter.close();
		System.out.println("getTermGONUTSStatus: Done");
	}
*/
}
