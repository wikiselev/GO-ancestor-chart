package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public class BitWriter {


    private byte[] data;
    private int csize;
    private long current;
    private long index;
    private long base;

    private RandomAccessFile f;
    private File file;

    public BitWriter(byte[] data) {
        //data=new byte[byteSize];
        this.data=data;
    }

    public BitReader read() throws IOException {
        close();
        return new BitReader(file);
    }


    public BitWriter(File file) throws FileNotFoundException {
        this(65536,file);
        this.file = file;
    }

    public BitWriter(int byteSize,File f) throws FileNotFoundException {
        this.f = new RandomAccessFile(f,"rw");
        data=new byte[byteSize];
    }

	private void writeByte() throws IOException {
		if (index-base>=data.length) save();
		data[((int) (index - base))]=(byte)(current & 0xff);
	}

    /**
     * Write specified number of bits of data.
     * Can safely write values upto 56 bits.
     *
     * @param value Data to write
     * @param size Bit size to write
     * @throws IOException on underlying IO failure
     */

    void write(long value, int size) throws IOException {
        //System.out.println("set: "+Integer.toBinaryString(value)+" "+size);
        if (size==0) return;
        value=value & ((0xffffffffffffffffl) >>> (64 - size));
        current|=value<<csize;
        csize+=size;

        while (csize>=8) {
	        writeByte();
            index++;
            current>>>=8;
            csize-=8;
        }
    }

    void write(byte[] data,int size) throws IOException {
        for (byte b : data) {
            int bits=8;
            if (bits>size) bits=size;
            write(b,bits);
            size-=bits;
            if (size<=0) return;
        }        
    }

    public void byteAlign() throws IOException {
        write(0,8-csize);
    }

    public void writeBytes(byte[] source, int bytes) throws IOException {
        int sourceIndex=0;
        while (sourceIndex<bytes) {
            if (index-base>=data.length) save();
            long size=Math.min(bytes-sourceIndex,data.length-(index-base));
            if (size==0) throw new IOException("Non terminating loop");
            System.arraycopy(source,sourceIndex,this.data,(int)(index-base),(int)size);
            sourceIndex+=size;
            index+=size;
        }
    }

    public void save() throws IOException {
        if (f==null) throw new IOException("Overflowed buffer");
        int count = (int)(index - base);
        f.write(data,0, count);


        base+=count;

    }


    public void flush() throws IOException {
        if (csize>0) {
	        writeByte();
	        index++;
        }
    }


    public void close() throws IOException {
        flush();
        save();
        if (f!=null) f.close();
    }


    public long bitCount() {
        return (index <<3)+csize;
    }

    public void writeInt(int v) throws IOException {
        write(v,32);
    }

    public void writeLong(long l) throws IOException {
        write(l,32);
        write(l>>32,32);
    }
        
    public void writeString(String text) throws IOException {
        for (int i = 0; i < text.length(); i++) {
            write((int) text.charAt(i), 16);
        }
        write(0, 16);
    }



    public void writeUTF8(String s) throws IOException {

            int i = 0;
            while (i < s.length()) {
                int cp = Character.codePointAt(s, i);
                if (cp < 0x80) write(cp,8);
                if (cp >= 0x80 && cp < 0x800) {
                    write((cp >> 6) | 0xc0,8);
                    write((cp & 0x3f) | 0x80,8);
                }
                if (cp >= 0x800 && cp < 0x10000) {
                    write((cp >> 12) | 0xe0,8);
                    write((cp >> 6) | 0x80,8);
                    write((cp & 0x3f) | 0x80,8);
                }
                if (cp >= 0x10000 && cp < 0x110000) {
                    write((cp >> 18) | 0xf0,8);
                    write((cp >> 12) | 0x80,8);
                    write((cp >> 6) | 0x80,8);
                    write((cp & 0x3f) | 0x80,8);
                }
                i+=Character.charCount(cp);
            }
           write(0,8);
     }

    /**
     * Utility method to identify the number of bits required to store values in the reange 0..below-1
     *
     * @param below values less than this parameter
     * @return number of bits
     */

    public static int getSize(long below) {
        int size=0;
        while ((1l<<size)<below) size++;
        return size;
    }




}
