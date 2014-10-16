package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.quickgo.web.data.TextSearch;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

public class WriterUtils {
	public static class ColumnWriter {

        DataLocation.TextTable column;

        public ColumnWriter(DataLocation.TextTable column) {
            this.column = column;
        }

        public ColumnWriter() {
        }

        Type type;
	    IndexSort.CollatingStringComparator collator = IndexSort.caseless;
	    IndexSort<String> values = new IndexSort<String>(collator);
	    private int[] indexesSorted = null;
	    private int[] indexesTranslated = null;

		public Type getType() {
			return type;
		}

		public int add(String value) {
	        return values.add(value);

	    }

	    public int search(String s) {
	        return values.search(s);
	    }

        public void write(String name) throws IOException {
            write(column.file(),name);
        }

	    public void write(File f,String name) throws IOException {
	        type = new Type(name, values.size());

	        new TextTableWriter(f,1).compressSortedSingle(type, values.sortedValues(), collator.collation);
	    }

		public int[] getIndexesSorted() {
			if (indexesSorted == null) {
				indexesSorted = values.sortedIndexes();
			}
			return indexesSorted;
		}

		public int[] getIndexesTranslated() {
			if (indexesTranslated == null) {
				indexesTranslated = values.indexesTranslate();
			}
			return indexesTranslated;
		}
	}

	public static class RepeatWriter {
		IntList repeats = new IntList();

	    public void write(int code) {
		    repeats.set(code, repeats.get(code) + 1);
	    }

	    public void complete(File f, Type key, Type rows, int[] sortedIndexes) throws IOException {
	        IntegerTableWriter repeatWriter = new IntegerTableWriter(f, 1);
	        for (int index : sortedIndexes) repeatWriter.write(repeats.get(index));
	        repeatWriter.compress(key, rows);
	    }
	}

	public static class VGIRowNumberList extends ArrayList<Integer> {
		int ixVirtualGroupingId;

		public VGIRowNumberList(int ixVirtualGroupingId) {
			this.ixVirtualGroupingId = ixVirtualGroupingId;
		}

		public int getIxVirtualGroupingId() {
			return ixVirtualGroupingId;
		}
	}

 	static class VGI2RowNumberMap extends TreeMap<String, VGIRowNumberList> {
		int size = 0;

		public void insert(String vgi, int vgiIndex, int rowNumber) {
			VGIRowNumberList vrnl = get(vgi);
			if (vrnl == null) {
				vrnl = new VGIRowNumberList(vgiIndex);
				put(vgi, vrnl);
			}
			vrnl.add(rowNumber);
			size++;
		}

		public void dump(String fileName) throws Exception {
			PrintStream ps = (fileName == null) ? System.out : new PrintStream(fileName);

			ps.println("Total size = " + size);

			for (String s : keySet()) {
				VGIRowNumberList vrnl = get(s);
				ps.print("[" + s + " (" + vrnl.getIxVirtualGroupingId() + ")]: ");
				for (Integer i : vrnl) {
					ps.print(i + " ");
				}
				ps.println();
			}

			if (fileName != null) {
				ps.close();
			}
		}

		public int getSize() {
			return size;
		}

		public int[] flatten() {
			int[] ia = new int[size];
			int ix = 0;
			for (String s : keySet()) {
				for (Integer i : get(s)) {
					ia[ix++] = i;
				}
			}
			return ia;
		}

		public int[] sort() {
			int[] ia = new int[size];
			int ix = 0;
			for (String s : keySet()) {
				for (Integer i : get(s)) {
					ia[i] = ix++;
				}
			}
			return ia;
		}
	}

	public static class FragmentedIntegerTableWriter extends IntegerTableWriter {
		public FragmentedIntegerTableWriter(File f, int columnCount) throws IOException {
			super(f, columnCount);
		}

		public void compress(Type rowType, VGI2RowNumberMap map, Type... types) throws IOException {
		    spool.rewind();
		    BitWriter bw=new BitWriter(file);
		    format.writeVersion(bw, Version.INITIAL);
		    bw.writeInt(sorted?1:0);
		    bw.writeInt(columnCount);
		    bw.writeInt(rowCount);
		    rowType.write(bw);
		    for (int i = 0; i < columnCount; i++) {
		        columns[i].max = types[i].cardinality - 1;
		        columns[i].finish();
		        types[i].write(bw);
		        bw.writeInt(columns[i].bits);
		        System.out.println("Row " + i + " " + columns[i].totalBitSize);
		    }

		    format.writeCheckPoint(bw, Magic.START);

		    System.out.println("Size: " + rowCount + " " + bw.bitCount());

			for (String s : map.keySet()) {
				for (Integer r : map.get(s)) {
					int[] allColumns = spool.read(r);
					for (int k = 0; k < allColumns.length; k++) {
						int value = allColumns[k];
					    if (valueTranslateMap != null && valueTranslateMap[k] != null) {
						    value = valueTranslateMap[k][value];
					    }
					    columns[k].write(bw, value);
					}
				}
			}

		    spool.close();
		    System.out.println("end:"+bw.bitCount());
		    format.writeCheckPoint(bw, Magic.END);

		    bw.close();

			if (valueTranslateMap != null) {
			    for (int k=0; k < valueTranslateMap.length; k++) {
			        valueTranslateMap[k] = null;
			    }
				valueTranslateMap = null;
			}
		}
	}


    public static class IndexColumnWriter {

        ColumnWriter column;


		IndexWriter valueIndex;


		public IndexColumnWriter(DataLocation.TextTable text,DataLocation.Index index) throws IOException {
            column=new ColumnWriter(text);
			valueIndex = new IndexWriter(index.file());
		}

	    public void index(int rownum,String word) throws IOException {
		    if (rownum<0) return;

			int code=column.add(word);
			valueIndex.write(rownum,code);
	    }

	    public void index(int rownum,String[] values) throws IOException {
		    if (rownum<0) return;

		    for (String word : values) {
                int code=column.add(word);
			    valueIndex.write(rownum,code);
		    }
	    }

	    public void index(int rownum, ArrayList<String> values) throws IOException {
		    if (rownum<0) return;

		    for (String word : values) {
                int code=column.add(word);
			    valueIndex.write(rownum,code);
		    }
	    }

		public void close(Type to) throws IOException {

            column.write("words");
            valueIndex.compress(column.getType(),to,column.getIndexesSorted());
		}

	}

	public static class TextIndexWriter {


        boolean autoStem;
        ColumnWriter column;


		IndexWriter wordIndex;
		IndexWriter pairIndex;
		IndexSort<int[]> pairs=new IndexSort<int[]>(CollectionUtils.intArrayComparator);
		private IntegerTableWriter pairWriter;

        public TextIndexWriter(DataLocation.TextIndexColumn index,boolean autoStem) throws IOException {
            this(index);
            this.autoStem = autoStem;

        }

		public TextIndexWriter(DataLocation.TextIndexColumn index) throws IOException {
            column=new ColumnWriter(index.words);
			wordIndex = new IndexWriter(index.wordIndex.file());
			pairIndex = new IndexWriter(index.pairIndex.file());
			pairWriter = new IntegerTableWriter(index.pairs.file(),2);
		}

		public void index(int rownum,String text) throws IOException {
			if (rownum<0) return;
			String[] words= TextSearch.split(text.toLowerCase());
			int prev=-1;
			for (String word : words) {
				int code=column.add(word);
				if (prev!=-1) pairIndex.write(rownum,pairs.add(new int[]{prev,code}));
                if (autoStem) {
                    for (int i=1;i<=word.length();i++) wordIndex.write(rownum,column.add(word.substring(0,i)));
                } else {
                    wordIndex.write(rownum,code);
                }                
				prev=code;
			}
		}

		public void close(Type to) throws IOException {
            column.write("words");
			IndexSort<int[]> pairsTranslated = translateWordPairs(column.getIndexesTranslated());

			Type proteinWords = column.getType();
			Type proteinPairs = new Type("pairs", pairs.size());

			wordIndex.compress(proteinWords,to,column.getIndexesSorted());

			pairWriter.compressSorted(proteinPairs,pairsTranslated.sortedValues(),proteinWords,proteinWords);

			pairIndex.compress(new Type("pairs",pairs.size()),to, pairsTranslated.sortedIndexes());
		}

		private IndexSort<int[]> translateWordPairs(int[] wordTranslateIndexes) {
			IndexSort<int[]> pairsTranslated=new IndexSort<int[]>(CollectionUtils.intArrayComparator);
			for (Map.Entry<int[], Integer> entry : pairs.map.entrySet()) {
				int[] pair=entry.getKey();
				pair[0]=wordTranslateIndexes[pair[0]];
				pair[1]=wordTranslateIndexes[pair[1]];
				pairsTranslated.map.put(pair, entry.getValue());
			}
			return pairsTranslated;
		}
	}
}
