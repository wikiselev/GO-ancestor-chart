package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.common.performance.*;

import java.io.*;

public class BitReader implements Closeable {

    private static Location me=new Location();

    public static final int BLOCKSIZE=65536;

    private byte[] data;
    private int dataLength;
    private int csize;
    private int current;
    private int offset;
    private long base;

    /*private InputStream is;*/

    RandomAccessFile raf;
    public long fileSize;
    public File file;

    public long readBytes;
    public long readNS;

    /*public BitReader(int byteSize, InputStream is) throws IOException {
        this.is = is;
        data = new byte[byteSize];
        load();
        base=0;
    }
*/



    public BitReader(File file,long byteSize) throws IOException {
        this.file = file;

        raf=new RandomAccessFile(file, "r");
        /*Action a=me.start("Open "+file);
        me.stop(a);*/

        byteSize=Math.min(raf.length(),byteSize);
        data=new byte[(int) byteSize];
        fileSize = raf.length();
        base=0;
    }

    public BitReader(File file) throws IOException {
        this(file,BLOCKSIZE);
    }

    public BitReader(File file,boolean all) throws IOException {
        this(file,all?Long.MAX_VALUE:BLOCKSIZE);
    }

    public BitReader(byte[] data, long base, long from) throws IOException {
        this.base = base;
        this.offset= (int) (from/8-base);        
        this.data = data;
        dataLength=data.length;
        read((int) (from % 8));
    }

    


    public class Cache {
        private byte[] data;
        private long base;
        private long from;

        public Cache(byte[] data, long base, long from) {

            this.data = data;
            this.base = base;
            this.from = from;
        }

        public BitReader use() throws IOException {
            return new BitReader(data,base,from);
        }
    }




    public void seek(long to) throws IOException {
        //System.out.println("Seek "+to+" "+offset+" "+base+" "+csize);
        if (bitCount()==to) return;
        long index=to/8;
        csize=0;
        current=0;
        if (index<base || index >= dataLength+base) load(index);
        offset= (int) (index-base);
        read((int) (to%8));
    }

    public boolean readBit() throws IOException {
        if (csize==0) {
            if (offset >= dataLength) load(offset+base);
            current = ((int) data[offset]) & 0xff;
            offset++;
            csize += 8;
        }

        int v = current & 1;
        
        csize --;
        current >>>= 1;

        return v>0;
    }

    /**
     * Read upto 24 bits of data
     *
     * @param size 0-24 bits
     * @param mask Presupplied bitmask normally: 0xffffffff>>(32-bits)
     * @return data of requested size
     * @throws IOException on underlying error
     */
    public int readShort(int size, int mask) throws IOException {

        if (size == 0) return 0;

        while (csize < size) {
            if (offset >= dataLength) load(offset+base);
            //System.out.println("Load: "+data[index]);
            current |= (((int) data[offset]) & 0xff) << csize;
            offset++;
            csize += 8;
        }

        int v = current & mask;
        //System.out.println("Read "+v+" "+current+" "+size);
        csize -= size;
        current >>>= size;
        //System.out.println("get: "+Counter.bitString(v,size));
        return v;
    }

    public int read(int size) throws IOException {
        if (size>=32) throw new IOException("Too many bits "+size);
        return (int)readLong(size);
    }

    public long readLong(int size) throws IOException {
        long c=current;
        if (size == 0) return 0;
        while (csize < size) {
            if (offset >= dataLength) load(offset+base);
            //System.out.println("Load: "+data[index]);
            c |= (((long) data[offset]) & 0xff) << csize;
            offset++;
            csize += 8;
        }

        long v = c & ((0xffffffffffffffffl) >>> (64 - size));
        //System.out.println("Read "+v+" "+current+" "+size);
        csize -= size;
        current= (int)(c>>size);
        //System.out.println("get: "+Counter.bitString(v,size));
        return v;
    }

    public long readLong() throws IOException {
        return readLong(32) | (readLong(32)<<32);
    }

    public int readInt() throws IOException {
        return (int)readLong(32);
    }

    public int readBEInt() throws IOException {
        int b1=read(8);
        int b2=read(8);
        int b3=read(8);
        int b4=read(8);
        return  (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
    }


    public Cache cache(long from,long size) throws IOException {
        long base=from/8;
        int dataLength= (int) Math.min(size/8+2, fileSize -base);
        byte[] data=new byte[dataLength];

        loadData(base, data, dataLength);
        return new Cache(data,base,from);
    }

    private void loadData(long base, byte[] data, int dataLength) throws IOException {
	    if (base < 0 || base >= fileSize) throw new IOException("Seek out of range: 0 >= " + base + " < fileSize (" + fileSize + ")");
        long start=System.nanoTime();
        raf.seek(base);
        raf.readFully(data,0,dataLength);

        readNS+=System.nanoTime()-start;
        readBytes+=dataLength;
    }

    public void load(long index) throws IOException {

        if (raf==null) throw new IOException("Not attached to file "+base+"<="+index+"<"+(base+dataLength));

        base=(index / data.length)*data.length;
        offset= (int) (index-base);

        dataLength= (int) Math.min(data.length, fileSize -base);

        loadData(base, data, dataLength);
    }



    public void close() throws IOException {
        data=null;
        Action a=me.start("Close");
        if (a!=null) a.setExtra("Read "+file+" bytes: "+String.format("%,d",readBytes)+" "+String.format("%,d",readNS)+" ns");
        if (raf!=null) raf.close();
        me.stop(a);
    }

    public long bitCount() {
        return ((base+offset)<<3)-csize;
    }

    public String readUTF8(int limit) throws IOException {
        StringBuilder s=new StringBuilder();
        readUTF8(s,limit);
        return s.toString();
    }

     public void readUTF8(StringBuilder s,int limit) throws IOException {

         s.setLength(0);
         while (limit>0) {
             int x = read(8);
             int f=0;
             int c=x & 0x7f;
             if (x >= 0xc0 && x < 0xe0) {c = x & 0x1f;f=1;}
             if (x >= 0xe0 && x < 0xf0) {c = x & 0x0f;f=2;}
             if (x >= 0xf0 && x < 0xf8) {c = x & 0x07;f=3;}
             while (f>0) {
                 x = read(8);
                 c = c << 6 + (x & 0x3f);
             }
             if (c==0) break;
             s.appendCodePoint(c);
             limit--;
         }
     }

    public String readString() throws IOException {
        StringBuilder buffer = new StringBuilder();
        int i;
        while ((i = (int) read(16)) != 0) buffer.append((char) i);
        return buffer.toString();
    }

    public String readString(int limit) throws IOException {
        StringBuilder buffer = new StringBuilder();
        int i;
        while ((i = (int) read(16)) != 0 && buffer.length()<limit) buffer.append((char) i);
        return buffer.toString();
    }

    public static void main(String[] args) {

        /*ByteArrayOutputStream baos=new ByteArrayOutputStream();
        BitWriter bw=new BitWriter(1024,baos);
        bw.write(0x380,32);
        bw.close();*/
        /*BitReader br=new BitReader(1024,new ByteArrayInputStream(baos.toByteArray()));
        for (int i=0;i<32;i++) {
            System.out.println(br.read(1));
        }
*/
    }


}
