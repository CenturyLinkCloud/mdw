/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.task;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

@Tracked(LogLevel.TRACE)
public class FormDataDocumentManualTaskActivity extends FormDataDocumentManualTaskActivityBase {

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
    		String v = this.getAttributeValue("Can respond task");
    		String taskInstCorrelationId = "true".equalsIgnoreCase(v)?
    				formdata.getAttribute(FormDataDocument.ATTR_ID):null;
    		populateFormDataMetaInfo(formdata, false, false);
    		Long taskInstanceId = TaskManagerAccess.getInstance().createGeneralTaskInstance(formdata);
    		taskInstCorrelationId = FormConstants.TASK_CORRELATION_ID_PREFIX + taskInstanceId.toString();
    		formdata.setAttribute(FormDataDocument.ATTR_ID, taskInstCorrelationId);
    		this.updateFormDataDocumentToVariable(formdata);
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
     * This method is used to create form data document to be sent to task manager.
     * There are typically three ways to create the data document:
     *    1. auto generate from process variables. This is the classic way.
     *    2. override this method and create the data in Java
     *    3. create the data in a previous activity and specify the doc variable.
     * There is also a 4th way that is between 1 and 2:
     *    4. use same code as 1. but override getDataFromVariable(String datapath) method,
     *       which is used to get variable value for the datapath specified in the form.
     *       The default method treat datapath as the variable name and the get the value
     *       of the variable. You can override the method to fetch the data in some other means.
     * The default method takes the 3rd way, but it also contains sample code (code that
     * never will hit) for the other 2 ways.
     */
    protected FormDataDocument createFormData() throws ActivityException {
        int whichWay = 3;
        FormDataDocument datadoc;
        if (whichWay==1) {      // auto generate from process variables
            datadoc = new FormDataDocument();
            String formname = this.getAttributeValue(TaskActivity.ATTRIBUTE_FORM_NAME);
            if (formname!=null && !formname.startsWith("html:")) {
            	// use form to selectively include process variables
            	fillInCustomActions(datadoc);
            	fillInDataFromProcessVariables(datadoc);
            } else {
            	// include all instantiated process variables
            	for (VariableInstanceInfo var : this.getParameters()) {
            		String value;
            		if (var.isDocument()) {
            			value = VariableTranslator.realToString(var.getType(), var.getData());
            		} else value = var.getStringValue();
            		try {
						datadoc.setValue(var.getName(), value);
					} catch (MbengException e) {
						super.logwarn("Failed to set variable in form data document: " + var.getName());
					}
            	}
            }
        } else if (whichWay==2) {       // generate through API
            // the following code is for sample only
            try {
                datadoc = new FormDataDocument();
                // set simple variable
                datadoc.setValue("var1", "How are you doing?");
                // set choice list for dropdown or radio buttons
                MbengNode choices = datadoc.setValue("choices", null, FormDataDocument.KIND_LIST);
                datadoc.setValue(choices, "One", "101");
                datadoc.setValue(choices, "Two", "102");
                // set table contents
                MbengNode allPorts = datadoc.setValue("allPorts", null, FormDataDocument.KIND_TABLE);
                MbengNode row;
                row = datadoc.setValue(allPorts, "One", null, FormDataDocument.KIND_ROW);
                datadoc.setValue(row, "name", "101", FormDataDocument.KIND_ENTRY);
                datadoc.setValue(row, "type", "Electric", FormDataDocument.KIND_ENTRY);
                datadoc.setValue(row, "status", "Available", FormDataDocument.KIND_ENTRY);
                row = datadoc.setValue(allPorts, "Two", null, FormDataDocument.KIND_ROW);
                datadoc.setValue(row, "name", "102", FormDataDocument.KIND_ENTRY);
                datadoc.setValue(row, "type", "Optical", FormDataDocument.KIND_ENTRY);
                datadoc.setValue(row, "status", "Incompatible", FormDataDocument.KIND_ENTRY);
            } catch (MbengException e) {
                throw new ActivityException(-1, e.getMessage(), e);
            }
        } else {    // 3rd and default way - get document from variable
            try {
                datadoc = super.getFormDataDocumentFromVariable();
            } catch (Exception e) {
                throw new ActivityException(-1, "Failed to load form data from variable", e);
            }
        }
        return datadoc;
    }

    /**
     * This method is used to extract data from the data document received from the task manager.
     * Like createFormData, there are typically three ways to extract the data document:
     *    1. auto populate the process variables from the data document. This is the classic way.
     *    2. override this method and populate the variables from the data in Java
     *    3. put the response data in the document variable specified in the designer.
     * Also like createFormData, there is also a 4th way that is between 1 and 2:
     *    4. use same code as 1. but override setDataToVariable(String datapath, String value) method,
     *       which is used to set the variable value from the datapath specified in the form.
     *       The default method treat datapath as the variable name and the set the value
     *       of the variable. You can override the method to set the data somewhere else.
     * The default method takes the 3rd way, but it also contains sample code (code that
     * never will hit) for the other 2 ways.
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
        int whichWay = 3;
        if (whichWay==1) {      // auto populate process variables
            DomDocument formdoc = loadForm();
            extractDataToProcessVariables(datadoc, formdoc.getRootNode());
        } else if (whichWay==2) {       // populate data through API
            // the following code is for sample only
            String v = datadoc.getValue("var1");
            super.setParameterValue("var1", v);
        } else {    // 3rd and default way - set the document variable specified in the designer
            String formAttr = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
            if (formAttr != null && formAttr.endsWith(".xhtml"))
                extractDataToProcessVariables(datadoc);
            if (!updateFormDataDocumentToVariable(datadoc))
                throw new ActivityException("No document variable is specified");
        }
        // one thing you may need is to get the task instance ID for the next page
        // in case of multiple page flows. The following call can be used.
        //      Long taskInstId = this.getTaskInstanceId(datadoc);
        return null;
    }

    protected final boolean updateFormDataDocumentToVariable(FormDataDocument datadoc)
        throws ActivityException
    {
        String formDataVar = this.getAttributeValue(TaskActivity.ATTRIBUTE_FORM_DATA_VAR);
        if (formDataVar==null) return true;
        VariableInstanceInfo var = getVariableInstance(formDataVar);
        if (var==null) return false;
        super.setParameterValueAsDocument(formDataVar, var.getType(), datadoc.format());
        return true;
    }

}
