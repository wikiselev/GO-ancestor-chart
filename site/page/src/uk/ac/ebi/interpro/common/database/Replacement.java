package uk.ac.ebi.interpro.common.database;


import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;

public class Replacement {
    String name;
    String value;
    public List<String> list=new ArrayList<String>();


    public void set(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }


    public String getValue() {
        return value;
    }

    public String toString() {
        if (value!=null) return value;
        return CollectionUtils.concat(list,", ");
    }

    public void add(String value) {
        list.add(value);
    }

    public Replacement(String name,String value) {
        this.value = value;
        this.name = name;
    }

    public Replacement(String name) {
        this.name = name;
    }
}
