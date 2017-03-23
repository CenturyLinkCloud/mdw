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
package com.centurylink.mdw.plugin.designer.model;

public class ElementChangeEvent {
    public enum ChangeType {
        ELEMENT_CREATE, ELEMENT_DELETE, RENAME, VERSION_CHANGE, LABEL_CHANGE, STATUS_CHANGE, SETTINGS_CHANGE, PROPERTIES_CHANGE
    }

    private ChangeType changeType;

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    public void setElement(WorkflowElement element) {
        this.element = element;
    }

    private Object newValue;

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public ElementChangeEvent(ChangeType changeType, WorkflowElement element) {
        this.changeType = changeType;
        this.element = element;
    }
}
