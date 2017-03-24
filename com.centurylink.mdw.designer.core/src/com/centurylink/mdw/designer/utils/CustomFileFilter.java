/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
