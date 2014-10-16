package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.exchange.compress.find.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.quickgo.web.configuration.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TextSearch {
    private static Location me=new Location();

//  private static Pattern split= Pattern.compile("[- \r\n\t]+");
	private static Pattern split= Pattern.compile("[ \r\n\t]+");
	private static Pattern clean= Pattern.compile("^[^A-Za-z0-9]*|[^A-Za-z0-9*]*$");

    static String[] empty={};


    public static String[] split(String text) {
        List<String> results=new ArrayList<String>();
        for (String s : split.split(text)) {
            String cleaned=clean.matcher(s).replaceAll("");
            if (cleaned.length()==0) continue;
            results.add(cleaned);
        }
        return results.toArray(new String[results.size()]);
    }

    

    public static class SearchField {
        public int score;
        public String name;

        public SearchField(int score,String name) {
            this.score = score;
            this.name = name;
        }

    }

    public static boolean findField(List<Match> matches, SearchField field) {
        if (matches==null) return false;
           for (Match word : matches) if (word!=null && word.field==field) return true;
           return false;
       }





    public static class Match implements Find,Comparable<Match> {

        public static Comparator<Match> reverseFrequencyOrder=new Comparator<Match>(){
            public int compare(Match m1, Match m2) {
                return m2.frequencyScore-m1.frequencyScore;
            }
        };


        /*private FieldScoreCard card;*/
        public final Find find;
        //public final int wordScore;
        public final SearchField field;

        public int fieldScore;
        public int wordCount;

        public int stopped=-1;
        public int cardinality;
        public int totalCardinality;
        public int frequencyScore;
        public int relativeScore;
        public int score;
        public String fieldName() {return field.name;}
        public String text;
        public boolean single;
        public boolean wildcard;
        public int wordNumberFirst;
        public int wordNumberLast;

        public Match(int wordNumberFirst,int wordNumberLast, String text,IndexReader index,List<Closeable> connection, int code, SearchField field,boolean single,boolean wildcard) throws IOException{
            this.wordNumberFirst = wordNumberFirst;
            this.wordNumberLast = wordNumberLast;
            this.single = single;
            this.wildcard = wildcard;
            this.text=text.intern();
            /*this.card = card;*/
            IndexReader.ValueRead find=index.open(connection,code);
            this.find =find;
            //this.wordScore = score;
            this.field = field;
            wordCount=wordNumberLast-wordNumberFirst+1;
            fieldScore=field.score;
            this.cardinality=find.count();
            this.totalCardinality=index.to.cardinality;
            int totalCardinalityScore=(int) Math.log(totalCardinality);
            int cardinalityScore=(int) Math.log(cardinality);
            this.relativeScore=totalCardinalityScore-cardinalityScore;

            this.frequencyScore=fieldScore*wordCount-relativeScore;
            this.score=fieldScore*wordCount+relativeScore;

        }


        public String makeText(String[] words) {
            StringBuilder sb=new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i>0 && (i<=wordNumberFirst || i>wordNumberLast)) sb.append(" ");
                if (i==wordNumberFirst) sb.append(text);
                if (i<wordNumberFirst || i>wordNumberLast) sb.append(words[i]);
            }
            return sb.toString();
        }


        public boolean check(int rownum) throws IOException {
            return find.next(rownum)==rownum;
        }


        public int next(int rownum) throws IOException {
            if (stopped!=-1) return Integer.MAX_VALUE;
            return find.next(rownum);
        }

        public Find[] getChildren() {
            return new Find[]{find};
        }

        public BitReader getBitReader() {
            return null;
        }


        public int compareTo(Match match) {
            return score-match.score;
        }

        public void stop(int rownum) {
            if (stopped!=-1) return;
            stopped=rownum;
        }

        @Override
        public String toString() {
            return "Match{" +
                    "field=" + field +
                    ", fieldScore=" + fieldScore +
                    ", wordCount=" + wordCount +
                    ", stopped=" + stopped +
                    ", cardinality=" + cardinality +
                    ", totalCardinality=" + totalCardinality +
                    ", frequencyScore=" + frequencyScore +
                    ", relativeScore=" + relativeScore +
                    ", score=" + score +
                    ", text='" + text + '\'' +
                    ", single=" + single +
                    ", wildcard=" + wildcard +
                    ", wordNumberFirst=" + wordNumberFirst +
                    ", wordNumberLast=" + wordNumberLast +
                    '}';
        }
    }

    public static class FieldScoreCard implements Comparable<FieldScoreCard>,Copyable<FieldScoreCard> {
        public int rownum;
        public int score;
        public int[] tieBreak;

        public List<Match> hits=new ArrayList<Match>();

        public FieldScoreCard(int[] tieBreak) {
            this.tieBreak = tieBreak;

        }

        public FieldScoreCard(int rownum, int score, List<Match> hits,int[] tieBreak) {
            this.tieBreak = tieBreak;
            this.rownum = rownum;
            this.score=score;
            this.hits=hits;
        }

        public int rownum() {return rownum;}

        public FieldScoreCard copy() {
            return new FieldScoreCard(rownum, score,new ArrayList<Match>(hits), tieBreak);
        }

        public void fastCalculate(int rownum,List<Match> matches) throws IOException {
            this.rownum=rownum;

            score=0;
            hits.clear();
            for (Match hit : matches) {
                if (rownum!=hit.next(rownum)) continue;                
                hits.add(hit);
                score+=hit.score;
            }


        }

        public void calculate(int rownum,List<Match> matches) throws IOException {
            this.rownum=rownum;

            score=0;
            hits.clear();


            for (Match hit : matches) {
                if (!hit.check(rownum)) continue;
                while (hits.size()<=hit.wordNumberFirst) hits.add(null);
                if (hits.get(hit.wordNumberFirst)==null || hits.get(hit.wordNumberFirst).score<hit.score)
                    hits.set(hit.wordNumberFirst,hit);
            }
            for (Match hit : hits) if (hit!=null) score+=hit.score;
        }



        public int tieBreakValue() {
            return tieBreak==null?0:tieBreak[rownum];
        }


        public int compareTo(FieldScoreCard o) {
            if (o==null) return 1;
            if (score!=o.score) return score-o.score;
            int tie=o.tieBreakValue()-tieBreakValue();
            if (tie!=0) return tie;
            return o.rownum-rownum;
        }
    }

    public TextTableReader.Cache wordTable;
    
    IndexReader wordIndex;
    IndexReader wordPairIndex;
    IntegerTableReader.Cache wordPairs;


    public TextSearch(TextTableReader.Cache wordTable, DataLocation.Index index) throws IOException {
        this(wordTable,index.read());
    }

    public TextSearch(TextTableReader.Cache wordTable, IndexReader wordIndex) {
        this.wordTable = wordTable;
        this.wordIndex = wordIndex;
    }

    public TextSearch(DataLocation.TextIndexColumn i) throws Exception {
        wordTable = new TextTableReader(i.words.file()).cache();

        if (i.pairs != null) {
	        wordPairs = new IntegerTableReader(i.pairs.file()).cache();
        }

        wordIndex = new IndexReader(i.wordIndex.file());
                                                
        if (i.pairIndex != null) {
	        wordPairIndex = new IndexReader(i.pairIndex.file());
        }
    }

    public void search(List<Closeable> connection,String[] words, List<Match> score,SearchField field,boolean allowWildcard) throws IOException {

        //todo: remove allowWildcard
        allowWildcard=false;

        int wildCardLimit=30;

        //int[] wordCodes=new int[words.length];
        int previous=-1;
        TextTableReader.Cursor wordCursor = wordTable.use();
        String[] row=new String[1];
        String previousWord=null;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            boolean wildCard=allowWildcard && i==words.length-1;
            if (word.endsWith("*")) {
                word = word.substring(0,word.length() - 1);
                if (allowWildcard) wildCard=true;
            }
            row[0] = word;
            int index = wordCursor.search(row,true);
            
            wordCursor.seek(index);
            for (int j =index; j < index+(wildCard?wildCardLimit:1); j++) {
                String[] foundRow=wordCursor.read();
                int compare = wordCursor.compare(row, foundRow);
                if (compare>1 || (!wildCard && compare>0)) break;
                String found=foundRow[0];
                score.add(new Match(i,i,found, wordIndex,connection,j, field, true, compare==1));
                if (wordPairIndex!=null && previous!=-1) {
                    int pair = wordPairs.use().search(new int[]{previous,j});
                    if (pair>=0)
                        score.add(new Match(i-1,i,previousWord+" "+found,wordPairIndex,connection, pair, field,false,compare==1));

                }
            }

            previous=index;
            previousWord=word;
        }
    }

/*
    public static void main(String[] args) {
        String[] sa1={"x","y","z"};
        String[] sa2={"x","a","b"};
        List<String> intersect=new ArrayList<String>(Arrays.asList(sa1));
        intersect.retainAll(Arrays.asList(sa2));
        System.out.println(intersect.toArray(new String[intersect.size()]).length);
    }
*/
}
