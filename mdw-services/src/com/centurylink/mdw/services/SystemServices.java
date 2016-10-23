/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.List;

import com.centurylink.mdw.model.system.SysInfoCategory;

public interface SystemServices {

    public enum SysInfoType {
        System,
        Thread,
        JMS,
        Caches,
        MBean
    }

    public List<SysInfoCategory> getSysInfoCategories(SysInfoType type);
    public SysInfoCategory getSystemInfo();
    public SysInfoCategory getDbInfo();
    public SysInfoCategory getSystemProperties();
    public SysInfoCategory getEnvironmentVariables();
    public SysInfoCategory getMdwProperties();
}
