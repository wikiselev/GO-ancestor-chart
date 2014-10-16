package uk.ac.ebi.interpro.common.collections;



import java.util.*;

public class LimitedSizeList<T> extends ArrayList<T> {
    private int maxSize;
    private Comparator<T> comparator;

    public LimitedSizeList(int maxSize,Comparator<T> comparator) {
        this.maxSize = maxSize;
        this.comparator = comparator;
    }

    public LimitedSizeList(int maxSize) {
        this(maxSize,null);
    }


    public boolean add(T t) {

        int index=0;
        if (comparator!=null) index=Collections.binarySearch(this,t,comparator);
        if (index<0) index=-index-1;
        super.add(index,t);

        while (size() > maxSize) remove(size()-1);
        return true;
    }
    
}
