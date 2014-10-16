package uk.ac.ebi.interpro.jxbp2;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD,ElementType.PARAMETER})
public @interface BindRegexp {
    String value();
}
