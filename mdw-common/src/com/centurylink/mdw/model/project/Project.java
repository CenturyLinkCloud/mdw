/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.model.project;

import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.ActivityImplementor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

@SuppressWarnings("unused")
public interface Project {

    File getAssetRoot() throws IOException;

    String getHubRootUrl() throws IOException;

    MdwVersion getMdwVersion() throws IOException;

    Data getData();

    default String readData(String name) throws IOException {
        return null;
    }

    default List<String> readDataList(String name) throws IOException {
        return null;
    }

    default SortedMap<String,String> readDataMap(String name) throws IOException {
        return null;
    }

    default Map<String,ActivityImplementor> getActivityImplementors() throws IOException {
        return null;
    }
}
