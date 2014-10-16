package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.xml.*;

import java.util.*;

import org.w3c.dom.*;

public class Features {
    private List<String> failedValues=new ArrayList<String>();

    public enum Name {Page,GAnnotation,InteractiveSearch,HierarchyPopup,CompareTerm}

    public Map<Name,String> values=new EnumMap<Name, String>(Name.class);
    private String prefix="/QuickGO/settings/";

    Features parent;
    /*SingleSignOn.SSOUser user;*/

    /*public void save() throws Exception {
        if (user!=null) {
            user.lock();
            for (Name n : values.keySet())
                user.set(prefix+n,values.get(n));
            user.finish();
        }
    }

    void load() {
        for (Name n : Name.values())
            values.put(n,user.get(prefix+n));

    }*/

    public Features(Element elt) {
        List<Element> fes=XMLUtils.getChildElements(elt,"feature");
        for (Element fe : fes) {
            String nameText = fe.getAttribute("name");
            try {
                values.put(Name.valueOf(nameText), XMLUtils.getInnerText(fe));
            } catch (Exception e) {
                failedValues.add(nameText);
            }
        }
    }

    public Features(Features parent/*,SingleSignOn.SSOUser user*/) {
        this.parent = parent;
/*        this.user = user;
        if (user!=null) load();*/
    }

    String get(Name name) {
        if (values.containsKey(name)) return values.get(name);
        if (parent!=null) return parent.get(name);
        return null;
    }

    boolean is(Name name) {
        return "true".equals(get(name));
    }
}
