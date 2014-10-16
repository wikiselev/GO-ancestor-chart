package uk.ac.ebi.interpro.common;

import java.util.*;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 27-Aug-2004
 * Time: 11:32:44
 * To change this template use File | Settings | File Templates.
 */
public class StringUtils {
    static char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Light weight single-delimiter parser.
     * Returns array:
     * [0] before delimeter occures
     * [1] after between first and second delimiter
     * ...
     * [n+1] after last (nth) delimiter occurance
     *
     * @param from      String to parse
     * @param delimiter Delimiter to split input String around
     * @return strings around delimiters.
     */
    public static List<String> listParse(String from, String delimiter) {

        List<String> l = new ArrayList<String>();

        int location = 0;

        while (true) {
            int p = from.indexOf(delimiter, location);
            if (p == -1) break;
            l.add(from.substring(location, p));
            location = p + delimiter.length();
        }

        l.add(from.substring(location));

        return l;
    }

    /**
     * Light weight single-delimiter parser.
     * Returns array:
     * [0] before delimeter occures
     * [1] after between first and second delimiter
     * ...
     * [n+1] after last (nth) delimiter occurance
     *
     * @param from      String to parse
     * @param delimiter Delimiter to split input String around
     * @return strings around delimiters.
     */
    public static String[] parse(String from, String delimiter) {

        List<String> l = listParse(from, delimiter);

        return (String[]) l.toArray(new String[l.size()]);
    }


    /**
     * Translate &amp; to &amp;amp;
     */
    public final static int MASK_AMP = 1;
    /**
     * Translate &lt; to &amp;lt;
     */
    public final static int MASK_LT = 2;
    /**
     * Translate &gt; to &amp;gt;
     */
    public final static int MASK_GT = 4;
    /**
     * Translate &quot; to &amp;quot;
     */
    public final static int MASK_QUOT = 8;
    /**
     * Translate &apos; to &amp;apos;
     */
    public final static int MASK_APOS = 16;


    /**
     * Translate to all default xml entities
     */
    public final static int MASK_FULL_XML = 31;
    /**
     * Translate to all default entities understood by html and xml
     */
    public final static int MASK_FULL_HTML = 15;
    /**
     * Translate to all the minimal entities required for normal text
     */
    public final static int MASK_XML_TEXT = 3;

    public static String xmlEncoder(CharSequence text) {
        return xmlEncoder(text,MASK_FULL_HTML);
    }


    /**
     * Transform any non-legal xml characters into their entity equivalents:
     * <p/>
     * <pre>
     * &  &amp;
     * <  &lt;
     * >  &gt;
     * '  &apos;
     * "  &quot;
     * </pre>
     *
     * @param text      to translate
     * @param xlateMask mask of different entity characters to translate. See MASK_XXXX constants.
     * @return translated string
     */
    public static String xmlEncoder(CharSequence text, int xlateMask) {

        if (text == null) return null;

        StringBuilder buff = new StringBuilder(text.length());
        int i = 0;

        while (i < text.length()) {

            if ((text.charAt(i) == '&') && ((xlateMask & MASK_AMP) > 0)) {
                buff.append("&amp;");
            } else if ((text.charAt(i) == '<') && ((xlateMask & MASK_LT) > 0)) {
                buff.append("&lt;");
            } else if ((text.charAt(i) == '>') && ((xlateMask & MASK_GT) > 0)) {
                buff.append("&gt;");
            } else if ((text.charAt(i) == '"') && ((xlateMask & MASK_QUOT) > 0)) {
                buff.append("&quot;");
            } else if ((text.charAt(i) == '\'') && ((xlateMask & MASK_APOS) > 0)) {
                buff.append("&apos;");
            } else {
                buff.append(text.charAt(i));
            }
            i++;

        }

        return buff.toString();


    }



    public static String xmlDecoder(String text) {

        if (text == null) return null;

        StringBuilder buff = new StringBuilder(text.length());
        StringBuilder entity = new StringBuilder();
        int i = 0;
        boolean amp=false;
        while (i < text.length()) {
            if (text.charAt(i)==';' && amp) {
                amp=false;
                String e = entity.toString();
                if (e.equals("amp")) buff.append("&");
                if (e.equals("lt")) buff.append("<");
                if (e.equals("gt")) buff.append(">");
                if (e.equals("quot")) buff.append("\"");
                if (e.equals("apos")) buff.append("\'");
            } else if (text.charAt(i)=='&') {
                amp=true;
                entity.setLength(0);
            } else {
                if (amp)
                    entity.append(text.charAt(i));
                else
                    buff.append(text.charAt(i));
            }
            i++;

        }

        return buff.toString();


    }

    /**
     * Transform any non-legal javascript string characters
     *
     * @param text to encode
     * @return translated string
     */
    public static String singleQuoteEncoder(String text) {
        return quoteEscape(text, "'", '\\');
    }

    /**
     * Escape illegal characters
     *
     * @param text    Input
     * @param illegal Contains characters to quote
     * @param quote   Character used for quoting
     * @return Escaped input string
     */
    public static String quoteEscape(String text, String illegal, char quote) {

        if (text == null) return null;

        StringBuilder buff = new StringBuilder(text.length());
        int i = 0;

        while (i < text.length()) {

            if (illegal.indexOf(text.charAt(i)) > -1)
                buff.append(quote);

            buff.append(text.charAt(i));

            i++;
        }

        return buff.toString();


    }





    static byte[] decodes = new byte[]{
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1
    };

    /**
     * Decode a base64 string
http://tools.ietf.org/html/2045
     *
     * @param in encoded in base64
     * @return decoded result
     */
    public static byte[] decodeBase64(String in) {

        int acc = 0;
        int sz = 0;

        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        for (int i = 0; i < in.length(); i++) {
            final byte decode = decodes[(byte) in.charAt(i)];
            if (decode == -1) break;
            acc = (acc << 6) + decode;
            sz += 6;
            if (sz >= 8) bos.write((acc >> (sz -= 8)) & 0xff);
        }
        return bos.toByteArray();
    }

    static String encodes="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    

    /**
     * Encode a base64 string
     *
     * @param in unencoded data
     * @return encoded result
     */
    public static String encodeBase64(byte[] in) {

        int acc = 0;
        int sz = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length;) {
            acc=acc<<8+in[i];
            sz+=8;
            while (sz>=6) {
                out.append(encodes.charAt(acc & 0x3f));
                sz-=6;
            }
        }

        if (sz==4) {
            out.append(encodes.charAt(acc & 0x3f));
            out.append("=");
        }
        if (sz==2) {
            out.append(encodes.charAt(acc & 0x3f));
            out.append("=");
            out.append("=");
        }

        return out.toString();
    }



    /**
     * Add hex coded 16 bits of integer (4 hex digits) to stringbuffer
     */
    public static void appendHex16Code(StringBuilder sb, int code) {
        sb.append(hex[(code & 0xf000) >> 12]).append(hex[(code & 0xf00) >> 8]).append(hex[(code & 0xf0) >> 4]).append(hex[(code & 0xf)]);
    }

    /**
     * Add hex coded 8 bits of integer (2 hex digits) to stringbuffer
     */
    public static void appendHex8Code(StringBuilder sb, int code) {
        sb.append(hex[(code & 0xf0) >> 4]).append(hex[(code & 0xf)]);
    }

    public static int unhex(char ch) {
        if ((ch >= '0') && (ch <= '9')) return ch - '0';
        if ((ch >= 'A') && (ch <= 'F')) return ch - 'A' + 10;
        if ((ch >= 'a') && (ch <= 'f')) return ch - 'a' + 10;
        return 0;
    }

    public static int parseInt(String text,int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch(NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long parseLong(String text, long defaultValue) {
        try {
            return Long.parseLong(text);
        } catch(NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean parseBoolean(String text,boolean defaultValue) {
        if (text==null) return defaultValue;
        return "true".equals(text);
    }

    public static byte[] decodeHex(String in) {
        byte[] data = new byte[in.length() / 2];
        for (int i = 0; i < in.length(); i += 2) {
            data[i / 2] = (byte) ((unhex(in.charAt(i)) << 4) + unhex(in.charAt(i + 1)));
        }
        return data;
    }

    public static String encodeHex(byte... data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) appendHex8Code(sb, b);
        return sb.toString();
    }




    public static String findReplace(String source, String find, String replace) {
        String result = source;
        StringBuilder sb = new StringBuilder(source);
        int pos = source.lastIndexOf(find);
        while (pos != -1) {
            try {
                sb.replace(pos, pos + find.length(), replace);
            } catch (Exception ex) {
                break;
            }
            try {
                pos = source.lastIndexOf(find, pos - 1);
            } catch (Exception ex) {
                break;
            }
        }
        result = sb.toString();
        return result;
    }

    /**
     * Locate and replace parameters in a string.
     * <br />
     * A parameter is defined by the supplied Seeker, and the first matching group is used for the name of the parameter.
     * This is replaced with a value obtained from parameters if available or otherwise replace. <br />
     * Optional parameters may be null. <br />
     *
     * @param parameters optional name value map
     * @param replace    optional default replacement
     * @param input      the input text
     * @param seek       Seeker used to identify replaceable sections
     * @param locate     optional list to which the replaced parameter names will be added in order
     * @param recurse    flag to indicate whether to perform replacement within values
     * @param target     optional stringbuffer to which the final text will be appended
     */
    public static void replaceParameters(Map<String,String> parameters, String replace, String input, Seeker seek, Seeker protect, List<String> locate, boolean recurse, StringBuilder target) {

        Seeker.Locator m = seek.matcher(input);


        Seeker.Locator p=(protect==null)?null:protect.matcher(input);

        int p1, p2, prev = 0,startProtect=0,endProtect=0;
        while (m.find()) {
            
            p1 = m.start();
            p2 = m.end();

            while (endProtect<=p1 && p!=null && p.find()) {
                startProtect=p.start();
                endProtect=p.end();
            }

            if (p2>startProtect && p1<endProtect) {
                continue;
            }

            if (target != null) target.append(input.substring(prev, p1));

            String replacement = replace;
            if (locate!=null || parameters!=null) {
                String name = m.group(1);
                if (locate != null) locate.add(name);

                if (parameters != null) replacement = (String) parameters.get(name);
            }
            if (replacement != null) {
                if (recurse) replaceParameters(parameters, replace, replacement, seek, protect,locate, recurse, target);
                else if (target != null) target.append(replacement);
            } else {
                if (target != null) target.append(input.substring(p1, p2));
            }

            prev = p2;
        }
        if (target != null) target.append(input.substring(prev));


    }

    /**
     * Locate and replace parameters in a string.
     * <br />
     * A parameter of the form:
     * <br />
     * prefix name postfix
     * <br />
     * is replaced by
     * <br />
     * value
     * <br />
     * where value is obtained from parameters if available or otherwise replace.
     * <br/>
     * So for instance
     * <pre>
     * StringBuffer sb=new StringBuffer();
     * Map parameters=new HashMap();
     * parameters.put("who","world");
     * replaceParameters(parameters,null,"hello world","${","}",null,false,sb);
     * </pre>
     * Then sb.toString() returns "hello world"
     * <br />
     * Optional parameters may be null.
     * <br />
     */

    public static void replaceParameters(Map<String,String> parameters, String replace, String input, String prefix, String postfix, List<String> locate, boolean recurse, StringBuilder target) {
        Seeker pattern = new StringSeeker(prefix, postfix);
        replaceParameters(parameters, replace, input, pattern, null, locate, recurse, target);
    }



    /**
     * Convenience wrapper.
     *
     * @param parameters optional name value map
     * @param replace    optional default replacement
     * @param input      the input text
     * @param seek       Seeker used to identify replaceable sections
     * @param locate     optional list to which the replaced parameter names will be added in order
     * @param recurse    flag to indicate whether to perform replacement within values
     * @return final replaced text
     * @see #replaceParameters(java.util.Map, String, String, String, String, java.util.List, boolean, StringBuilder)
     */
    public static String replaceParameters(Map<String, String> parameters, String replace, String input, Seeker seek, List<String> locate, boolean recurse) {
        return replaceParameters(parameters, replace, input, seek, null, locate, recurse);
    }

    public static String replaceParameters(Map<String, String> parameters, String replace, String input, Seeker seek, Seeker protect, List<String> locate, boolean recurse) {
        StringBuilder target = new StringBuilder();
        replaceParameters(parameters, replace, input, seek, protect, locate, recurse, target);
        return target.toString();
    }

    /**
     * Convenience wrapper.
     *
     * @param parameters optional name value map
     * @param replace    optional default replacement
     * @param input      the input text
     * @param prefix     start of replacement section
     * @param postfix    end of replacement section
     * @param locate     optional list to which the replaced parameter names will be added in order
     * @param recurse    flag to indicate whether to perform replacement within values
     * @return final replaced text
     * @see #replaceParameters(java.util.Map, String, String, String, String, java.util.List, boolean, StringBuilder)
     */
    public static String replaceParameters(Map<String,String> parameters, String replace, String input, String prefix, String postfix, List<String> locate, boolean recurse) {
        StringBuilder target = new StringBuilder();
        replaceParameters(parameters, replace, input, prefix, postfix, locate, recurse, target);
        return target.toString();
    }

    /**
     * Convenience wrapper. No default replacement text and no recursion.
     *
     * @param parameters optional name value map
     * @param input      the input text
     * @param prefix     start of replacement section
     * @param postfix    end of replacement section
     * @return final replaced text
     * @see #replaceParameters(java.util.Map, String, String, String, String, java.util.List, boolean, StringBuilder)
     */
    public static String replaceParameters(Properties parameters, String input, String prefix, String postfix) {
        StringBuilder target = new StringBuilder();
        replaceParameters((Map)parameters, null, input, prefix, postfix, null, false, target);
        return target.toString();
    }

    /**
     * Convenience wrapper.
     *
     * @param input   the input text
     * @param replace the replacement text
     * @param seeker  Seeker used to identify replaceable sections
     * @return array containing replaced text (index 0) and parameter names (index>0)
     * @see #replaceParameters(java.util.Map, String, String, String, String, java.util.List, boolean, StringBuilder)
     */
    public static String[] locateParameters(String input, String replace, Seeker seeker,Seeker protect) {

        List<String> locate = new ArrayList<String>();
        locate.add(null);
        StringBuilder target = new StringBuilder();
        replaceParameters(null, replace, input, seeker, protect, locate, false, target);
        locate.set(0, target.toString());
        return (String[]) locate.toArray(new String[locate.size()]);
    }


    public static String nvl(String input) {
        return nvl(input,"");
    }





    /**
     * Returns its argument, unless null, in which the second (fallback) argument is returned.
     */
    public static <X> X nvl(X nullable, X fallback) {
        return nullable == null ? fallback : nullable;
    }

    /**
     * Iterates through it's arguments and returns the first one that is not null.
     *
     * @param successive list of arguments to search through
     * @return first non-null argument
     */
    public static <X> X nvl(X... successive) {
        for (X x : successive) {
            if (x!=null) return x;
        }
        return null;
    }


    public static String initialiser(String input, String init) {
        if (input == null || input.length() == 0)
            return init;
        else
            return input;
    }

//    public static Properties loadProperties(InputStream is) throws IOException {
//        Properties props = new Properties();
//        props.load(is);
//        return props;
//
//
//    }

    /**
     * Extract a map from a string.
     * <p/>
     * Map is represented as a series of name=value pairs separated by ;
     * The value may be quoted with "" or '', in which case \ can be used to escape
     * <p/>
     * For example:
     * <p/>
     * x=1 ; z="y"; rob = 'penguin\'s friend'
     *
     * @param text string representation of map
     * @return Map<String,String> of name, value pairs
     */
    public static Map<String, String> extract(String text) {
        return extract(text, "=", "\"'", "\"'", "\\", ";", " ");
    }

    /**
     * Extract a map from a string.
     * See {@link #extract(String)}
     * All syntatic elements can be specified
     * <p/>
     * For example: extract(text,"=","\"'","\"'","\\",";"," ");
     *
     * @param text        string representation of map
     * @param equals      name value delimiter
     * @param openQuotes  Opening quotation marks
     * @param closeQuotes Closing quotation marks (each one should correspond to an opening quote)
     * @param escape      Escape characters which may be used in quoted string
     * @param separator   separator between name,value pairs
     * @param ignore      ignorable component
     * @return Map<String,String> of name value pairs
     */
    public static Map<String, String> extract(String text, String equals, String openQuotes, String closeQuotes, String escape, String separator, String ignore) {
        StringProcess sp = new StringProcess(text);

        sp.setStringEscape(escape);

        Map<String, String> map = new HashMap<String, String>();
        String delimiters = ignore + equals;
        sp.setString(openQuotes, closeQuotes);

        while (!sp.atEnd()) {
            String name = sp.getSymbol(delimiters);
            sp.ignore(delimiters);
            if (sp.atEnd()) break;            
            String value = sp.get();
            sp.ignore(delimiters);
            map.put(name, value);

        }

        return map;

    }


    /**
     * Scramble a string using key. Xors alternating 16bit characters with lo and hi 16bits of int.
     *
     * @param value Input string
     * @param key   Scramble key
     * @return Hex coded scrambled string
     */

    public static String scramble(String value, int key) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            int lsb = key & 0xffff;
            key = (key >> 16) + (lsb << 16);

            int encode = ((int) value.charAt(i)) ^ lsb;

            StringUtils.appendHex16Code(sb, encode);

        }

        return sb.toString();
    }

    /**
     * UnScramble a string using key. Xors alternating 16bit characters with lo and hi 16bits of int.
     *
     * @param value Scrambled hex encoded string
     * @param key   Scramble key
     * @return Unscrambled string
     */

    public static String unscramble(String value, int key) {

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < value.length(); i += 4) {

            int code = Integer.parseInt(value.substring(i, i + 4), 16);

            int lsb = key & 0xffff;
            key = (key >> 16) + (lsb << 16);

            sb.append((char) (((int) code) ^ lsb));

        }

        return sb.toString();
    }

    /**
     * Convert a comma separated list of hex values to an int array
     */
    public static int[] getCSHCL(String colourList) {
        List colourNames = Collections.list(new StringTokenizer(colourList, ","));
        int[] ccs = new int[colourNames.size()];
        for (int i = 0; i < colourNames.size(); i++) {
            String c = (String) colourNames.get(i);
            ccs[i] = (int) Long.parseLong(c, 16);
        }
        return ccs;
    }

    
    /**
     * Convert an int array into a comma separated list of hex values
     */
    public static String makeCSHCL(int[] col) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < col.length; i++) {
            int c = col[i];
            if (i != 0) sb.append(",");
            sb.append(Integer.toHexString(c));
        }
        return sb.toString();

    }


    public static int getIndex(String[] parameters, String seek) {
        for (int i = 0; i < parameters.length; i++) if (parameters[i].equals(seek)) return i;
        return -1;
    }


    public static int cf(String s1,String s2) {
        if (s1==s2) return 0;
        if (s1==null) return 1;
        if (s2==null) return -1;
        return s1.compareTo(s2);
    }




    interface Pat {

    }

    interface State {

    }

    class Seq {

    }

    class MSM {
        String test;

    }


    public static void main(String[] args) throws UnsupportedEncodingException {
//        Map p=new HashMap();
//        p.put("x","y");
//        System.out.println("lardme: "+replaceParameters(p,null,"${x}","${","}",null,true));

//        System.out.println("Map extraction: "+extract("x=y;q=\" w\"; a = b ;c= '\"d\\'e' ;n=;=e"));

//        System.out.println(new String(decodeHex(encodeHex("david".getBytes("ASCII"))), "ASCII"));
//        System.out.println(encodeHex("david".getBytes("ASCII")));

        System.out.println(replaceParameters(null,"-","xx'xx'xx'x'x'x'x",new RegexSeeker("(x)"),new RegexSeeker("'[^']*'"),null,false));
        //System.out.println("axa.'axa'.axa".replaceAll("^[^']*(\'[^']*\')[^']*x","y"));
        //System.out.println("axa.'axa'.axa".replaceAll("(?<=^[^']*(?:\'[^']*\'[^']*)*)x","y"));

    }


}
