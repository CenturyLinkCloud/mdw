package com.centurylink.mdw.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledJob {

    /**
     * Value is the job name.
     */
    String value();

    /**
     * Use an expression for dynamic values.
     */
    String schedule();

    String enabled() default "";

    boolean defaultEnabled() default true;

    boolean isExclusive() default false;
}
