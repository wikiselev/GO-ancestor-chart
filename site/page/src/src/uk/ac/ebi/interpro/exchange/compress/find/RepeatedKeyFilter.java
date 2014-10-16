package uk.ac.ebi.interpro.exchange.compress.find;

import uk.ac.ebi.interpro.exchange.compress.*;

import java.io.*;

public class RepeatedKeyFilter extends FindCache {
    int[] keyRepeatCount;
    private Find any;
    private Find keyFilter;
    private Find underlying;
    /*int index = -1;
            int next = -1;
    int goodIndex=-1;
*/


    public RepeatedKeyFilter(int[] data, Find underlying,Find any, Find keyFilter) {

        this.keyRepeatCount = data;
        this.underlying = FindCache.cache(underlying);
        this.any = FindCache.cache(any);
        this.keyFilter = FindCache.cache(keyFilter);
    }

    int thisKey=0;
    int nextKey=0;
    int keyIndex=-1;
    boolean eof;
    int matchCount=0;
    int[] matches=new int[4096];


    public int forwards(int at) throws IOException {


        while (at>=nextKey) {
            if (!findNextKey()) return Integer.MAX_VALUE;
        }

        for (int i = 0; i < matchCount; i++) {
              if (matches[i]>=at) return matches[i];
        }

        return forwards(nextKey);
    }

    private boolean findNextKey() throws IOException {
        while (keyIndex< keyRepeatCount.length-1) {
            thisKey=nextKey;            
            keyIndex++;
            nextKey=thisKey+ keyRepeatCount[keyIndex];

            if (keyFilter.next(keyIndex)>keyIndex) continue;

            int match=thisKey;
            matchCount=0;
            boolean ok=false;
            while ((match=underlying.next(match))<nextKey) {
                if (any.next(match)==match) ok=true;
                matches[matchCount++]=match;
                match++;
            }
            if (ok) return true;
        }
        thisKey=Integer.MAX_VALUE;
        nextKey=Integer.MAX_VALUE;
        return false;
    }
/*

    public int next(int at) throws IOException {


                while (at >= next) {
                    if (at == Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    index++;
                    if (index>= keyindex.length) next=Integer.MAX_VALUE;
                    else next = keyindex[index];
                    if (at < next) {
                        if (goodIndex<index) goodIndex=keyFilter==null?index:keyFilter.next(index);
                        if (goodIndex>index) at=next;
                        else if (any !=null && any.next(at)>=next) at=next;
                    }

                }
                return at;

            }
*/

    public Find[] getChildren() {
        return new Find[]{underlying,any,keyFilter};
    }

    public BitReader getBitReader() {
        return null;
    }

    @Override
    public String toString() {
        return "RepeatedKeyFilter";
    }
}
