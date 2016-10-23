/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import com.centurylink.mdw.util.file.FileHelper;

public class JavaNaming {

    public static String getValidPackageName(String raw) {
        String out = FileHelper.stripDisallowedFilenameChars(raw);
        return out.replaceAll(" ", "");
    }

    public static String getValidClassName(String raw) {
        String out = raw.endsWith(".java") ? raw.substring(0, raw.length() - 5) : raw;
        out = FileHelper.stripDisallowedFilenameChars(out);
        for (int i = 0; i < out.length(); i++) {
            if (!Character.isJavaIdentifierPart(out.charAt(i)))
                out = out.replace(out.charAt(i), '_');
        }
        return out;
    }
    // To get the class name from qualified name
    public static String getClassName(String qualifiedName) {
        if (qualifiedName.lastIndexOf(".") > 0 )
            return qualifiedName.substring(qualifiedName.lastIndexOf(".")+1);
        else
            return qualifiedName;
    }

}
