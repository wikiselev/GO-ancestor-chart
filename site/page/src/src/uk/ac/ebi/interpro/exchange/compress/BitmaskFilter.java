package uk.ac.ebi.interpro.exchange.compress;

public class BitmaskFilter {
    byte[] valueMask;
    public int count;

    public BitmaskFilter(int count) {
        this.count = count;
        valueMask=new byte[count/8+1];
    }

    public void add(int index) {
        valueMask[index/8]|=1<<index%8;
    }
    public boolean test(int index) {
        return (((int)valueMask[index/8]) & (1<<(index%8)))!=0;
    }
}
