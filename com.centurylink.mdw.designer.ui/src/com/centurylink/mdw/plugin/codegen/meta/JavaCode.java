/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.meta;

public class JavaCode extends Code {
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    private String className;

    public String getClassName() {
        return className;
    }

    public JavaCode(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

}
