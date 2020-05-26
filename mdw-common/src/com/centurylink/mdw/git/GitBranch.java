package com.centurylink.mdw.git;

import com.centurylink.mdw.model.Jsonable;

/**
 * Git branch known to remote origin.
 */
public class GitBranch implements Jsonable {

    public String id;
    public String getId() { return id; }

    private String name;
    public String getName() { return name; }

    public GitBranch(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
