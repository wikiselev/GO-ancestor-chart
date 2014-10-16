package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import java.nio.charset.*;
import java.nio.*;
import java.io.*;

/**
 * URL manipulation utilities.
 *
 */
public class URLUtils {

    public static Charset utf8= Charset.forName("UTF-8");

    /**
     * Encode string using %XX for URL usage. UTF8 encoding character set.
     * @param from original string
     * @return encoded string
     */
    public static String encodeURL(String from) {

        StringBuilder sb = new StringBuilder();

        ByteBuffer bytes = utf8.encode(from);
        while (bytes.hasRemaining()) {
            char ch = (char) (bytes.get());
            if ((ch>='a' && ch<='z') || (ch>='A'  && ch<='Z') || (ch>='0' && ch<='9')
                    || ch=='_' || ch=='.' || ch=='~' || ch=='-')                   
                sb.append(ch);
            else {
                sb.append("%");
                StringUtils.appendHex8Code(sb, ch);
            }
        }

        return sb.toString();
    }

    /**
     * Decode string using %XX for URL usage. UTF8 encoding character set.
     *
     * @param from encoded string
     * @return decoded string
     */
    public static String decodeURL(String from) {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();

        for (int i = 0; i < from.length(); i++) {
            char ch = from.charAt(i);

            if (ch!='%')
                baos.write(ch);
            else {
                baos.write(StringUtils.decodeHex(from.substring(i+1,i+3))[0]);
                i+=2;
            }
        }

        return utf8.decode(ByteBuffer.wrap(baos.toByteArray())).toString();
    }

    public static Map<String, String> getFromParameterMap(Map/*<String, String[]>*/ httpParameters) {
        @SuppressWarnings({"unchecked"}) Map<String, String[]> parameters = (Map<String, String[]>) httpParameters;
        return getLastParameters(parameters);
    }

    private static Map<String, String> getLastParameters(Map<String, String[]> parameters) {
        Map<String, String> init = new HashMap<String, String>();
        for (String key : parameters.keySet()) {
            String[] values = parameters.get(key);
            init.put(key, values[values.length-1]);
        }
        return init;
    }

    private static Map<String, String> lastParametersList(Map<String, List<String>> parameters) {
        Map<String, String> init = new HashMap<String, String>();
        for (String key : parameters.keySet()) {
            List<String> values = parameters.get(key);
            init.put(key, values.get(values.size()-1));
        }
        return init;
    }

    public static Map<String,String> decodeURLLast(String url) {
        return lastParametersList(decodeURLMapList(url));
    }

    public static Map<String,String[]> decodeURLMulti(String url) {
        Map<String, List<String>> parameters = decodeURLMapList(url);

        Map<String,String[]> results=new HashMap<String, String[]>();

        for (String s : parameters.keySet()) {
            List<String> list = parameters.get(s);
            results.put(s,list.toArray(new String[list.size()]));
        }
        return results;
    }

    private static Map<String, List<String>> decodeURLMapList(String url) {
        Map<String,List<String>> parameters= CollectionUtils.arrayListHashMap();
        if (url==null) return parameters;
        int p=url.indexOf("?");
        if (p>=0) url=url.substring(p+1);        
        for (String s : url.split("&")) {
            int equals=s.indexOf("=");
            if (equals>=0) parameters.get(decodeURL(s.substring(0,equals))).add(decodeURL(s.substring(equals+1)));
            else parameters.get(decodeURL(s)).add(null);
        }
        return parameters;
    }

    public static String encodeURL(String prefix, Map<String,String> parameters) {
        if (parameters==null) return prefix;
        StringBuilder sb=new StringBuilder(prefix);
        boolean first=true;
        for (String key : parameters.keySet()) {
            sb.append(first?"?":"&");
            sb.append(encodeURL(key));
            String value = parameters.get(key);
            if (value!=null) {
                sb.append("=");
                sb.append(encodeURL(value));
            }
            first=false;
        }
        return sb.toString();
    }

    public static String encodeURLMulti(String prefix,Map<String,String[]> parameters) {
        if (parameters==null) return prefix;
        StringBuilder sb=new StringBuilder(prefix);
        boolean first=true;
        for (String key : parameters.keySet()) {
            String[] values=parameters.get(key);
            if (values==null) continue;
            for (String value : values) {
                sb.append(first?"?":"&");
                sb.append(encodeURL(key));
                if (value!=null) {
                    sb.append("=").append(encodeURL(value));
                }
                first=false;
            }


        }
        return sb.toString();
    }

    public static String queryPrefix(String target) {
        int p=target.indexOf("?");
        if (p<0) return target;
        return target.substring(0,p);
    }

    public static String queryString(String target) {
        int p=target.indexOf("?");
        if (p<0) return "";
        return target.substring(p);
    }
}
