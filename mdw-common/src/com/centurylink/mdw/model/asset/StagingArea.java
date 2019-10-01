package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.model.Jsonable;

public class StagingArea implements Jsonable {

    public StagingArea(String userCuid, String userName) {
        this.userCuid = userCuid;
        this.userName = userName;
        this.loaded = System.currentTimeMillis();
    }

    private String userCuid;
    public String getUserCuid() { return userCuid; }

    private String userName;
    public String getUserName() { return userName; }

    private GitBranch branch;
    public GitBranch getBranch() { return branch; }
    public void setBranch(GitBranch branch) { this.branch = branch; }

    private long loaded;
    public long getLoaded() { return loaded; }
}
