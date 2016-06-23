/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.system.SystemUtil;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugins.CommonThreadPool;

public class SystemInfo implements TextService {

    public static final String PARAM_INFO_TYPE = "type";
    public static final String TYPE_THREAD_DUMP = "threadDump";
    public static final String TYPE_THREAD_DUMP_COUNT = "threadDumpCount";
    public static final String TYPE_POOL_STATUS = "threadPoolStatus";
    public static final String TYPE_MEM_INFO = "memoryInfo";
    public static final String TYPE_TOP_OUTPUT = "topOutput";

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String infoType = parameters.get(PARAM_INFO_TYPE) == null ? null : parameters.get(PARAM_INFO_TYPE).toString();
        if (infoType == null) {
            throw new ServiceException("Missing parameter: 'type' is required.");
        }

        if (infoType.equals(TYPE_THREAD_DUMP)) {
            return new SystemUtil().getThreadDump();
        }
        else if (infoType.equals(TYPE_THREAD_DUMP_COUNT)) {
            return new SystemUtil().getThreadDumpCount();
        }
        else if (infoType.equals(TYPE_POOL_STATUS)) {
            ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
            if (!(threadPool instanceof CommonThreadPool))
                throw new ServiceException("ThreadPoolProvider is not MDW CommonThreadPool");
            return ((CommonThreadPool)threadPool).currentStatus();
        }
        else if (infoType.equals(TYPE_MEM_INFO)) {
            return new SystemUtil().getMemoryInfo();
        }
        else if (infoType.equals(TYPE_TOP_OUTPUT)) {
            return new SystemUtil().getTopInfo();
        }
        else {
            throw new ServiceException("Unsupported type: " + infoType);
        }
    }

}
