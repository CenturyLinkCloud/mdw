/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.List;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.task.TaskTemplateList;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.services.TaskTemplateServices;
import com.centurylink.mdw.services.dao.task.TaskDAO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;

public class TaskTemplateServicesImpl implements TaskTemplateServices {

    public TaskTemplateList getTaskTemplates() throws ServiceException {
        try {
            ProcessLoader processLoader = DataAccess.getProcessLoader();
            List<TaskVO> taskVoList = processLoader.getTaskTemplates();
            return new TaskTemplateList(taskVoList);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public TaskVO getTaskTemplate(String taskId) throws ServiceException {
        try {

            return (TaskTemplateCache.getTaskTemplate(Long.parseLong(taskId)));
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);

        }

    }

    public void createTaskTemplate(TaskVO TaskTemplate) throws ServiceException {

    }

    public void updateTaskTemplate(TaskVO taskTemplate) throws ServiceException {
        try {

            TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));
            taskDao.saveTask(taskTemplate, true);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);

        }

    }

    /**
     * TODO: Delete A Task Template based on Task Id
     */
    public void deleteTaskTemplate(String id) throws ServiceException {

    }

    public List<TaskCategory> getTaskCategories() throws ServiceException {
        try {
            TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));
            return (taskDao.getAllTaskCategories());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
