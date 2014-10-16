package uk.ac.ebi.interpro.common;

import java.util.regex.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 04-Oct-2005
 * Time: 13:56:24
 * To change this template use File | Settings | File Templates.
 */
public class RegexSeeker implements Seeker {

    public class RegexLocator implements Locator {
        Matcher base;
        public RegexLocator(Matcher base) {
            this.base = base;
        }
        public boolean find() {
            return base.find();
        }
        public int start() {
            return base.start();
        }
        public int end() {
            return base.end();
        }
        public String group(int group) {

            return base.group(group);
        }

        public int count() {
            return base.groupCount();
        }
    }

    Pattern base;

    public RegexSeeker(Pattern pattern) {
        this.base = pattern;
    }

    public RegexSeeker(String regex) {
        this.base = Pattern.compile(regex);
    }

    public Locator matcher(String seek) {
        return new RegexLocator(base.matcher(seek));
    }

    public static void main(String[] args) {
        Locator locator = new RegexSeeker("(?:1|A)(.*)(?:2|B)").matcher("AXB");
        locator.find();
        System.out.println(locator.group(1));
    }
}
