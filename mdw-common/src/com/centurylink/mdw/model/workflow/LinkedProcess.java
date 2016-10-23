/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
