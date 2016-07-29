/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.task;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.VariableConstants;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.qwest.mbeng.MbengException;

public class SendResponseToTask extends DefaultActivityImpl {

	protected static final String FORCE_SEND_IN_SERVICE = "Force send in Service Process";

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
    		FormDataDocument datadoc = this.createFormData();
    		// above needs to be called before checking for services, as
    		// with service processes the data doc may still need to be created
    		// by overriding subclasses
    		if (getEngine().isInService()) {
    			if (!"true".equalsIgnoreCase(this.getAttributeValue(FORCE_SEND_IN_SERVICE))) return;
    		}
    		datadoc.setAttribute(FormDataDocument.META_TASK_INSTANCE_ID,
    				getProcessInstanceOwnerId().toString());
    		datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_RESPOND_TASK);
			datadoc.setAttribute(FormDataDocument.ATTR_ENGINE_CALL_STATUS, "DONE");
			if (getProcessInstance().getSecondaryOwner().equals(OwnerType.PROCESS_INSTANCE) &&
					"true".equalsIgnoreCase(datadoc.getMetaValue(FormDataDocument.META_AUTOBIND))) {
				Long bindingProcInstId = getProcessInstance().getSecondaryOwnerId();
				autobindOutput(this.getProcessDefinition(), bindingProcInstId);
			}
			TaskManagerAccess.getInstance().sendMessage(datadoc);
    	} catch (Exception e) {
    		throw new ActivityException(-1, e.getMessage(), e);
    	}
    }

	protected FormDataDocument createFormData()
			throws ActivityException, MbengException {
    	return getFormDataDocumentFromVariable();
	}

    protected final FormDataDocument getFormDataDocumentFromVariable()
    		throws ActivityException, MbengException
    		{
		String formDataVar = this.getAttributeValue(TaskActivity.ATTRIBUTE_FORM_DATA_VAR);
		if (formDataVar==null) throw new ActivityException("Form data variable not defined");
		Object datadocref = this.getParameterValue(formDataVar);
		FormDataDocument datadoc = null;
		if (datadocref == null) {
		    datadoc = new FormDataDocument();  // starting with blank
		    datadoc.setMetaValue(FormDataDocument.META_FORM_DATA_VARIABLE_NAME, formDataVar);
		    DocumentReference docRef = createDocument(FormDataDocument.class.getName(), datadoc, OwnerType.PROCESS_INSTANCE, getProcessInstanceId(), null, null);
		    setParameterValue(formDataVar, docRef);
		}
		else {
		    if (!(datadocref instanceof DocumentReference))
		        throw new ActivityException("Form data variable not bound to document");
		    String datadocContent = getDocumentContent((DocumentReference)datadocref);
		    if (datadocContent==null) throw new ActivityException("Form data document not exist");
		    datadoc = new FormDataDocument();
		    datadoc.load(datadocContent);
		}
		return datadoc;
	}

    private void autobindOutput(ProcessVO procdef, Long parentProcInstId)
			throws ActivityException {
		for (VariableVO childVar : procdef.getVariables()) {
			int varCat = childVar.getVariableCategory().intValue();
			if (varCat!=VariableVO.CAT_OUTPUT && varCat!=VariableVO.CAT_INOUT) continue;
		 	String vn = childVar.getVariableName();
		 	if (vn.equals(VariableConstants.RESPONSE)) continue;
         	if (vn.equals(VariableConstants.MASTER_DOCUMENT)) continue;
		 	String value = this.getParameterStringValue(vn);
		 	if (value==null) continue;
		 	super.setParameterValue(parentProcInstId, vn, value);
		}
    }

}
