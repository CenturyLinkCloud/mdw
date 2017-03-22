/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.meta;

import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class EventHandler extends Code {
    private String inputText;

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }


    private String process;

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    private String event;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    private boolean createDocument;

    public boolean isCreateDocument() {
        return createDocument;
    }

    public void setCreateDocument(boolean createDocument) {
        this.createDocument = createDocument;
    }

    private String documentVariable;

    public String getDocumentVariable() {
        return documentVariable;
    }

    public void setDocumentVariable(String documentVariable) {
        this.documentVariable = documentVariable;
    }

    private boolean custom;

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    // only used by the custom handler templates
    private boolean launchSynchronous;

    public boolean isLaunchSynchronous() {
        return launchSynchronous;
    }

    public void setLaunchSynchronous(boolean launchSynchronous) {
        this.launchSynchronous = launchSynchronous;
    }

    public ExternalEvent createExternalEvent(String handlerString) {
        ExternalEventVO externalEventVO = new ExternalEventVO();
        externalEventVO.setEventName(inputText);
        externalEventVO.setEventHandler(handlerString);
        WorkflowPackage workflowPackage = getPackage() == null ? getProject().getDefaultPackage()
                : getPackage();
        return new ExternalEvent(externalEventVO, workflowPackage);
    }
}
