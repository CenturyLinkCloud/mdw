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

/**
 * Not really just for OSGi builds anymore.
 */
public interface OsgiBuildFile {
    public String getVersion();

    public String getMdwVersion();

    public String getOutputDirectory();

    public String getArtifactGenDir();

    public String getArtifactName();

    public boolean exists() throws Exception;

    public long lastModified();

    public String parseSymbolicName() throws Exception;

    public OsgiManifestDescriptor parse() throws Exception;

}
