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

import com.centurylink.mdw.designer.display.TextNote;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

public class Note extends WorkflowElement {
    private TextNote textNote;

    public TextNote getTextNote() {
        return textNote;
    }

    public void setTextNote(TextNote textNote) {
        this.textNote = textNote;
    }

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public Note(TextNote textNote, WorkflowProcess processVersion) {
        this.textNote = textNote;
        this.process = processVersion;
        setProject(processVersion.getProject());
    }

    public String getTitle() {
        return "Note";
    }

    public Long getId() {
        return textNote.getId();
    }

    public String getName() {
        return textNote.getName();
    }

    public void setName(String name) {
        textNote.setName(name);
    }

    @Override
    public String getFullPathLabel() {
        return getPath() + (getProcess() == null ? "Note " : getProcess().getName() + "/Note ")
                + getLabel();
    }

    public String getText() {
        return textNote.vo.getContent();
    }

    public void setText(String text) {
        textNote.setText(text);
    }

    public void adjustSize() {
        textNote.adjustSize();
    }

    public String getIcon() {
        return "doc.gif";
    }

    public boolean isReadOnly() {
        return process.isReadOnly();
    }

    public boolean hasInstanceInfo() {
        return false;
    }

    public Entity getActionEntity() {
        return Entity.Note;
    }
}
