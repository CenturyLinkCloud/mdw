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
package com.centurylink.mdw.plugin.designer.model;

import java.util.Date;

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public interface Versionable {
    public enum Increment {
        Major, Minor, Overwrite
    }

    public WorkflowProject getProject();

    public String getName();

    public String getTitle();

    public int getVersion();

    public void setVersion(int version);

    public String getVersionLabel();

    public String getVersionString();

    public int getNextMajorVersion();

    public int getNextMinorVersion();

    public int parseVersion(String versionString) throws NumberFormatException;

    public String formatVersion(int version);

    public String getLockingUser();

    public void setLockingUser(String lockUser);

    public Date getModifyDate();

    public void setModifyDate(Date modDate);

    public String getExtension();
}
