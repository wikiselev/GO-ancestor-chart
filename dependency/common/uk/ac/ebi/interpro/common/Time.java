package uk.ac.ebi.interpro.common;

import java.util.regex.*;

public class Time {

    private long nanos;

    public Time(String input) {
        nanos =getNanosFromString(input);
    }

    private Time(long nanos) {
        this.nanos =nanos;
    }

    public static Time ms(long input) {
        return new Time(input*MS_NS);
    }

    public static Time seconds(long input) {
        return new Time(input*SECOND_NS);
    }

    public static Time hours(long input) {
        return new Time(input*HOUR_NS);
    }

    public String toString() {
        return getTextFromNanos(nanos,false,-1);
    }


    public long getMillis() {
        return nanos/1000000;
    }

    public static final long NS_NS=1;
    public static final long US_NS=1000;
    public static final long MS_NS=1000000;
    public static final long SECOND_NS=1000*MS_NS;
    public static final long MINUTE_NS=SECOND_NS*60;
    public static final long HOUR_NS=MINUTE_NS*60;
    public static final long DAY_NS=HOUR_NS*24;
    public static final long WEEK_NS=DAY_NS*7;

    public static final String[] timeNames={"nanosecond","microsecond","millisecond","second","minute","hour","day"};
    public static final String[] timeAbbr={"ns","us","ms","s","m","h","d"};
    public static final long[] timeQuantity={NS_NS,US_NS,MS_NS,SECOND_NS,MINUTE_NS,HOUR_NS,DAY_NS};


    public static final Pattern recognize= Pattern.compile("([0-9]+) *([a-z]*)");


    public static long getMillisFromString(String input) {
        return getNanosFromString(input)/MS_NS;
    }

    /**
     * Convert time period from string to milliseconds.
     *<pre>
     * Input
     * x d[ay[s]] x h[our[s]] x m[inute[s]] x s[econd[s]] x ms|millsecond[s]
     * where x is a number. All fields optional, spaces optional.
     * If no recognised unit is specified, ms are used.
     *</pre>
     *
     * @param input Text of time
     * @return milliseconds
     */

    public static long getNanosFromString(String input) {
        long total=0;
        input=input.toLowerCase();

        Matcher m=recognize.matcher(input);
        while (m.find()) {
            String time=m.group(1);
            String text=m.group(2).toLowerCase();
            long factor=1;
            for (int i = 0; i < timeNames.length; i++)
                if (text.startsWith(timeNames[i]) || text.equals(timeAbbr[i])) factor=timeQuantity[i];
            total+=Long.parseLong(time)*factor;
        }
        return total;
    }

    /**
     * Convert time interval from milliseconds to string, using compact format.
     *
     * @param time milliseconds     
     * @return textual representation of time interval
     */
    public static String getTextFromMillis(long time) {
        return getTextFromNanos(time*MS_NS,false,-1);
    }

    public static String getTextFromNanos(long time) {
        return getTextFromNanos(time,false,-1);
    }

    /**
     * Convert time interval from nanoseconds to string.
     *
     * @param time nanoseconds
     * @param english use english format ("seconds") instead of compact format ("s")
     * @param limit how much detail (or negative for unlimited)
     * @return textual representation of time interval
     */

    public static String getTextFromNanos(long time,boolean english,int limit) {
        if (time==0) return "0";
        StringBuffer sb=new StringBuffer();
        boolean start=false;
        for (int i = timeQuantity.length-1;i>=0; i--) {
            if (start) limit--;
            if (limit==0) break;
            long q;
            if (i<timeQuantity.length-1) q=time%timeQuantity[i+1]/timeQuantity[i];
            else q=time/timeQuantity[i];
            if (q==0) continue;
            start=true;
            sb.append(q);
            if (english) sb.append(" ").append(timeNames[i]);
            else sb.append(timeAbbr[i]);
            if (english && q>1) sb.append("s");

            sb.append(" ");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(getTextFromNanos(DAY_NS*3+MINUTE_NS,true,2));
        System.out.println(getTextFromNanos(DAY_NS*3+MINUTE_NS,true,3));
        System.out.println(getTextFromNanos(HOUR_NS*3+SECOND_NS*88+44,false,-1));
        System.out.println(getNanosFromString(".. 1 day 3 hours 77ms 8US"));
    }



}
