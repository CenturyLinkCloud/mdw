package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.model.workflow.LinkedProcess;
import com.centurylink.mdw.model.workflow.Process;

import java.util.List;

/**
 * Design-time workflow definition services.
 */
public interface DesignServices {

    List<Process> getProcessDefinitions(Query query) throws ServiceException;
    Process getProcessDefinition(String assetPath, Query query) throws ServiceException;
    Process getProcessDefinition(Long id) throws ServiceException;

    ActivityList getActivityDefinitions(Query query) throws ServiceException;

    List<ActivityImplementor> getImplementors() throws ServiceException;
    ActivityImplementor getImplementor(String className) throws ServiceException;

    List<LinkedProcess> getProcessHierarchy(String processAsset) throws ServiceException;
}
