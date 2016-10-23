/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
