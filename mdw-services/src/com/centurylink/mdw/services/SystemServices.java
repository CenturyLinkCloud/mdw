/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
        Caches,
        JMS,
        MBean
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
