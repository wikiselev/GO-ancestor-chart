package uk.ac.ebi.quickgo.web;

import uk.ac.ebi.quickgo.web.data.*;

import java.util.*;

public class QuickGOSession {
    enum Setting {
        GO_DATABASE("GO Database");

        Setting(String description) {
        }
    }


    Map<Setting,String> settings=new EnumMap<Setting, String>(Setting.class);

    List<Term> selectedTerms=new ArrayList<Term>();

    
}
