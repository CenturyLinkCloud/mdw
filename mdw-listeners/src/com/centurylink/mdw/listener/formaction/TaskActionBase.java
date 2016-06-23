/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.Map;

import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.services.task.TaskManagerAccess;

public class TaskActionBase extends FormActionBase {

	protected String findTaskManagerUrl(String applName) throws Exception {
		return TaskManagerAccess.getInstance().findTaskManagerUrl(applName);
	}

    /**
     * This method is used to call a SubProcess synchronously from a General Manual Task
     * Here is one example to use from  XHTL page
     * <t:subform id="callSubProcForm">
        <t:inputHidden id="hiddenAction" value="com.centurylink.mdw.listener.formaction.TaskActionBase?subaction=call_engine&amp;PROCESS_NAME=SyncProcessForTask" />
        <t:commandButton value="Call Subprocess" actionFor="callSubProcForm" onclick="{ return true; }" />
       </t:subform>
     * @param datadoc Form Data document for input to the sub process.
     * @param metainfo : Task Action attributes
     * @return FormDataDocument Updated Form Data document returned from the Subprocess
     */
    public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> metainfo) {
        String action = metainfo.get("subaction");
        if (action.equals("call_engine")) {
            try {
                String procname = metainfo.get(FormDataDocument.META_PROCESS_NAME);
                metainfo.put(FormDataDocument.META_PROCESS_NAME, procname);
                datadoc = super.startProcess(datadoc, datadoc.format(), metainfo);
            }
            catch (Exception e) {
                datadoc.addError("Failed to execute the action: " + e.getMessage());
            }
        }
        return datadoc;
    }
}
