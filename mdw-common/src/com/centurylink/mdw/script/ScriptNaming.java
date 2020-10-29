package com.centurylink.mdw.script;

import com.centurylink.mdw.java.JavaNaming;

public class ScriptNaming {

    /**
     * http://jira.codehaus.org/browse/GROOVY-3054
     */
    public static String getValidName(String raw) {
        return JavaNaming.getValidClassName(raw).replace('-', '_');
    }
}
