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

import java.util.List;

import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

/**
 * Wraps a Designer workflow SubGraph.
 */
public class EmbeddedSubProcess extends WorkflowElement {
    private SubGraph subGraph;

    public SubGraph getSubGraph() {
        return subGraph;
    }

    // parent process
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private List<ProcessInstanceVO> subProcessInstances;

    public List<ProcessInstanceVO> getSubProcessInstances() {
        return subProcessInstances;
    }

    public void setSubProcessInstances(List<ProcessInstanceVO> subProcessInstances) {
        this.subProcessInstances = subProcessInstances;
    }

    public Entity getActionEntity() {
        return Entity.Process;
    }

    public EmbeddedSubProcess(SubGraph subGraph, WorkflowProcess processVersion) {
        this.process = processVersion;
        this.subGraph = subGraph;
    }

    public EmbeddedSubProcess(ProcessVO subprocessVO, WorkflowProcess processVersion) {
        this.process = processVersion;
        DesignerProxy designerProxy = processVersion.getProject().getDesignerProxy();
        this.subGraph = new SubGraph(subprocessVO, null, designerProxy.getNodeMetaInfo(),
                designerProxy.getIconFactory());
    }

    public ProcessVO getSubProcessVO() {
        if (subGraph == null)
            return null;
        return subGraph.getProcessVO();
    }

    public WorkflowProject getProject() {
        return process.getProject();
    }

    @Override
    public String getTitle() {
        return "Embedded Subprocess";
    }

    @Override
    public Long getId() {
        return subGraph.getId();
    }

    public int getSequenceId() {
        return subGraph.getSequenceId();
    }

    public boolean isReadOnly() {
        return process.isReadOnly();
    }

    public boolean hasInstanceInfo() {
        return subProcessInstances != null;
    }

    /**
     * true if the activity is displayed within a process (still might not have
     * instance info if flow has not reached here yet)
     */
    public boolean isForProcessInstance() {
        return process != null && process.hasInstanceInfo();
    }

    @Override
    public String getIcon() {
        return "element.gif";
    }

    @Override
    public String getPath() {
        String path = getProjectPrefix();
        if (getProject() != null && getProcess() != null && !getProcess().isInDefaultPackage())
            path += getProcess().getPackage().getName() + "/";
        return path;
    }

    @Override
    public String getFullPathLabel() {
        return getPath() + (getProcess() == null ? "" : getProcess().getName() + "/") + getLabel();
    }

    public String getName() {
        return subGraph.getName();
    }

    public void setName(String name) {
        subGraph.setName(name);
    }

    public String getDescription() {
        return subGraph.getDescription();
    }

    public void setDescription(String description) {
        subGraph.setDescription(description);
    }

    public List<AttributeVO> getAttributes() {
        return subGraph.getAttributes();
    }

    public String getAttribute(String name) {
        return subGraph.getAttribute(name);
    }

    public void setAttribute(String name, String value) {
        subGraph.setAttribute(name, value);
    }

    @Override
    public boolean overrideAttributesApplied() {
        return getProcess().overrideAttributesApplied();
    }

    @Override
    public boolean isOverrideAttributeDirty(String prefix) {
        return getProcess().isAttributeOwnerDirty(prefix, OwnerType.PROCESS, getId());
    }

    @Override
    public void setOverrideAttributeDirty(String prefix, boolean dirty) {
        getProcess().setAttributeOwnerDirty(prefix, OwnerType.PROCESS, getId(), dirty);
    }
}
