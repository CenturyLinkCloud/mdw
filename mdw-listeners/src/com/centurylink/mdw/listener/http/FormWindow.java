/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;

@Deprecated
public class FormWindow {

	private TaskInstanceVO taskInstance;
	private FormDataDocument datadoc;	// only used by JSF session, not by HTML session
	private boolean toDelete;

	public FormWindow(TaskInstanceVO taskInst, FormDataDocument datadoc) {
		this.taskInstance = taskInst;
		this.datadoc = datadoc;
		toDelete = false;
	}

	public TaskInstanceVO getTaskInstance() {
		return taskInstance;
	}
	public void setTaskInstance(TaskInstanceVO taskInstance) {
		this.taskInstance = taskInstance;
	}

	// only used by JSF session, not by HTML session
	public FormDataDocument getData() {
		return datadoc;
	}
	// only used by JSF session, not by HTML session
	public void setData(FormDataDocument datadoc) {
		this.datadoc = datadoc;
	}

	public boolean isToDelete() {
		return toDelete;
	}
	public void setToDelete(boolean toDelete) {
		this.toDelete = toDelete;
	}

	public Long getId() {
		return taskInstance.getTaskInstanceId();
	}

	// only used by JSF session, not by HTML session
	public void clearErrors() {
		if (datadoc!=null) {
			datadoc.clearErrors();
	    	datadoc.setMetaValue(FormDataDocument.META_PROMPT, null);
	    	datadoc.setMetaValue(FormDataDocument.META_INITIALIZATION, null);
		}
	}

	// only used by JSF session, not by HTML session
	public void addError(String errmsg) {
		if (datadoc!=null) datadoc.addError(errmsg);
	}

}
