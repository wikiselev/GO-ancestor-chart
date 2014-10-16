package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;

public class LongList {

    

    protected long[] data;
    public int length;
    private static Location me=new Location();

    public LongList(int initialCapacity) {
        data =new long[initialCapacity];
    }
    protected LongList() {}
    public LongList(File f) throws IOException {load(f);}

    public long get(int index) {
        if (index>=length) throw new IndexOutOfBoundsException("Accessing beyond end of array:"+index+">="+length);
        return data[index];
    }

    public void add(long l) {
        if (length== data.length) resize(data.length*2);
        data[length++]=l;
    }

    public void resize(int sz) {
        System.arraycopy(data,0,data=new long[sz],0,length);
    }

    public void finish() {}


    public void save(File f) throws IOException {        
        BitWriter bw=new BitWriter(65536,f);
        bw.writeInt(length);
        for (int i=0;i<length;i++) bw.writeLong(data[i]);
        bw.close();
        
    }

    public void load(File f) throws IOException {
        Action a=me.start("Load "+f);
        MemoryMonitor mm=new MemoryMonitor();
        
        BitReader br=new BitReader(f);
        length=br.readInt();
        data =new long[length];
        for (int i=0;i< length;i++) data[i]=br.readLong();
        br.close();

        me.note("Loaded "+data.length);
        me.note(mm.end(),mm);
        me.stop(a);
    }



    public int size() {
        return length;
    }
}
