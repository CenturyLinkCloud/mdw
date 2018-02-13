/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

public class Repository implements Jsonable {

    private String provider;
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    private String branch;
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    private String commit;
    public String getCommit() { return commit; }
    public void setCommit(String commit) { this.commit = commit; }

}
