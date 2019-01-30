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

    String schedule();

    String enabledProp() default "";

}
