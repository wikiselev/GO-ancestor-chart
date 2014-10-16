package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import java.lang.reflect.*;

public class FieldIntrospectiveMap extends AbstractMap<String,Object> {

    Set<Entry<String,Object>> entries=new HashSet<Entry<String,Object>>();

    public FieldIntrospectiveMap(final Object object) {
        

        final Field[] fields = object.getClass().getFields();
        for (final Field field : fields) {
            entries.add(new Entry<String, Object>() {
                public String getKey() {
                    return field.getName();
                }

                public Object getValue() {
                    try {
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }

                public Object setValue(Object o) {
                    try {
                        Object v = getValue();
                        field.set(object, o);
                        return v;
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }
            });
        }
    }


    public Object put(String key, Object value) {
        for (Entry<String, Object> entry : entries) {
            if (entry.getKey().equals(key)) {
                Object v=entry.getValue();
                entry.setValue(value);
                return v;
            }
        }
        throw new IllegalArgumentException("No such field "+key);
    }

    public Set<Entry<String,Object>> entrySet() {
        return entries;

    }

    public String toString() {
        return CollectionUtils.dump(this);
    }


}
