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
package com.centurylink.mdw.dataaccess;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VersionControlDummy implements VersionControl {

    long currentId;
    Map<File,Long> file2id;
    Map<Long,File> id2file;

    public void connect(String repositoryUrl, String user, String password, File localDir) throws IOException {
        clear();
    }

    public long getId(File file) {
        Long id = file2id.get(file);
        if (id == null) {
            id = currentId++;
            file2id.put(file, id);
            id2file.put(id, file);
        }
        return id;
    }

    public File getFile(long id) {
        return id2file.get(id);
    }

    public void clearId(File file) {
        Long id = file2id.remove(file);
        if (id != null)
            id2file.remove(id);
    }

    public void clear() {
        currentId = 100;
        file2id = new HashMap<File,Long>();
        id2file = new HashMap<Long,File>();
    }

    /**
     * no multiversion support
     */
    public AssetRevision getRevision(File file) {
        return new AssetRevision();
    }

    public void setRevision(File file, AssetRevision rev) {
    }

    public void deleteRev(File file) {
    }
}
