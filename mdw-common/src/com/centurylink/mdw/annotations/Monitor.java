package com.centurylink.mdw.annotations;

import java.lang.annotation.*;

/**
 * Use monitor annotations to expose your activity, process, adapter, service or task monitors
 * on the Configurator Monitoring tab in MDW Studio.  Enabled monitors will be notified at specified
 * lifecycle stages.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {

    /**
     * Value is the monitor label.
     */
    String value();

    /**
     * This represents the type of monitor to register.
     */
    Class<? extends com.centurylink.mdw.monitor.Monitor> category();

    /**
     * Recommended practice is to leave this set to false, and then enable
     * per activity in MDW Studio or MDWHub.  This way only designated activities
     * will trigger this monitor.  If defaultEnabled is set to true, this monitor
     * is enabled unless explicitly deselected in MDW Studio/Hub.
     */
    boolean defaultEnabled() default false;

    /**
     * Default value for user-entered options for this monitor.
     * The value entered in MDW Studio/Hub is available as the forth column in the
     * JSONArray value from attribute "monitors" in the runtimeContext.
     */
    String defaultOptions() default "";
}
