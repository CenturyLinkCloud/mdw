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
import com.centurylink.mdw.designer.display.WorkflowImageHelper;
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
        String processIdParam = request.getParameter("processId");
        String processInstanceIdParam = request.getParameter("processInstanceId");
        String taskInstanceIdParam = request.getParameter("taskInstanceId");
        if (processIdParam == null && processInstanceIdParam == null && taskInstanceIdParam == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Parameter required: processId, processInstanceId or taskInstanceId");
            return;
        }

        try {
            long processId = 0;
            long processInstanceId = 0;

            if (processIdParam != null) {
                // process image by process instance ID
                try {
                    processId = Long.parseLong(processIdParam);
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad processId: " + processIdParam);
                }
            }

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
                    if (processInstanceIdParam != null && !String.valueOf(processInstanceId).equals(processInstanceIdParam))
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Task instance " + taskInstanceId + " is not for process instance " + processInstanceIdParam);
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad taskInstanceId: " + taskInstanceIdParam);
                }
            }

            if (processInstanceIdParam != null) {
                // process image by process instance ID
                try {
                    processInstanceId = Long.parseLong(processInstanceIdParam);
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad processInstanceId: " + processInstanceIdParam);
                }
            }

            WorkflowImageHelper helper = new WorkflowImageHelper();
            WorkflowServices services = ServiceLocator.getWorkflowServices();
            ProcessVO process = null;
            ProcessInstanceVO processInstance = null;
            if (processId > 0) {
                process = ProcessVOCache.getProcessVO(processId);
            }
            if (processInstanceId > 0) {
                processInstance = services.getProcess(processInstanceId);
                if (processInstance == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + processInstanceId);
                process = ProcessVOCache.getProcessVO(processInstance.getProcessId());
                if (processId > 0 && processId != process.getId())
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Process instance: " + processInstanceId + " is not for process " + processId);
            }

            if (process == null) {
                String msg = "Process not found for ID: " + processInstance.getProcessId();
                if (processInstanceId > 0)
                    msg += " for instance: " + processInstanceId;
                throw new ServiceException(ServiceException.NOT_FOUND, msg);
            }

            // TODO: Embedded subprocs
//            if (graph.subgraphs != null) {
//                Map<String, String> pMap = new HashMap<String, String>();
//                pMap.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
//                pMap.put("ownerId", processInstance.getId().toString());
//                pMap.put("processId", procdef.getProcessId().toString());
//                List<ProcessInstanceVO> childProcessInstances = dao
//                        .getProcessInstanceList(pMap, 0, QueryRequest.ALL_ROWS, procdef, null)
//                        .getItems();
//                for (ProcessInstanceVO childInst : childProcessInstances) {
//                    ProcessInstanceVO fullChildInfo = dao.getProcessInstanceAll(childInst.getId(), procdef);
//                    childInst.copyFrom(fullChildInfo);
//                    Long subprocId = new Long(childInst.getComment());
//                    for (SubGraph subgraph : graph.subgraphs) {
//                        if (subgraph.getProcessVO().getProcessId().equals(subprocId)) {
//                            List<ProcessInstanceVO> insts = subgraph.getInstances();
//                            if (insts == null) {
//                                insts = new ArrayList<ProcessInstanceVO>();
//                                subgraph.setInstances(insts);
//                            }
//                            insts.add(childInst);
//                            break;
//                        }
//                    }
//                }
//            }

            BufferedImage image = helper.getProcessImage(process, processInstance);
            if ("image/jpeg".equals(request.getHeader("Accept"))) {
                response.setContentType("image/jpeg");
                ImageIO.write(image, "jpeg", response.getOutputStream());
            }
            else {
                response.setContentType("image/png");
                ImageIO.write(image, "png", response.getOutputStream());
            }
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