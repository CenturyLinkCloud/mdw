/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.TaskInstanceObserver;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;

/**
 * The observer factory is responsible for instantiating registered task observers.
 * Multiple observer registrations per task are supported by specifying a #-delimited list.
 */
public class TaskInstanceObserverFactory {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();


    /**
     * Returns the registered task instance observers
     * @param taskId
     * @return list of the observer instances for the specified task
     */
    public static List<TaskInstanceObserver> getObservers(Long taskId) throws ObserverException {
        List<TaskInstanceObserver> observers = new ArrayList<TaskInstanceObserver>();

        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskId);

        // pre-MDW 4.2 style observers
        String classesAttr = taskVO.getAttribute(TaskAttributeConstant.OBSERVER_NAME);
        if (!StringHelper.isEmpty(classesAttr)) {
        	StringTokenizer st = new StringTokenizer(classesAttr, "# ,;");
        	while (st.hasMoreTokens()) {
        		String className = st.nextToken();
        		try {
        			TaskInstanceObserver observer = (TaskInstanceObserver) ApplicationContext.getClassInstance(className);
        			if (observer != null)
        				observers.add(observer);
        		}
        		catch (Exception ex) {
        			// don't disrupt processing; just log exception
        			logger.severeException(ex.getMessage(), ex);
        		}
        	}
        }

        return observers;
    }
}
