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
import java.util.List;
import java.util.Map;

public interface VersionControl {
    void connect(String repositoryUrl, String user, String password, File localDir) throws IOException;
    long getId(File file) throws IOException;
    File getFile(long id);
    AssetRevision getRevision(File file) throws IOException;
    void setRevision(File file, AssetRevision rev) throws IOException;
    void clearId(File file);
    void deleteRev(File file) throws IOException;
    void clear();
    boolean exists();
    void hardCheckout(String branch, Boolean hard) throws Exception;
    String getCommit() throws IOException;
}
