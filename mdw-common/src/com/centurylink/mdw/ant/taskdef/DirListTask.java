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
package com.centurylink.mdw.ant.taskdef;

// java sdk
import java.io.File;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

/***
 * Ant task to list out the contents of a given directory.
 */
public class DirListTask extends MatchingTask {

    private File mDir;
    private Hashtable<String,String> mDirList;

    /**
     * Attribute 'dir' setter
     * @param aDir
     */
    public void setDir (File aDir) {
      mDir = aDir;
    }

    /**
     * Execute TASK
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (mDir == null) {
            throw new BuildException("dir must be specified");
        }

        DirectoryScanner ds = getDirectoryScanner(mDir);
        String[] files = ds.getIncludedFiles();
        mDirList = new Hashtable<String,String>();
        for (int i=0; i < files.length; i++) {
            String fileName = this.tokenize(files[i], "/\\");
            mDirList.put(fileName, (mDir + "/" + files[i]));
        }
    }

    /**
     * Tokenize the passed in string by the delim supplied.
     * @param aFilePath
     * @param aDelim
     * @return String -- final String from the aFilePath
     */
    public String tokenize(String aFilePath, String aDelim) {
        String fileName = aFilePath;
        StringTokenizer st = new StringTokenizer(aFilePath, aDelim);

        // We are interested in the last token (file name with extension)
        while(st.hasMoreTokens()) {
            fileName = st.nextToken();
        }
        return fileName;
    }

    /**
     * Return the file names and their associated abs path map
     * @return Hashtable
     */
    public Hashtable<String,String> getDirList() {
        return mDirList;
    }
}
