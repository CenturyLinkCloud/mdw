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
package com.centurylink.mdw.services.request;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.monitor.LoadBalancedScheduledJob;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;
import org.apache.xmlbeans.XmlObject;

import java.util.Map;
import java.util.TimerTask;

/**
 * Used for old-school (non-service) request handling.
 * Strictly for MDW internal use.
 */
public class FallbackRequestHandler implements RequestHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * The handler creates a standard error response message.
     * The status code and status message are set in the following
     * ways:
     * <ul>
     *    <li>If the message cannot be parsed by the generic XML Bean,
     *         the status code is set to -2 and the status message is
     *         status message "failed to parse XML message"</li>
     *    <li>If there is no matching handler found, but the root element
     *         is "ping", the status code is set to 0 and and the
     *         status message is set to the content of the ping in the
     *         request</li>
     *    <li>Otherwise, the status code is set to -3 and the status message
     *         is set to "No event handler has been configured for message"</li>
     * </ul>
     */
    public Response handleRequest(Request request, Object message, Map<String,String> headers) {
        String msg;
        int code;
        if (message == null) {
            msg = "failed to parse request message";
            code = ListenerHelper.RETURN_STATUS_NON_XML;
        } else {
            String rootNodeName = XmlPath.getRootNodeName((XmlObject)message);
            if (rootNodeName != null && rootNodeName.equals("ping")) {
                msg = XmlPath.evaluate((XmlObject)message, "ping");
                if (msg == null)
                    msg = "ping is successful";
                code = ListenerHelper.RETURN_STATUS_SUCCESS;
            } else {
                msg = "No request handler has been configured for message";
                code = ListenerHelper.RETURN_STATUS_NO_HANDLER;
                logger.error(msg + ": " + message);
            }
        }
        StatusMessage statusMessage = new StatusMessage(code, msg);
        if (request.getContent().trim().startsWith("{")) {
            return new Response(statusMessage.getJsonString());
        }
        else {
            return new Response(statusMessage.getXml());
        }
    }

    public String handleSpecialEventMessage(XmlObject msgdoc) {
        String rootNodeName = XmlPath.getRootNodeName(msgdoc);
        String response;
        if (rootNodeName.equals("_mdw_property")) {
            String propname = XmlPath.getRootNodeValue(msgdoc);
            response = PropertyManager.getProperty(propname);
            if (response == null)
                response = "";
        } else if (rootNodeName.equals("_mdw_run_job")) {
            String classNameAndArgs = XmlPath.getRootNodeValue(msgdoc);
            CallURL url = new CallURL(classNameAndArgs);
            String className = url.getAction();
            Object aTimerTask;
            try {
                ScheduledJob job = MdwServiceRegistry.getInstance().getScheduledJob(className);
                if (job != null) {
                    boolean enabled = true;
                    boolean exclusive = false; // means only one can run at a time
                    com.centurylink.mdw.annotations.ScheduledJob scheduledJobAnnotation =
                            job.getClass().getAnnotation(com.centurylink.mdw.annotations.ScheduledJob.class);
                    if (scheduledJobAnnotation != null) {
                        enabled = scheduledJobAnnotation.defaultEnabled();
                        String enabledProp = scheduledJobAnnotation.enabled();
                        if (enabledProp.startsWith("${props['") && enabledProp.endsWith("']}")) {
                            enabledProp = enabledProp.substring(9, enabledProp.length() - 3);
                            enabled = PropertyManager.getBooleanProperty(enabledProp, enabled);
                        }
                        else if (!enabledProp.isEmpty()) {
                            enabled = "true".equalsIgnoreCase(enabledProp);
                        }
                        exclusive = scheduledJobAnnotation.isExclusive();
                    }
                    if (enabled) {
                        logger.debug("Running scheduled job: " + job.getClass());
                        if (job instanceof LoadBalancedScheduledJob) {
                            ((LoadBalancedScheduledJob)job).runOnLoadBalancedInstance(url);
                        }
                        else {
                            if (exclusive) {
                                EventServices eventServices = ServiceLocator.getEventServices();
                                eventServices.runScheduledJobExclusively(job, url);
                            }
                            else {
                                // don't care when or if completed
                                job.run(url, s -> {});
                            }
                        }
                    }
                    else {
                        logger.debug("Scheduled job is disabled: " + job.getClass());
                    }
                }
                else {
                    aTimerTask = Class.forName(className).newInstance();
                    if (aTimerTask instanceof ScheduledJob) {
                        if (aTimerTask instanceof LoadBalancedScheduledJob) {
                            ((LoadBalancedScheduledJob) aTimerTask).runOnLoadBalancedInstance(url);
                        }
                        else {
                            ((ScheduledJob) aTimerTask).run(url, s -> {});
                        }
                    }
                    else
                        ((TimerTask) aTimerTask).run(); // for backward compatibility
                }
            }
            catch (Exception ex) {
                logger.error("Failed to create instance for scheduled job " + className, ex);
            }
            // else exception already logged
            response = "OK";    // not used
        } else if (rootNodeName.equals("_mdw_refresh")) {
            String cachename = XmlPath.getRootNodeValue(msgdoc);
            CacheRegistration.broadcastRefresh(cachename, MessengerFactory.newInternalMessenger());
            response = "OK";
        } else if (rootNodeName.equals("_mdw_document_content")) {
            String documentId = XmlPath.evaluate(msgdoc, "/_mdw_document_content/document_id");
            String type = XmlPath.evaluate(msgdoc, "/_mdw_document_content/type");
            try {
                Document doc = ServiceLocator.getWorkflowServices().getDocument(new Long(documentId));
                if (type.equals(Object.class.getName())) {
                    Object obj = VariableTranslator.realToObject(getPackage(doc), "java.lang.Object", doc.getContent(getPackage(doc)));
                    response = obj.toString();
                } else response = doc.getContent(getPackage(doc));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                response = "ERROR: " + e.getClass().getName() + " - " + e.getMessage();
            }
        } else if (rootNodeName.equals("_mdw_task_sla")) {
            try {
                TaskServices taskServices = ServiceLocator.getTaskServices();
                String taskInstId = XmlPath.evaluate(msgdoc, "/_mdw_task_sla/task_instance_id");
                String isAlert = XmlPath.evaluate(msgdoc, "/_mdw_task_sla/is_alert");
                taskServices.updateTaskInstanceState(Long.valueOf(taskInstId), Boolean.valueOf(isAlert));
                response = "OK";
            } catch (Exception e) {
                logger.error("Failed to change task state", e);
                response = "ERROR: " + e.getMessage();
            }
        } else {
            response = "ERROR: unknown internal message " + rootNodeName;
        }
        return response;
    }

    private Package getPackage(Document doc) throws ServiceException {
        try {
            EventServices eventMgr = ServiceLocator.getEventServices();
            Long procInstId = null;
            if (doc.getOwnerType().equals(OwnerType.VARIABLE_INSTANCE)) {
                VariableInstance varInstInf = eventMgr.getVariableInstance(doc.getOwnerId());
                procInstId = varInstInf.getProcessInstanceId();
            }
            else if (doc.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                procInstId = doc.getOwnerId();
            }
            if (procInstId == null)
                return null;
            ProcessInstance procInst = eventMgr.getProcessInstance(procInstId);
            return PackageCache.getPackage(procInst.getPackageName());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
