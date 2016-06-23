/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.List;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.task.TaskTemplateList;
import com.centurylink.mdw.model.value.task.TaskVO;

public interface TaskTemplateServices {

    public TaskTemplateList getTaskTemplates() throws ServiceException;
    public TaskVO getTaskTemplate(String id) throws ServiceException;
    public void createTaskTemplate(TaskVO TaskTemplate) throws ServiceException;
    public void updateTaskTemplate(TaskVO TaskTemplate) throws ServiceException;
    public void deleteTaskTemplate(String id) throws ServiceException;
    public List<TaskCategory>  getTaskCategories() throws ServiceException;

}
