/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Differences vs. remote.  Paths are relative to git root.
 */
public class GitDiffs {

    public static enum DiffType {
        /**
         * Files deleted from the repo, or local files not in repository (eg: unstaged).
         */
        EXTRA,
        /**
         * Files added to the repo, or deleted locally but still in repo.
         */
        MISSING,
        /**
         * Locally modified, remotely modified or conflicting.
         */
        DIFFERENT
    }

    private Map<DiffType,List<String>> diffs = new HashMap<DiffType,List<String>>();
    public Map<DiffType,List<String>> getDiffs() { return diffs; }

    public GitDiffs() {
        for (DiffType diffType : DiffType.values())
            diffs.put(diffType, new ArrayList<String>());
    }

    public List<String> getDiffs(DiffType type) {
        List<String> list = diffs.get(type);
        Collections.sort(list);
        return list;
    }

    public DiffType getDiffType(String path) {
        for (DiffType diffType : DiffType.values()) {
            for (String diff : getDiffs(diffType)) {
                if (diff.equals(path))
                    return diffType;
            }
        }
        return null;
    }

    public void add(DiffType type, String path) {
        List<String> list = diffs.get(type);
        if (!list.contains(path))
            list.add(path);
    }

    public boolean isEmpty() {
        for (DiffType diffType : DiffType.values()) {
            if (!getDiffs(diffType).isEmpty())
                return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DiffType diffType : DiffType.values()) {
            for (String diff : getDiffs(diffType))
                sb.append(diffType).append(": ").append(diff).append("\n");
        }
        return sb.toString();
    }

}
