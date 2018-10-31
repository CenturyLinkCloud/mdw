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
package com.centurylink.mdw.model;

import com.centurylink.mdw.model.system.MdwVersion;

import java.io.File;
import java.util.List;
import java.util.SortedMap;

public interface Project {

    File getAssetRoot();

    String getHubRootUrl();

    MdwVersion getMdwVersion();

    default String readData(String name) {
        return null;
    }

    default List<String> readDataList(String name) {
        return null;
    }

    default SortedMap<String,String> readDataMap(String name) {
        return null;
    }
}
