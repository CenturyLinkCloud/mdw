/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.groovy;

import com.centurylink.mdw.java.JavaNaming;

public class GroovyNaming {

    /**
     * http://jira.codehaus.org/browse/GROOVY-3054
     */
    public static String getValidClassName(String raw) {
        return JavaNaming.getValidClassName(raw).replace('-', '_');
    }

}
