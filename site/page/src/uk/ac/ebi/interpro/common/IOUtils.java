package uk.ac.ebi.interpro.common;

import java.io.*;
import java.net.*;
import java.util.*;

public class IOUtils {

    /**
     * Copy bytes from input to output until no more available.
     * Note: Doesn't close either.
     * @param is Input
     * @param os Output
     * @throws java.io.IOException only if thrown by underlying IO
     */

    public static void copy(InputStream is,OutputStream os) throws IOException {
        byte[] buff = new byte[1024];
             int ct;
             while ((ct = is.read(buff)) > 0) {
                 os.write(buff, 0, ct);
             }

    }

    /**
     * Copy characters from input to output until no more available.
     * Note: Doesn't close either.
     *
     * @param rd Input
     * @param wr Output
     * @throws IOException only if thrown by underlying IO
     */

    public static void copy(Reader rd,Writer wr) throws IOException {
        char[] buff = new char[1024];
             int ct;
             while ((ct = rd.read(buff)) > 0) {
                 wr.write(buff, 0, ct);
             }

    }

    /**
     * Read all data from reader into a string. Doesn't close the reader
     * @param rd Reader from which data is loaded
     * @return text
     * @throws java.io.IOException on underlying error
     */

    public static String readString(Reader rd) throws IOException {
        StringWriter sw=new StringWriter();
        copy(rd,sw);
        return sw.toString();
    }

    /**
     * Read all data from reader into a string, and close the reader
     * @param rd Reader from which data is loaded
     * @return text
     * @throws java.io.IOException on underlying error
     */

    public static String readStringClose(Reader rd) throws IOException {
        String s=readString(rd);
        rd.close();
        return s;
    }


    /**
     * Load all the data from a URL into a string
     * @param url Data loadeded from here
     * @return Data from URL as a string
     * @throws java.io.IOException on underlying error
     */


    public static String readString(URL url) throws IOException {
        if (url==null) return null;
        return readStringClose(new InputStreamReader(url.openStream()));
    }



    /**
     * Load all the data from a stream into a byte array
     * @param is Stream from which data is loaded
     * @return data
     * @throws java.io.IOException on underlying error
     */


    public static byte[] readBytes(InputStream is) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is,baos);
        return baos.toByteArray();
    }



    /**
     * Load all the data from a stream into a byte array, and then close the stream
     * @param is Stream from which data is loaded
     * @return data
     * @throws java.io.IOException on underlying error
     */


    public static byte[] readBytesClose(InputStream is) throws IOException {
        byte[] bytes = readBytes(is);
        is.close();
        return bytes;
    }

    /**
     * Load all the data from a URL into a string
     * @param url Data loadeded from here
     * @return Data from URL as a string
     * @throws java.io.IOException on underlying error
     */


    public static byte[] readBytes(URL url) throws IOException {

        if (url==null) return null;
        return readBytesClose(url.openStream());

    }

    /**
     * Write a string to a file.
     *
     * @param contents Source
     * @param file Target
     * @throws IOException on underlying failure
     */

    public static void copy(String contents, File file) throws IOException {
        FileWriter fw=new FileWriter(file);
        copy(new StringReader(contents),fw);
        fw.close();
    }

    /**
     * Download a URL to a file.
     *
     * @param url Source
     * @param file Target
     * @throws IOException on underlying failure
     */
    public static void copy(URL url, File file) throws IOException {
        InputStream is=url.openStream();
        OutputStream os=new FileOutputStream(file);
        IOUtils.copy(is,os);
        is.close();
        os.close();
    }

    /**
     * Close all items in a closeable collection.
     *
     * @param holder from which all things will be closed
     * @return a list of exceptions (empty if none) that were caught while closing
     */
    public static List<Exception> closeAll(List<Closeable> holder) {
        List<Exception> exceptions=new ArrayList<Exception>();
        for (Closeable c : holder) {
            try {c.close();} catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

    /**
     * Wrap a writer with a writer which encodes XML text entities. Encodes only &lt; and &amp;.
     *
     * @param wr writer which will be wrapped
     * @return Writer to which text to be entity encoded can be written
     */

    public static Writer xmlEncode(final Writer wr) {
        return xmlEncode(wr,StringUtils.MASK_XML_TEXT);
    }

    /**
     * Wrap a writer with a writer which encodes XML text entities.
     *
     * @param wr writer which will be wrapped
     * @param mask XML mask
     * @return Writer to which text to be entity encoded can be written
     * @see StringUtils.xmlEncoder(CharSequence,int) 
     */

    public static Writer xmlEncode(final Writer wr, final int mask) {
        return new Writer() {

            public void write(int i) throws IOException {write(String.valueOf((char) i));}

            public void write(char[] chars) throws IOException {write(chars,0,chars.length);}

            public void write(char[] chars, int start, int end) throws IOException {write(new String(chars,start,end));}

            public void write(String s) throws IOException {append(s);}

            public void write(String s, int start, int end) throws IOException {append(s, start, end);}

            public Writer append(CharSequence s) throws IOException {
                wr.write(StringUtils.xmlEncoder(s,mask));
                return this;
            }

            public Writer append(CharSequence s, int start, int end) throws IOException {
                return append(s.subSequence(start,end));
            }

            public Writer append(char c) throws IOException {return append(String.valueOf(c));}

            public void flush() throws IOException {wr.flush();}

            public void close() throws IOException {wr.close();}
        };
    }


    /**
     * Convert a RandomAccessFile into an outputstream
     */
    static class RAFOut extends OutputStream {
        RandomAccessFile raf;

        public void write(int i) throws IOException {
            raf.write(i);
        }

        public RAFOut(RandomAccessFile raf) {
            this.raf=raf;
        }

        public void write(byte[] bytes) throws IOException {
            raf.write(bytes);
        }

        public void write(byte[] bytes, int off, int sz) throws IOException {
            raf.write(bytes, off,sz);
        }


        public void close() throws IOException {
            raf.close();
        }
    }

    /**
     * Convert a RandomAccessFile into an inputstream
     */
    static class RAFIn extends InputStream {

        RandomAccessFile raf;

        public RAFIn(RandomAccessFile raf) {
            this.raf = raf;
        }

        public int read() throws IOException {
            return raf.read();
        }

        public int read(byte[] bytes) throws IOException {
            return raf.read(bytes);
        }

        public int read(byte[] bytes, int off, int sz) throws IOException {
            return raf.read(bytes,off, sz);
        }

        public long skip(long l) throws IOException {
            return raf.skipBytes((int)l);
        }

        public void close() throws IOException {
            raf.close();
        }

    }

    /**
     * Convert a relative file to an absolute file using a reference.
     * If the path is absolute then it is returned as a File
     *
     * @param base File (absolute) which provides base if necessary
     * @param path Path (absolute or relative) to be made absolute
     * @return Absolute File
     */
    public static File relativeFile(File base, String path) {
        if (path==null) return base;
        File file = new File(path);
        if (file.isAbsolute()) return file;
        return new File(base, path);
    }

    /**
     * Convert a relative URI to an absolute URI using a reference.
     * If the uri is absolute then it is returned
     *
     * @param baseFile File (absolute) which provides base if necessary
     * @param uri URI (absolute or relative) to be made absolute
     * @return Absolute URI
     */
    public static URI relativeURI(File baseFile, String uri) throws URISyntaxException {
        URI base=new File(baseFile,".").toURI();
        if (uri==null) return base;
        URI file = new URI(uri);
        if (file.isAbsolute()) return file;
        return base.resolve(file);
    }


    public static void main(String[] args) throws MalformedURLException, URISyntaxException {
        System.out.println(new File(new File("/x/y"),"u/i"));
        System.out.println(new File("/x/y/.").toURI());
        System.out.println(new File(new File("/x/y/.").toURI()));
        System.out.println(new File(new URL(new File("/x/y/.").toURI().toURL(),"u/i").toURI()));
        System.out.println(new File(new URL(new File("/x/y/").toURI().toURL(),"/u/i").toURI()));
        System.out.println(new File(new File("/x/y/").toURI().resolve("u/i")).toURI());
        System.out.println(new File(new File("/x/y/").toURI().resolve("/u/i")).toURI());
        System.out.println(new URL(new URL("file:/x/y/"),"u/i"));
        System.out.println(new URL(new URL("file:/x/y/"),"u/i"));
        System.out.println(relativeFile(new File("/"),null));
    }


}
