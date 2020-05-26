package com.centurylink.mdw.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {

    /**
     * Value corresponds to the variable type.
     */
    String value();

}
