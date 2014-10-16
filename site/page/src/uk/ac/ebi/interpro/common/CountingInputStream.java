package uk.ac.ebi.interpro.common;

import java.io.*;

public class CountingInputStream extends FilterInputStream {

    long count;

    public long getCount() {
        return count;
    }

    public CountingInputStream(InputStream inputStream) {
        super(inputStream);
    }

    public int read() throws IOException {
        count++;
        return in.read();
    }

    public int read(byte[] bytes) throws IOException {
        int i = in.read(bytes);
        count+=i;
        return i;
    }

    public int read(byte[] bytes, int i, int i1) throws IOException {
        int c = in.read(bytes, i, i1);
        count+=c;
        return c;
    }

    public long skip(long l) throws IOException {
        long c = in.skip(l);
        count+=c;
        return c;
    }
}
