package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;

public class IntList {


    protected int[] data;
    protected int length;
    private static Location me=new Location();

    public IntList() {
        this(1024);
    }

    public IntList(int initialCapacity) {
        data =new int[initialCapacity];
    }
    public IntList(File f) throws IOException {load(f);}


    public void validate(int index) {
        if (index>=data.length) resize(index*2);        
        if (index>=length) length=index+1;

    }

    public void add(int value) {
        set(length,value);
    }

    public void set(int index,int value) {
        validate(index);
        data[index]=value;
    }

    public int get(int index) {
        if (index>=length) return 0;
        return data[index];
    }

    public void resize(int sz) {
        System.arraycopy(data,0,data=new int[sz],0,length);
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
        data =new int[length];
        for (int i=0;i< length;i++) data[i]=br.readInt();
        br.close();

        me.note("Loaded "+data.length);
        me.note(mm.end(),mm);
        me.stop(a);
    }



    public int size() {
        return length;
    }

    /**
     * Returns the current backing array.
     * Note this array may be longer than the current size, and may be invalidated by any mutation operations.
     *
     * @return
     */
    public int[] getArray() {
        return data;
    }

}
