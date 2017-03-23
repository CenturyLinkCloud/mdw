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

public interface VersionControl {
    public void connect(String repositoryUrl, String user, String password, File localDir) throws IOException;
    public long getId(File file) throws IOException;
    public File getFile(long id);
    public AssetRevision getRevision(File file) throws IOException;
    public void setRevision(File file, AssetRevision rev) throws IOException;
    public void clearId(File file);
    public void deleteRev(File file) throws IOException;
    public void clear();
}
