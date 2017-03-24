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
