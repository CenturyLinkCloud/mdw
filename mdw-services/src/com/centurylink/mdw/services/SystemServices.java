package com.centurylink.mdw.services;

import java.io.IOException;
import java.util.List;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.system.SysInfoCategory;

public interface SystemServices {

    enum SysInfoType {
        System,
        Thread,
        Class,
        CLI,
        Caches,
        JMS,
        MBeans,
        Memory,
        Exit // only works from localhost
    }

    List<SysInfoCategory> getSysInfoCategories(SysInfoType type, Query query) throws ServiceException;
    SysInfoCategory getSystemInfo();
    SysInfoCategory getDbInfo();
    SysInfoCategory getSystemProperties();
    SysInfoCategory getMdwProperties();
    SysInfoCategory findClass(String className, ClassLoader classLoader);
    SysInfoCategory findClass(String className);
    String runCliCommand(String command) throws IOException, InterruptedException;
}
