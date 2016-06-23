/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class CustomFileFilter extends FileFilter {
    
    public final static String XML = ".xml";
    public final static String XPDL = ".xpdl";
    public final static String PROCESS = ".process";
    
    private String extension;

    public CustomFileFilter(String extension) {
        this.extension = extension;
    }
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        String filename = file.getName();
        return filename.endsWith(extension);
    }
    public String getDescription() {
    	if (extension.equals(XPDL)) return "XPDL files";
    	else if (extension.equals(XPDL)) return "Process files";
    	else if (extension.equals(".jar")) return "Jar files";
    	else return "XML files";
    }
}
