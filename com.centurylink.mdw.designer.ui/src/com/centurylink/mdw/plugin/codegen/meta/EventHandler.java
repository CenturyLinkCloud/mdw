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
package com.centurylink.mdw.plugin.codegen.meta;

import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class EventHandler extends Code {
    private String messagePattern;

    public String getMessagePattern() {
        return messagePattern;
    }

    public void setMessagePattern(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    private String eventName;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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
        if (eventName != null && eventName.length() > 0)
            externalEventVO.setEventName(eventName);
        else
            externalEventVO.setEventName(messagePattern);
        externalEventVO.setMessagePattern(messagePattern);
        externalEventVO.setEventHandler(handlerString);
        WorkflowPackage workflowPackage = getPackage() == null ? getProject().getDefaultPackage()
                : getPackage();
        return new ExternalEvent(externalEventVO, workflowPackage);
    }
}
