package com.centurylink.mdw.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.centurylink.mdw.activity.types.ActivityCategory;
import com.centurylink.mdw.activity.types.GeneralActivity;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Activity {

    /**
     * Value corresponds to the activity label.
     */
    String value();

    String icon() default "shape:activity";
    String pagelet() default "{}";

    Class<? extends ActivityCategory> category() default GeneralActivity.class;
}
