package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import java.lang.reflect.*;


public class Creator {

    public interface KeyFactory<K,T> {
        T make(K key);

    }

    public interface Factory<T> {
        T make();
    }

    public static <X> Factory<List<X>> arrayList() {return new Factory<List<X>>() {
        public ArrayList<X> make() {return new ArrayList<X>();}
    };
    }


    public static <K,X> Factory<Map<K,X>> hashMap() {
        return new Factory<Map<K,X>>() {
            public Map<K,X> make() {
                return new HashMap<K,X>();
            }
        };
    }
    
    public static <K,X> Factory<Map<K,X>> autoHashMap(final Factory<X> member) {
        return new Factory<Map<K,X>>() {
            public Map<K,X> make() {
                return new AutoMap<K,X>(new HashMap<K,X>(), member);
            }
        };
    }

    public static <K,X> Factory<Map<K,X>> autoHashMap(final KeyFactory<K,X> member) {
        return new Factory<Map<K,X>>() {
            public Map<K,X> make() {
                return new AutoMap<K,X>(new HashMap<K,X>(), member);
            }
        };
    }

    public static <T> Factory<T> reflective(final Class<T> target) {

        return new Factory<T>() {
            public T make() {
            //public Object make() {
                try {
                    return target.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected instantiation exception",e);
                }
            }
        };
    }

    public static <T> Constructor<T> findConstructor(Class<T> c,Class... params) {
        try {
            return c.getConstructor(params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }


    public static <K,T> KeyFactory<K,T> reflective(final Class<T> target, final Class<K> key, final Object... constructorParams) {

        Class[] paramClasses=new Class[constructorParams.length];

        for (int i = 0; i < constructorParams.length; i++) paramClasses[i] = constructorParams[i].getClass();

        final Constructor<T> c1=findConstructor(target,key);
        if (c1!=null)
            return new KeyFactory<K,T>() {
                public T make(K k) {
                    try {
                        return c1.newInstance(k);
                    } catch (Exception e) {
                        throw new RuntimeException("Unexpected instantiation exception calling "+target.getCanonicalName()+"("+key.getCanonicalName()+")",e);
                    }
                }
            };
        final Constructor<T> c0=findConstructor(target,paramClasses);
        if (c0!=null)
            return new KeyFactory<K,T>() {
                public T make(K k) {
                    try {
                        return c0.newInstance(constructorParams);
                    } catch (Exception e) {
                        throw new RuntimeException("Unexpected instantiation exception calling "+target.getCanonicalName()+"()",e);
                    }
                }
            };
        return new KeyFactory<K,T>() {
            public T make(K k) {
                throw new RuntimeException("No suitable constructor for "+target.getCanonicalName());
            }
        };
    }

//    public static class X {}
//    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
////        for (Constructor constructor : X.class.getConstructors()) {
////            System.out.println(constructor.toString());
////        };
//        System.out.println(X.class.getConstructor().newInstance().getClass());
//    }
}