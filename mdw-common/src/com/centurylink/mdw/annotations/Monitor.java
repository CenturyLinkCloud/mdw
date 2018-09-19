/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {

    /**
     * Value is the monitor label.
     */
    String value();

    /**
     * This represents the type of monitor to (optionally register).
     */
    public Class<? extends com.centurylink.mdw.monitor.Monitor> category();

    /**
     * Recommended practice is to leave this set to false, and to allow
     * per-activity configuration in MDW Studio or MDWHub.  This way only
     * designated activities will trigger this monitor.
     * If global is true, the enablement checkbox is readonly in MDW Studio/Hub.
     */
    public boolean global() default false;

    /**
     * If true, monitor will be invoked asynchronously (and any return
     * values are not honored).
     */
    public boolean offline() default false;

    /**
     * Default value for user-entered options for this monitor.
     * The value entered in MDW Studio/Hub is available as attribute "Options"
     * in the runtimeContext.
     */
    public String defaultOptions() default "";
}
