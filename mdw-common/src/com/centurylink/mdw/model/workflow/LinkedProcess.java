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
package com.centurylink.mdw.model.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workflow process in a hierarchical structure of called/calling processes.
 * TODO: Refactor ProcessHierarchyContentProvider in the Designer UI project to use this model object.
 */
public class LinkedProcess {

    private Process process;
    public Process getProcess() { return process; }

    private LinkedProcess parent;
    public LinkedProcess getParent() { return parent; }
    public void setParent(LinkedProcess parent) { this.parent = parent; }

    private List<LinkedProcess> children = new ArrayList<LinkedProcess>();
    public List<LinkedProcess> getChildren() { return children; }
    public void setChildren(List<LinkedProcess> children) { this.children = children; }

    public LinkedProcess(Process process) {
        this.process = process;
    }
}
