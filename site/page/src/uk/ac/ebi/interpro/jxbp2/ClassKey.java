package uk.ac.ebi.interpro.jxbp2;

public class ClassKey implements Key {
    Class<?> c;

    public ClassKey(Class<?> c) {
        this.c = c;
    }

    public boolean matches(Object o) {
        //System.out.println("?? "+o.getClass().getName()+" "+c+" "+c.isInstance(o));
        return c.isInstance(o);
    }


    public String toString() {
        return c.getCanonicalName();
    }
}
