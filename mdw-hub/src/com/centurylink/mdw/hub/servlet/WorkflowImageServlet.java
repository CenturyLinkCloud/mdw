/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.designer.display.InstanceImageHelper;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;

/**
 * Designer workflow snapshot images.  Only works for VCS Assets.
 * TODO: Add retrieval by process id (for Assets view).
 */
public class WorkflowImageServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String processInstanceIdParam = request.getParameter("processInstanceId");
        String taskInstanceIdParam = request.getParameter("taskInstanceId");
        if (processInstanceIdParam == null && taskInstanceIdParam == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter required: processInstanceId or taskInstanceId");
            return;
        }

        try {
            long processInstanceId = 0;
            if (taskInstanceIdParam != null) {
                // process image by task instance ID
                try {
                    long taskInstanceId = Long.parseLong(taskInstanceIdParam);
                    TaskInstanceVO taskInstance = ServiceLocator.getTaskServices().getInstance(taskInstanceId);
                    if (taskInstance == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + taskInstanceId);
                    if (!OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Task instance " + taskInstanceId + " not for workflow process.");
                    processInstanceId = taskInstance.getOwnerId();
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad taskInstanceId: " + taskInstanceIdParam);
                }
            }
            else {
                // process image by process instance ID
                try {
                    processInstanceId = Long.parseLong(processInstanceIdParam);
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad processInstanceId: " + processInstanceIdParam);
                }
            }
            InstanceImageHelper helper = new InstanceImageHelper();
            WorkflowServices services = ServiceLocator.getWorkflowServices();
            ProcessInstanceVO processInstance = services.getProcess(processInstanceId);
            if (processInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + processInstanceId);
            ProcessVO process = ProcessVOCache.getProcessVO(processInstance.getProcessId());
            BufferedImage image = helper.getProcessInstanceImage(process, processInstance);
            response.setContentType("image/jpeg");
            ImageIO.write(image, "jpeg", response.getOutputStream());
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getErrorCode(), ex.getMessage());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException("Cannot create DesignerDataAccess", ex);
        }
    }
}