/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
