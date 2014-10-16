package uk.ac.ebi.interpro.common;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 31-Oct-2004
 * Time: 22:32:19
 * To change this template use File | Settings | File Templates.
 */
public class StringProcess {
    private String target;
    private int location;
    private String stringStart="\"",stringEnd="\"",escape="\'";
    private String delimiters=" ";

//    public static class Symbol {
//        String get(StringProcess sp) {
//            char ch;
//            while (!sp.atEnd() && (ch=sp.current())) {
//
//                sp.next();
//            }
//        }
//    }
//
//    public static class String {
//        String get(StringProcess sp) {
//            char ch;
//            while (!sp.atEnd() && (ch=sp.current())) {
//
//                sp.next();
//            }
//        }
//    }

    public StringProcess(String target) {
        this.target = target;
    }

    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }

    public void setString(char stringStart,char stringEnd) {
        this.stringStart = ""+stringStart;
        this.stringEnd = ""+stringEnd;
    }

    public void setString(String stringStart,String stringEnd) {
        this.stringStart = stringStart;
        this.stringEnd = stringEnd;
    }

    public void setStringEscape(char escape) {
        this.escape=""+escape;
    }

    public void setStringEscape(String escape) {
        this.escape=""+escape;
    }

    public void disableEscape() {
        this.escape="";
    }


    public String getString() {
        int stringType=stringStart.indexOf(current());
        if (stringType<0) return null;
        next();
        char endString=stringEnd.charAt(stringType);

        boolean escaped = false;
        StringBuffer sb = new StringBuffer();
        char ch;
        while (location < target.length() && (((ch = current()) != endString) || escaped)) {
            if (escaped) {
                sb.append(ch);
                escaped=false;
            } else {
                if (escape.indexOf(ch)>=0)
                    escaped = true;
                else
                    sb.append(ch);
            }
            next();
        }

        if ((location < target.length() && (ch = current()) == endString))
            next();
               
        return sb.toString();
    }
    private void next() {
        location++;
    }

    public char current() {
        return target.charAt(location);
    }




    public void ignore(String delims) {
        while ((location < target.length()) && (delims.indexOf(current()) != -1))
            next();
    }

    public String getSymbol() {
        return getSymbol(delimiters);
    }

    public String getSymbol(String delimiters) {
        int was = location;
        while ((location < target.length()) && (delimiters.indexOf(current()) == -1))
            next();
        int now=location;

        return target.substring(was, now);
    }

    public String get() {
        String s=getString();
        if (s!=null) return s;            
        return getSymbol();
    }

    public boolean is(String test) {
        if (location + test.length() > target.length()) return false;
        if (!target.substring(location, location + test.length()).equals(test)) return false;
        location += test.length();
        return true;
    }


    public boolean atEnd() {
        return location >= target.length();
    }
}
