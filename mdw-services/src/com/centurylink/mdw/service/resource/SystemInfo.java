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
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.system.SystemUtil;

public class SystemInfo implements TextService {

    public static final String PARAM_INFO_TYPE = "type";
    public static final String TYPE_THREAD_DUMP = "threadDump";
    public static final String TYPE_THREAD_DUMP_COUNT = "threadDumpCount";
    public static final String TYPE_POOL_STATUS = "threadPoolStatus";
    public static final String TYPE_MEM_INFO = "memoryInfo";
    public static final String TYPE_TOP_OUTPUT = "topOutput";

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        String infoType = metaInfo.get(PARAM_INFO_TYPE) == null ? null : metaInfo.get(PARAM_INFO_TYPE).toString();
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
