/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLister {

    File dir;
    List<File> fileList;

    public FileLister(File dir) {
        this.dir = dir;
    }

    public List<File> list() {
        fileList = new ArrayList<File>();
        add(dir);
        return fileList;
    }

    private void add(File node) {
        if (!node.getName().equals(".metadata")) { // annoying Dimensions files
            if (node.isDirectory()) {
                if (!node.equals(dir))
                  fileList.add(node);
                for (File sub : node.listFiles())
                    add(sub);
            }
            else {
                fileList.add(node);
            }
        }
    }
}
