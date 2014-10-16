package uk.ac.ebi.interpro.common;

/**
 * An interface for classes which locate
 *
 * Works the same way as JSDK RegEx
 *
 * Seeker is like a Pattern
 * Locator is like a Matcher
 */

public interface Seeker {

    interface Locator {
        boolean find();
        int start();
        int end();
        String group(int group);
        int count();
    }

    Locator matcher(String seek);

    
}
