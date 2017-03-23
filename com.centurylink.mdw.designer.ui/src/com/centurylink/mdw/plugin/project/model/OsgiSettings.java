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
package com.centurylink.mdw.plugin.project.model;

public class OsgiSettings {
    private boolean gradleBuild;

    public boolean isGradleBuild() {
        return gradleBuild;
    }

    public void setGradleBuild(boolean gradleBuild) {
        this.gradleBuild = gradleBuild;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private String artifactId;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getOutputDir() {
        if (gradleBuild)
            return "build/classes";
        else
            return "target";
    }

    public String getSourceDir() {
        return "src/main/java";
    }

    public String getResourceDir() {
        return "src/main/resources";
    }

    public String getLibDir() {
        return getResourceDir() + "/lib";
    }
}
