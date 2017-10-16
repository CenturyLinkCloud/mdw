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
package com.centurylink.mdw.dataaccess;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetRef {

    private Long definitionId;
    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long id) { this.definitionId = id; }

    /**
     * com.centurylink.mdw.demo.intro/HandleOrder.proc v0.18
     */
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public AssetRef() {
    }

    public AssetRef(String name, Long id, String ref) {
        this.name = name;
        this.definitionId = id;
        this.ref = ref;
    }

    public String toString() {
        return name + " (" + definitionId + "=" + ref + ")";
    }


    private static final Pattern pathPattern = Pattern.compile("(.*) v[0-9\\.\\[,\\)]*$");

    /**
     * File path corresponding to name.
     */
    public String getPath() {
        Matcher matcher = pathPattern.matcher(name);
        if (matcher.find()) {
            String match = matcher.group(1);
            int lastDot = match.lastIndexOf('.');
            if (lastDot == -1)
                throw new IllegalStateException("Bad asset path: " + match);
            return match.substring(0, lastDot).replace('.', '/') + match.substring(lastDot);
        }
        else {
            return null;
        }
    }
}
