/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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

    AssetFile(PackageDir parent, String fileName, AssetRevision rev) {
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