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
package com.centurylink.mdw.dataaccess.file;

import java.io.File;

import com.centurylink.mdw.dataaccess.AssetRevision;

public class AssetFile extends File {

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private AssetRevision revision;
    public AssetRevision getRevision() { return revision; }
    public void setRevision(AssetRevision rev) { this.revision = rev; }

    private PackageDir parent;
    public PackageDir getPackage() { return parent; }

    public AssetFile(PackageDir parent, String fileName, AssetRevision rev) {
        super(parent + "/" + fileName);
        this.parent = parent;
        logicalFile = new File(parent.getPackageName() + "/" + getName() + " " + rev.getFormattedVersion());
        this.revision = rev;
    }

    /**
     * com.centurylink.mdw.demo.intro/HandleOrder.proc v0.18
     */
    private File logicalFile;
    public File getLogicalFile() { return logicalFile; }
}