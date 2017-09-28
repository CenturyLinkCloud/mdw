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
package com.centurylink.mdw.cli;

import java.io.File;

public class VcInfo {

    public VcInfo(String url, String user, String password, File localDir, String branch) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.localDir = localDir;
        this.branch = branch;
    }

    private String url;
    public String getUrl() { return url; }

    private String user;
    public String getUser() { return user; }

    private String password;
    String getPassword() { return password; }

    private File localDir;
    public File getLocalDir() { return localDir; }

    private String branch;
    public String getBranch() { return branch; }
}
