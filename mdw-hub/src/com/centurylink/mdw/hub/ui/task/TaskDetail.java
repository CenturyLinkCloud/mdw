/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.hub.jsf.component.ActionMenu;
import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * TaskDetail with RF4 action menu.
 */
public class TaskDetail extends com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public TaskDetail() {
        super(null);
    }

    @Override
    public UIComponent getTaskActionMenu() throws UIException, CachingException {
        MenuBuilder menuBuilder = new TaskDetailMenuBuilder(getId(), this, getValidTaskActions());
        return new ActionMenu(menuBuilder);
    }

    protected void retrieveInstance(String instanceId) throws UIException {
        try {
            TaskManager taskMgr = RemoteLocator.getTaskManager();
            TaskInstanceVO taskInstance = taskMgr.getTaskInstanceVO(new Long(instanceId));
            FullTaskInstance fullTaskInst = new FullTaskInstance(taskInstance);
            fullTaskInst.setInFinalStatus(taskMgr.isInFinalStatus(taskInstance));
            if (TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId()).isMasterTask()) {
                List<FullTaskInstance> subTaskInsts = new ArrayList<FullTaskInstance>();
                for (TaskInstanceVO subTaskInst : taskMgr.getSubTaskInstances(taskInstance.getTaskInstanceId()))
                    subTaskInsts.add(new FullTaskInstance(subTaskInst));
                fullTaskInst.setSubTaskInstances(subTaskInsts);
            }
            setModelWrapper(fullTaskInst);

            if (getFullTaskInstance().isAutoformTask()) {
                retrieveInstanceData();
            }
            else if (taskInstance.isGeneralTask()) {
                // TODO: redirect to TaskManager for General Tasks
            }
            else {
                if (!getFullTaskInstance().isHasCustomPage())
                    retrieveInstanceData();
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new UIException("Error retrieving Task Detail.", ex);
        }
    }
}
