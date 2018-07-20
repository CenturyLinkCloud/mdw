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
package com.centurylink.mdw.auth;

public class AuthExcludePattern {
    private String path;
    private String extension;
    private String prefix;

    public AuthExcludePattern(String pattern) {
        if (pattern.endsWith("/*")) {
            pattern = pattern.substring(0, pattern.length() - 1);
            if (!pattern.startsWith("/"))
                pattern = "/" + pattern;
            this.prefix = pattern;
        }
        else if (pattern.startsWith("*.") && pattern.length() > 2) {
            this.extension = pattern.substring(2);
        }
        else {
            if (!pattern.startsWith("/"))
                pattern = "/" + pattern;
            this.path = pattern;
        }
    }

    /**
     * Checks for a match.
     *
     * @param accessPath
     * @return true if the url fits any of the patterns
     */
    public boolean match(String accessPath) {
        if (extension != null) {
            for (int i = accessPath.length() - 1; i >= 0; i--) {
                if (accessPath.charAt(i) == '.') {
                    String ext = accessPath.substring(i + 1);
                    if (extension.equals(ext)) {
                        return true;
                    }
                    break;
                }
            }
        }
        if (accessPath.equals(path)) {
            return true;
        }
        if (prefix != null && accessPath.startsWith(prefix)) {
            return true;
        }
        return false;
    }
}
