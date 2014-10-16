package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 04-Oct-2005
 * Time: 14:02:12
 * To change this template use File | Settings | File Templates.
 */
public class StringSeeker implements Seeker {
    String[] find;
    public StringSeeker(String prefix, String postfix) {
        find= new String[]{prefix, postfix};
    }

    

    public StringSeeker(String... find) {
        this.find = find;
    }

    class StringLocator implements Locator {
        int[] location=new int[find.length];
        String seek;
        int l=0;
        public StringLocator(String seek) {
            this.seek = seek;
        }
        public boolean find() {
            for (int i = 0; i < find.length; i++) {
                l=seek.indexOf(find[i],l);
                if (l==-1) return false;
                location[i]=l;
            }
            return true;
        }
        public int start() {
            return location[0];
        }
        public int end() {
            return location[location.length-1]+find[location.length-1].length();
        }
        public String group(int group) {
            if (group==0) return seek.substring(start(),end());
            return seek.substring(location[group-1]+find[group-1].length(),location[group]);
        }


        public int count() {
            return location.length-1;
        }


        public String toString() {
            StringBuffer sb=new StringBuffer("Matches at");
            for (int i = 0; i < location.length; i++)
                sb.append(" ").append(location[i]).append("-").append(location[i]+find[i].length());
            return sb.toString();
        }
    }


    public String toString() {
        return "StringSeeker"+ CollectionUtils.dump(find);
    }

    public Locator matcher(String seek) {
        return new StringLocator(seek);
    }
}
