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
package com.centurylink.mdw.container.plugin;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * <p>
 * Pretty much does what CommonThreadPool.recordReject does.
 * <b>Note</b> that it does not requeue the rejected task
 * </p>
 * @author aa70413
 *
 */
public class MDWRejectedExecutionHandler implements RejectedExecutionHandler {


    private StandardLogger logger=LoggerUtil.getStandardLogger();
    /* (non-Javadoc)
     * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        logger.severe("+++Reject work " + r.toString() + ": pool Size=" + executor.getPoolSize()+" : maxPoolSize="+executor.getMaximumPoolSize());


    }

}
