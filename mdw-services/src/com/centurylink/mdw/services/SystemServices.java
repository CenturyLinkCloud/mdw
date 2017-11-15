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
package com.centurylink.mdw.services;

import java.util.List;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.system.SysInfoCategory;

public interface SystemServices {

    public enum SysInfoType {
        System,
        Thread,
        Class,
        Git,
        Caches,
        JMS,
        MBean,
        Exit // only works from localhost
    }

    public List<SysInfoCategory> getSysInfoCategories(SysInfoType type, Query query) throws ServiceException;
    public SysInfoCategory getSystemInfo();
    public SysInfoCategory getDbInfo();
    public SysInfoCategory getSystemProperties();
    public SysInfoCategory getEnvironmentVariables();
    public SysInfoCategory getMdwProperties();
    public SysInfoCategory findClass(String className, ClassLoader classLoader);
    public SysInfoCategory findClass(String className);
}
