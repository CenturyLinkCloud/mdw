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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

/**
 * Wraps a ProcessVO model object.
 */
public class WorkflowProcess extends WorkflowElement
        implements Versionable, AttributeHolder, IEditorInput, Comparable<WorkflowProcess> {
    public static String getVersionString(int version) {
        return version / 1000 + "." + version % 1000;
    }

    private WorkflowPackage processPackage;

    public WorkflowPackage getPackage() {
        if (processPackage == null)
            return getProject().getDefaultPackage();
        return processPackage;
    }

    public void setPackage(WorkflowPackage pv) {
        this.processPackage = pv;
        if (processVO != null && pv != null)
            processVO.setPackageName(pv.getName());
    }

    public boolean isInDefaultPackage() {
        return processPackage == null || processPackage.isDefaultPackage();
    }

    public Entity getActionEntity() {
        if (hasInstanceInfo())
            return Entity.ProcessInstance;
        else
            return Entity.Process;
    }

    private ProcessVO processVO;

    public ProcessVO getProcessVO() {
        return processVO;
    }

    public void setProcessVO(ProcessVO processVO) {
        this.processVO = processVO;
    }

    private ProcessInstanceVO processInstanceInfo;

    public ProcessInstanceVO getProcessInstance() {
        return processInstanceInfo;
    }

    public void setProcessInstance(ProcessInstanceVO info) {
        this.processInstanceInfo = info;
    }

    public boolean hasInstanceInfo() {
        return processInstanceInfo != null;
    }

    private List<ProcessInstanceVO> embeddedSubProcessInstances;

    public List<ProcessInstanceVO> getEmbeddedSubProcessInstances() {
        return embeddedSubProcessInstances;
    }

    public void setEmbeddedSubProcessInstances(List<ProcessInstanceVO> embeddedInstances) {
        this.embeddedSubProcessInstances = embeddedInstances;
    }

    private Map<Long, List<TaskInstanceVO>> taskInstances;

    public Map<Long, List<TaskInstanceVO>> getTaskInstance() {
        return taskInstances;
    }

    public void setTaskInstances(Map<Long, List<TaskInstanceVO>> taskInstances) {
        this.taskInstances = taskInstances;
    }

    public List<TaskInstanceVO> getTaskInstances(Long activityId) {
        if (taskInstances == null)
            return null;
        return taskInstances.get(activityId);
    }

    public List<TaskInstanceVO> getMainTaskInstances(Long activityId) {
        List<TaskInstanceVO> mainTaskInsts = new ArrayList<TaskInstanceVO>();
        List<TaskInstanceVO> allTaskInsts = getTaskInstances(activityId);
        if (allTaskInsts != null) {
            for (TaskInstanceVO taskInstance : allTaskInsts) {
                if (!taskInstance.isSubTask())
                    mainTaskInsts.add(taskInstance);
            }
        }
        return mainTaskInsts;
    }

    public List<TaskInstanceVO> getSubTaskInstances(Long activityId) {
        List<TaskInstanceVO> subTaskInsts = new ArrayList<TaskInstanceVO>();
        List<TaskInstanceVO> allTaskInsts = getTaskInstances(activityId);
        if (allTaskInsts != null) {
            for (TaskInstanceVO taskInstance : allTaskInsts) {
                if (taskInstance.isSubTask())
                    subTaskInsts.add(taskInstance);
            }
        }
        return subTaskInsts;
    }

    private List<WorkflowProcess> descendantProcessVersions;

    public List<WorkflowProcess> getDescendantProcessVersions() {
        return descendantProcessVersions;
    }

    public void setDescendantProcessVersions(List<WorkflowProcess> dpvs) {
        descendantProcessVersions = dpvs;
    }

    public boolean hasDescendantProcessVersions() {
        return descendantProcessVersions != null && descendantProcessVersions.size() > 0;
    }

    public void addDescendantProcessVersion(WorkflowProcess processVersion) {
        if (descendantProcessVersions == null)
            descendantProcessVersions = new ArrayList<WorkflowProcess>();
        descendantProcessVersions.add(processVersion);
        Collections.sort(descendantProcessVersions);
    }

    public void removeDescendantProcessVersion(WorkflowProcess processVersion) {
        descendantProcessVersions.remove(processVersion);
        Collections.sort(descendantProcessVersions);
    }

    private boolean readOnly = true;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (descendantProcessVersions != null) {
            for (WorkflowProcess descendant : descendantProcessVersions) {
                if (descendant.getVersion() == getVersion())
                    descendant.readOnly = readOnly;
            }
        }
        if (getTopLevelVersion() != null && getTopLevelVersion().getVersion() == getVersion())
            getTopLevelVersion().readOnly = readOnly;
    }

    public WorkflowProcess(WorkflowProject workflowProject, ProcessVO processVO) {
        setProject(workflowProject);
        this.processVO = processVO;
        sync();
    }

    public WorkflowProcess(WorkflowProcess cloneFrom) {
        this(cloneFrom.getProject(), cloneFrom.getProcessVO());
        this.processPackage = cloneFrom.getPackage();
    }

    public WorkflowProcess() {
    }

    // top-level versions are those that are related to a package
    // as well as the latest version for an unpackaged process version
    private WorkflowProcess topLevelVersion;

    public WorkflowProcess getTopLevelVersion() {
        return topLevelVersion;
    }

    public void setTopLevelVersion(WorkflowProcess topLevelVersion) {
        this.topLevelVersion = topLevelVersion;
    }

    public boolean isTopLevel() {
        // only top-level versions don't have a topLevelVersion
        return topLevelVersion == null;
    }

    public boolean isArchived() {
        return getPackage() == null || getPackage().isArchived();
    }

    @Override
    public String getTitle() {
        return "Process";
    }

    @Override
    public Long getId() {
        if (processVO == null || processVO.getProcessId() == null)
            return null;

        return new Long(processVO.getProcessId());
    }

    public String getIdLabel() {
        if (getProject().getPersistType() == PersistType.Git)
            return getId() + " (" + getHexId() + ")";
        else
            return String.valueOf(getId());
    }

    public String getVersionString() {
        return processVO.getVersionString();
    }

    public int getVersion() {
        return processVO.getVersion();
    }

    public void setVersion(int version) {
        processVO.setVersion(version);
    }

    public int parseVersion(String versionString) throws NumberFormatException {
        return ProcessVO.parseVersion(versionString);
    }

    public String formatVersion(int version) {
        return ProcessVO.formatVersion(version);
    }

    public List<AttributeVO> getAttributes() {
        return processVO.getAttributes();
    }

    private String name;

    public String getName() {
        if (processVO == null) // skeleton mode
            return name;
        else
            return processVO.getProcessName();
    }

    public void setName(String name) {
        if (processVO == null)
            this.name = name;
        else
            processVO.setProcessName(name);
    }

    private String description;

    public String getDescription() {
        if (processVO == null)
            return description;
        else
            return processVO.getProcessDescription();
    }

    public void setDescription(String description) {
        if (processVO == null)
            this.description = description;
        else
            processVO.setProcessDescription(description);
    }

    boolean isInRuleSet;

    public boolean isInRuleSet() {
        if (processVO == null)
            return isInRuleSet;
        else
            return processVO.isInRuleSet();
    }

    public void setInRuleSet(boolean inRuleSet) {
        if (processVO == null)
            this.isInRuleSet = inRuleSet;
        else
            processVO.setInRuleSet(inRuleSet);
    }

    public WorkflowProcess getNextVersion() {
        if (processVO.getNextVersion() == null)
            return null;

        WorkflowProcess pv = new WorkflowProcess(getProject(), processVO.getNextVersion());
        pv.setPackage(getProject().findPackage(pv));
        return pv;
    }

    public WorkflowProcess getPreviousVersion() {
        if (processVO.getPrevVersion() == null)
            return null;

        WorkflowProcess pv = new WorkflowProcess(getProject(), processVO.getPrevVersion());
        pv.setPackage(getProject().findPackage(pv));
        return pv;
    }

    public List<WorkflowProcess> getAllProcessVersions() {
        List<WorkflowProcess> allVersions = new ArrayList<WorkflowProcess>();
        for (WorkflowProcess pv = this; pv != null; pv = pv.getPreviousVersion())
            allVersions.add(pv);
        for (WorkflowProcess pv = this.getNextVersion(); pv != null; pv = pv.getNextVersion())
            allVersions.add(pv);

        Collections.sort(allVersions);
        Collections.reverse(allVersions);
        return allVersions;
    }

    public List<ProcessVO> getEmbeddedSubProcesses() {
        if (processVO == null)
            return null;
        return processVO.getSubProcesses();
    }

    public ProcessVO getEmbeddedSubProcess(Long processId) {
        if (getEmbeddedSubProcesses() == null)
            return null;
        for (ProcessVO embeddedSubProc : getEmbeddedSubProcesses()) {
            if (embeddedSubProc.getProcessId().equals(processId))
                return embeddedSubProc;
        }
        return null;
    }

    public boolean isSynchronous() {
        return "SERVICE".equals(processVO.getAttribute("PROCESS_VISIBILITY"));
    }

    public void setSynchronous(boolean synchronous) {
        processVO.setAttribute("PROCESS_VISIBILITY", synchronous ? "SERVICE" : "PUBLIC");
    }

    public String getStartPage() {
        return processVO.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE);
    }

    public void setStartPage(String page) {
        processVO.setAttribute(WorkAttributeConstant.PROCESS_START_PAGE, page);
    }

    public String getCreateUser() {
        return processVO.getCreateUser();
    }

    public void setCreateUser(String user) {
        processVO.setCreateUser(user);
    }

    public Date getCreateDate() {
        return processVO.getCreateDate();
    }

    public String getFormattedCreateDate() {
        if (getCreateDate() == null)
            return "";
        return PluginUtil.getDateFormat().format(getCreateDate());
    }

    public Date getModifyDate() {
        return processVO.getModifyDate();
    }

    public String getFormattedModifyDate() {
        if (getModifyDate() == null)
            return "";
        return PluginUtil.getDateFormat().format(getModifyDate());
    }

    public void setModifyDate(Date modDate) {
        processVO.setModifyDate(modDate);
    }

    public boolean isStub() {
        return processVO == null;
    }

    public List<VariableVO> getVariables() {
        List<VariableVO> variables = processVO.getVariables();
        if (variables == null)
            return new ArrayList<VariableVO>();
        Collections.sort(variables, new Comparator<VariableVO>() {
            public int compare(VariableVO v1, VariableVO v2) {
                return v1.getName().compareToIgnoreCase(v2.getName());
            }
        });
        return variables;
    }

    public void setVariables(List<VariableVO> variables) {
        processVO.setVariables(variables);
    }

    public VariableVO getVariable(String name) {
        for (VariableVO variableVO : getVariables()) {
            if (variableVO.getVariableName().equals(name))
                return variableVO;
        }
        return null;
    }

    public List<String> getVariableNames() {
        List<String> variableNames = new ArrayList<String>();
        List<VariableVO> variableVOs = getVariables();
        for (VariableVO variableVO : variableVOs) {
            variableNames.add(variableVO.getVariableName());
        }
        Collections.sort(variableNames, new Comparator<String>() {
            public int compare(String n1, String n2) {
                return n1.compareToIgnoreCase(n2);
            }
        });
        return variableNames;
    }

    public List<String> getDocRefVariableNames() {
        List<String> docRefVariableNames = new ArrayList<String>();
        List<VariableVO> variableVOs = getVariables();
        for (VariableVO variableVO : variableVOs) {
            String varType = variableVO.getVariableType();
            if ("com.qwest.mdw.model.FormDataDocument".equals(varType) // TODO
                                                                       // temp
                                                                       // hack
                                                                       // to
                                                                       // work
                                                                       // with
                                                                       // MDW 4
                    || VariableTranslator.isDocumentReferenceVariable(null, varType))
                docRefVariableNames.add(variableVO.getVariableName());
        }
        Collections.sort(docRefVariableNames);
        return docRefVariableNames;
    }

    public List<String> getNonDocRefVariableNames() {
        List<String> nonDocRefVariableNames = new ArrayList<String>();
        List<VariableVO> variableVOs = getVariables();
        for (VariableVO variableVO : variableVOs) {
            String varType = variableVO.getVariableType();
            if (!VariableTranslator.isDocumentReferenceVariable(null, varType))
                nonDocRefVariableNames.add(variableVO.getVariableName());
        }
        Collections.sort(nonDocRefVariableNames);
        return nonDocRefVariableNames;
    }

    public List<VariableVO> getInputVariables() {
        List<VariableVO> inputVars = new ArrayList<VariableVO>();
        for (VariableVO variableVO : getVariables()) {
            int category = variableVO.getVariableCategory().intValue();
            if (category == VariableVO.CAT_INPUT || category == VariableVO.CAT_INOUT)
                inputVars.add(variableVO);
        }
        return inputVars;
    }

    public List<VariableVO> getOutputVariables() {
        List<VariableVO> outputVars = new ArrayList<VariableVO>();
        for (VariableVO variableVO : getVariables()) {
            int category = variableVO.getVariableCategory().intValue();
            if (category == VariableVO.CAT_OUTPUT || category == VariableVO.CAT_INOUT)
                outputVars.add(variableVO);
        }
        return outputVars;
    }

    public List<VariableVO> getInputOutputVariables() {
        List<VariableVO> inputVars = new ArrayList<VariableVO>();
        for (VariableVO variableVO : getVariables()) {
            int category = variableVO.getVariableCategory().intValue();
            if (category == VariableVO.CAT_INPUT || category == VariableVO.CAT_INOUT
                    || category == VariableVO.CAT_OUTPUT || category == VariableVO.CAT_STATIC)
                inputVars.add(variableVO);
        }
        return inputVars;
    }

    public String getAttribute(String attrName) {
        return processVO.getAttribute(attrName);
    }

    public void setAttribute(String attrName, String value) {
        processVO.setAttribute(attrName, value);
    }

    public int getNextMajorVersion() {
        return processVO.getNewVersion(true);
    }

    public int getNextMinorVersion() {
        return processVO.getNewVersion(false);
    }

    public String getNewVersionString(boolean major) {
        return processVO.getNewVersionString(major);
    }

    public int getNewVersion(boolean major) {
        return processVO.getNewVersion(major);
    }

    public boolean exists() {
        // TODO for most recent files list
        return false;
    }

    public ImageDescriptor getImageDescriptor() {
        return MdwPlugin.getImageDescriptor("icons/process.gif");
    }

    public IPersistableElement getPersistable() {
        // persistence is provided by dao
        return null;
    }

    public String getToolTipText() {
        // TODO include qualifier
        return getName();
    }

    @Override
    public String getLabel() {
        if (hasInstanceInfo())
            return getName() + " - " + getProcessInstance().getId().toString();
        else
            return getName() + " " + getVersionLabel();
    }

    @Override
    public String getIcon() {
        return "process.gif";
    }

    public String getVersionLabel() {
        return "v" + getVersionString();
    }

    public String getLockingUser() {
        return processVO.getModifyingUser();
    }

    public void setLockingUser(String lockingUser) {
        processVO.setModifyingUser(lockingUser);
    }

    public Date getLockedDate() {
        return processVO.getModifyDate();
    }

    public void setLockedDate(Date lockedDate) {
        processVO.setModifyDate(lockedDate);
    }

    public boolean isLockedToUser() {
        if (getProject().isFilePersist())
            return !getProject().isRemote();

        String currentUser = getProject().getUser().getUsername();
        String lockingUser = getLockingUser();
        return currentUser.equalsIgnoreCase(lockingUser);
    }

    public int getPerformanceLevel() {
        return processVO.getPerformanceLevel();
    }

    public void setPerformanceLevel(int level) {
        setAttribute(WorkAttributeConstant.PERFORMANCE_LEVEL, String.valueOf(level));
    }

    public String getEmptyTransitionOutcome() {
        return processVO.getTransitionWithNoLabel();
    }

    public void setEmptyTransitionOutcome(String opt) {
        setAttribute(WorkAttributeConstant.TRANSITION_WITH_NO_LABEL, opt);
    }

    public int getDefaultTransitionRetryLimit() {
        String attr = getAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT);
        if (attr == null)
            return 0;
        else
            return Integer.parseInt(attr);
    }

    public void setDefaultTransitionRetryLimit(int limit) {
        setAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT, String.valueOf(limit));
    }

    public String getRenderingEngine() {
        return getAttribute(WorkAttributeConstant.RENDERING_ENGINE);
    }

    public void setRenderingEngine(String renderingEngine) {
        setAttribute(WorkAttributeConstant.RENDERING_ENGINE, renderingEngine);
    }

    public boolean isCompatibilityRendering() {
        return WorkAttributeConstant.COMPATIBILITY_RENDERING.equals(getRenderingEngine());
    }

    public String getExtension() {
        return null;
    }

    public boolean meetsVersionSpec(String version) {
        return processVO.meetsVersionSpec(version);
    }

    /**
     * remote means federated process
     */
    public boolean isRemote() {
        return processVO.isRemote();
    }

    public boolean isLatest() {
        return getNextVersion() == null;
    }

    public void remove(WorkflowProcess pv) {
        getProcessVO().remove(pv.getProcessVO());
    }

    public String getVcsAssetPath() {
        if (!getProject().isFilePersist())
            throw new UnsupportedOperationException("Only for VCS Assets");

        return getPackage().getVcsAssetPath() + "/" + getName() + ".proc";
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    public int compareTo(WorkflowProcess other) {
        int res = this.getName().compareToIgnoreCase(other.getName());
        if (res != 0)
            return res;
        // versions sorted in descending order
        return this.getVersion() - other.getVersion();
    }

    public boolean equals(Object o) {
        if (!(o instanceof WorkflowProcess))
            return false;

        if (!super.equals(o))
            return false;

        WorkflowProcess other = (WorkflowProcess) o;

        if (this.isDummy() != other.isDummy())
            return false;

        if (!this.getProject().equals(other.getProject()))
            return false;

        if (this.getProcessInstance() == null) {
            return other.getProcessInstance() == null;
        }
        else {
            if (other.getProcessInstance() == null)
                return false;
            else if (!this.getProcessInstance().equals(other.getProcessInstance()))
                return false;
        }

        return true;
    }

    public boolean isHomogeneous(WorkflowElement we) {
        if (!super.isHomogeneous(we))
            return false;

        if (!(we instanceof WorkflowProcess))
            return false;

        WorkflowProcess other = (WorkflowProcess) we;
        return (this.isTopLevel() == other.isTopLevel() && this.isArchived() == other.isArchived());
    }

    private boolean dummy;

    public boolean isDummy() {
        return dummy;
    }

    public void setDummy(boolean dummy) {
        this.dummy = dummy;
    }

    // for reverting
    private String oldDescription;

    /**
     * Revert the top level information in case changes aren't saved.
     */
    public void revert() {
        setDescription(oldDescription);
    }

    public void sync() {
        oldDescription = getDescription();
    }

    private DesignerDataAccess designerDataAccess; // for multi-thread support

    public DesignerDataAccess getDesignerDataAccess() {
        return designerDataAccess;
    }

    public void setDesignerDataAccess(DesignerDataAccess dataAccess) {
        this.designerDataAccess = dataAccess;
    }

    /**
     * don't change the format of this output since it is use for drag-and-drop
     * support
     */
    public String toString() {
        String packageLabel = getPackage() == null || getPackage().isDefaultPackage() ? ""
                : getPackage().getLabel();
        return "Process~" + getProject().getName() + "^" + packageLabel + "^" + getLabel();
    }

    @Override
    public Long getProcessId() {
        return getId();
    }

    @Override
    public boolean overrideAttributesApplied() {
        return getProcessVO().overrideAttributesApplied();
    }

    // for activities, transitions and embedded subprocesses
    private Map<String, Map<String, List<Long>>> dirtyAttributeOwnerPrefixes;

    public boolean isAttributeOwnerDirty(String prefix, String ownerType, Long ownerId) {
        if (prefix == null || dirtyAttributeOwnerPrefixes == null)
            return false;
        Map<String, List<Long>> dirtyAttributeOwners = dirtyAttributeOwnerPrefixes.get(prefix);
        if (dirtyAttributeOwners == null)
            return false;
        List<Long> dirtyOwnerIds = dirtyAttributeOwners.get(ownerType);
        if (dirtyOwnerIds == null)
            return false;
        else
            return dirtyOwnerIds.contains(ownerId);
    }

    public void setAttributeOwnerDirty(String prefix, String ownerType, Long ownerId,
            boolean dirty) {
        if (prefix == null)
            return;
        if (dirty) {
            if (dirtyAttributeOwnerPrefixes == null)
                dirtyAttributeOwnerPrefixes = new HashMap<String, Map<String, List<Long>>>();
            Map<String, List<Long>> dirtyAttributeOwners = dirtyAttributeOwnerPrefixes.get(prefix);
            if (dirtyAttributeOwners == null) {
                dirtyAttributeOwners = new HashMap<String, List<Long>>();
                dirtyAttributeOwnerPrefixes.put(prefix, dirtyAttributeOwners);
            }
            List<Long> dirtyOwnerIds = dirtyAttributeOwners.get(ownerType);
            if (dirtyOwnerIds == null) {
                dirtyOwnerIds = new ArrayList<Long>();
                dirtyAttributeOwners.put(ownerType, dirtyOwnerIds);
            }
            if (!dirtyOwnerIds.contains(ownerId))
                dirtyOwnerIds.add(ownerId);
        }
        else {
            if (dirtyAttributeOwnerPrefixes != null) {
                Map<String, List<Long>> dirtyAttributeOwners = dirtyAttributeOwnerPrefixes
                        .get(prefix);
                if (dirtyAttributeOwners != null) {
                    List<Long> dirtyOwnerIds = dirtyAttributeOwners.get(ownerType);
                    if (dirtyOwnerIds != null)
                        dirtyOwnerIds.remove(ownerId);
                }
            }
        }
    }

    public boolean isAnyAttributeOwnerDirty() {
        if (dirtyAttributeOwnerPrefixes != null) {
            for (String prefix : dirtyAttributeOwnerPrefixes.keySet()) {
                Map<String, List<Long>> dirtyAttributeOwners = dirtyAttributeOwnerPrefixes
                        .get(prefix);
                if (dirtyAttributeOwners != null && !dirtyAttributeOwners.isEmpty()) {
                    for (String ownerType : dirtyAttributeOwners.keySet()) {
                        List<Long> dirtyOwnerIds = dirtyAttributeOwners.get(ownerType);
                        if (!dirtyOwnerIds.isEmpty())
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public void clearAttributeOwnersDirty() {
        dirtyAttributeOwnerPrefixes = null;
    }

    @Override
    public void fireElementChangeEvent(ChangeType changeType, Object newValue) {
        super.fireElementChangeEvent(changeType, newValue);
        if (getProject().isFilePersist()) {
            if (changeType == ChangeType.ELEMENT_CREATE || changeType == ChangeType.ELEMENT_DELETE
                    || changeType == ChangeType.RENAME)
                getPackage().refreshFolder();
            else if (changeType == ChangeType.VERSION_CHANGE)
                getPackage().refreshMdwMetaFolder();
        }
    }

}
