/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.task;

import java.util.List;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.qwest.mbeng.MbengException;

@Tracked(LogLevel.TRACE)
public class AutoFormManualTaskActivity extends FormDataDocumentManualTaskActivityBase {

    /**
     *
     * The method creates a new task instance, or in case a task instance ID
     * is already known (the method getTaskInstanceId() return non-null),
     * the method sends a response back to the task instance.
     * The method invokes createFormData() to get the form data document,
     * and by default it takes the data from the document variable specified in the designer.
     *
     */
    @Override
    public void execute() throws ActivityException {
        try {
            FormDataDocument formdata = this.createFormData();
            populateFormDataMetaInfo(formdata, false, false);

            Long taskInstanceId;
            if ("true".equalsIgnoreCase(PropertyManager.getProperty("mdw.create.autoform.task.thru.event"))) { // non-standard prop
                // create task through old event mechanism
                taskInstanceId = TaskManagerAccess.getInstance().createGeneralTaskInstance(formdata);
            }
            else {
                String taskLogicalId = getAttributeValue(ATTRIBUTE_TASK_LOGICAL_ID); // old task activity attribute
                String taskTemplate = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE);
                if (taskTemplate != null) {
                    // new-style task templates
                    String templateVersion = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE_VERSION);
                    AssetVersionSpec spec = new AssetVersionSpec(taskTemplate, templateVersion == null ? "0" : templateVersion);
                    TaskVO template = TaskTemplateCache.getTaskTemplate(spec);
                    if (template == null)
                        throw new ActivityException("Task template not found: " + spec);
                    taskLogicalId = template.getLogicalId();
                }
                TaskServices taskServices = ServiceLocator.getTaskServices();
                taskInstanceId = taskServices.createAutoFormTaskInstance(taskLogicalId,
                        getMasterRequestId(), getProcessInstanceId(), getActivityInstanceId(), formdata).getTaskInstanceId();
            }

            String taskInstCorrelationId = FormConstants.TASK_CORRELATION_ID_PREFIX + taskInstanceId.toString();
            formdata.setAttribute(FormDataDocument.ATTR_ID, taskInstCorrelationId);
            super.loginfo("Task instance created - ID " + taskInstanceId);
            if (this.needSuspend()) {
                getEngine().createEventWaitInstance(
                        this.getActivityInstanceId(),
                        getEventName(formdata),
                        EventType.EVENTNAME_FINISH, true, true);
                EventWaitInstanceVO received = registerWaitEvents(false,true);
                if (received!=null)
                        resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                                         received.getCompletionCode());
            }
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    /**
     * TODO: Git rid of FormDataDocument.
     */
    @Override
    protected void populateFormDataMetaInfo(FormDataDocument datadoc, boolean subsequentCall, boolean updateActivityInstanceId)
    throws ActivityException {
        try {
            String taskTemplateAttr = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE);
            if (taskTemplateAttr == null) {
                // old-style (non-template)
                super.populateFormDataMetaInfo(datadoc, subsequentCall, updateActivityInstanceId);
                return;
            }
            // else subsequent call no need to set form name
            if (subsequentCall) {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_RESPOND_TASK);
                if (updateActivityInstanceId || datadoc.getMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID)==null)
                    datadoc.setMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID, getActivityInstanceId().toString());
            }
            else {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CREATE_TASK);
                datadoc.setMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID, getActivityInstanceId().toString());
            }
            datadoc.setAttribute(FormDataDocument.ATTR_NAME, getActivityId().toString());
            datadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, getProcessInstanceId().toString());
            if (!subsequentCall) {
                // use Task Template for attributes
                String templateVersion = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE_VERSION);
                AssetVersionSpec spec = new AssetVersionSpec(taskTemplateAttr, templateVersion == null ? "0" : templateVersion);
                TaskVO template = TaskTemplateCache.getTaskTemplate(spec);
                if (template == null)
                    throw new ActivityException("Task template not found: " + spec);
                String taskLogicalId = template.getLogicalId();
                datadoc.setMetaValue(FormDataDocument.META_TASK_LOGICAL_ID, taskLogicalId);

                String sla = template.getAttribute(TaskAttributeConstant.TASK_SLA);
                if (sla != null) // always in seconds for templates
                  datadoc.setMetaValue(FormDataDocument.META_DUE_IN_SECONDS, Integer.toString(Integer.parseInt(sla)));
                datadoc.setMetaValue(FormDataDocument.META_TASK_NAME, template.getAttribute(template.getName()));
                datadoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, getMasterRequestId());
                fillInCustomActions(datadoc);
            }
        }
        catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    /**
     * This method is used to create form data document to be sent to task manager.
     * The methods populates variable instance data and custom actions in the data document
     * @throws
     */
    protected FormDataDocument createFormData() throws ActivityException {
        FormDataDocument datadoc = new FormDataDocument();
        String formname = this.getAttributeValue(TaskActivity.ATTRIBUTE_FORM_NAME);
        if (formname!=null && !formname.startsWith("html:")) {
        	// use form to selectively include process variables
        	fillInCustomActions(datadoc);
        	fillInVariables(datadoc);
        } else {
        	// include all instantiated process variables
        	for (VariableInstanceInfo var : this.getParameters()) {
        		String value;
        		if (var.isDocument()) {
        			value = getDocumentContent((DocumentReference)var.getData());
        		} else value = var.getStringValue();
        		try {
        			datadoc.setValue(var.getName(), value);
        		} catch (MbengException e) {
        			super.logwarn("Failed to set variable in form data document: " + var.getName());
        		}
        	}
        }
        return datadoc;
    }

    /**
     * This method is used to extract data from the data document received from the task manager.
     * The method updates all variables specified as non-readonly
     *
     * @param datadoc
     * @param formnode
     * @return completion code; when it returns null, the completion
     *   code is taken from the completionCode parameter of
     *   the form data document attribute FormDataDocument.ATTR_ACTION
     * @throws ActivityException
     */
    protected String extractFormData(FormDataDocument datadoc)
            throws ActivityException {
    	String varstring = this.getAttributeValue(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
		List<String[]> parsed = StringHelper.parseTable(varstring, ',', ';', 5);
		for (String[] one : parsed) {
			String varname = one[0];
			String displayOption = one[2];
			if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
			if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_READONLY)) continue;
			if (varname.startsWith("#{") || varname.startsWith("${")) continue;
			String data = datadoc.getValue(varname);
			setDataToVariable(varname, data);
		}
        return null;
    }

    protected void fillInVariables(FormDataDocument formdatadoc)
	throws ActivityException {
    	try {
			String varstring = this.getAttributeValue(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
			List<String[]> parsed = StringHelper.parseTable(varstring, ',', ';', 5);
			for (String[] one : parsed) {
				String varname = one[0];
				// TODO: support expressions in displaying autoform manual tasks
				// (currently only supported for Task services
				if (!varname.startsWith("#{") && !varname.startsWith("${")) {
    				String displayOption = one[2];
    				if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
    				String data;
    				VariableInstanceInfo varinst = this.getVariableInstance(varname);
    				if (varinst!=null) {
    					if (varinst.isDocument()) {
    						DocumentReference docref = (DocumentReference)varinst.getData();
    						data = super.getDocumentContent(docref);
    					} else data = varinst.getStringValue();
    					formdatadoc.setValue(varname, data);
    				}
				}
			}
		} catch (MbengException e) {
			throw new ActivityException(-1, "Failed to fill in variable data", e);
		}
    }
}
