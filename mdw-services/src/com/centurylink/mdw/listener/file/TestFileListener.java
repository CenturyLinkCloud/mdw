/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.file;

import java.io.File;
import java.util.Date;

public class TestFileListener extends FileListener {

    @Override
    public void reactToFile(File file) {
        System.out.println(new Date() + "processing file: " + file);
    }

}
