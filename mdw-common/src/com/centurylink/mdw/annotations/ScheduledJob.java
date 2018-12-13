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

    public String schedule();

    public boolean enabled() default true;

    public String enabledProp() default "";

}
