/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.Date;

public class AssetRevision {

    private int version;
    private String modUser;
    private Date modDate;
    private String comment;

    public int getVersion() { return version; }
    public void setVersion(int ver) { this.version = ver; }

    public String getFormattedVersion() {
        return "v" + getVersionString();
    }

    public String getVersionString() {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;

    }

    public String getModUser() { return modUser; }
    public void setModUser(String user) { this.modUser = user; }

    public Date getModDate() { return modDate; }
    public void setModDate(Date date) { this.modDate = date; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

}
