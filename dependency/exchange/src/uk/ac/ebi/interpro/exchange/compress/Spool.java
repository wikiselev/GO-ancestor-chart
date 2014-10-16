package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public class Spool {
    private int columns;

    public Spool(File file) throws IOException {

        this.spool = new File(file.getParentFile(),file.getName()+".tmp");;
        write=new BitWriter(spool);
    }

    public Spool(File file,int columns) throws IOException {
        this(file);
        this.columns = columns;

    }

    private File spool;
    BitWriter write;
    BitReader read;

    public long index() {
        return write.bitCount();
    }

	public void write(String s) throws IOException {
        write.writeString(s);
    }

	public void write(int i) throws IOException {
        write.writeInt(i);
    }

	public void rewind() throws IOException {
        write.close();
        read=new BitReader(spool);

    }

    public void seek(long index) throws IOException {
        read.seek(index);
    }

	public int[] read(int rownumber) throws IOException {
        if (columns==0) throw new IOException("File is not seekable");
        seek((((long)rownumber)*columns)*32);
		int[] data = new int[columns];
		for (int i = 0; i < columns; i++) {
			data[i] = read.readInt();
		}
        return data;
    }

	public int read() throws IOException {
        return read.readInt();
    }

	public void close() throws IOException {
        read.close();
    }


    public String readString() throws IOException {
        return read.readString();
    }
}