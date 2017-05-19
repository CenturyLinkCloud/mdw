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
package com.centurylink.mdw.plugin.designer;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.MessageDialog;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Provides access to the cached data collections stored on Designer classic's
 * MainFrame object.
 */
public class PluginDataAccess {
    private WorkflowProject workflowProject;
    private DesignerDataModel designerDataModel;

    public DesignerDataModel getDesignerDataModel() {
        return designerDataModel;
    }

    private DesignerDataAccess designerDataAccess;

    DesignerDataAccess getDesignerDataAccess() {
        return designerDataAccess;
    }

    void setDesignerDataAccess(DesignerDataAccess designerDataAccess) {
        this.designerDataAccess = designerDataAccess;
    }

    public PluginDataAccess(WorkflowProject workflowProject, DesignerDataModel dataModel,
            DesignerDataAccess dataAccess) {
        this.workflowProject = workflowProject;
        this.designerDataModel = dataModel;
        this.designerDataAccess = dataAccess;
    }

    /**
     * Do NOT use DesignerDataModel package cache
     */
    private List<PackageVO> packageVOs;

    public List<PackageVO> getPackages(boolean reload) {
        return getPackages(reload, null);
    }

    public List<PackageVO> getPackages(boolean reload, ProgressMonitor progressMonitor) {
        if (packageVOs == null || reload) {
            try {
                packageVOs = designerDataAccess.getAllPackages(progressMonitor);
                packageVOs.add(0,
                        determineDefaultPackage(packageVOs, designerDataModel.getProcesses()));
            }
            catch (final Exception ex) {
                // otherwise the message won't display because we're not on the
                // UI thread
                MdwPlugin.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        PluginMessages.uiError(ex, "Load Packages", workflowProject);
                    }
                });
            }
        }
        if (packageVOs == null) // can happen for MDW 3.2 db
        {
            packageVOs = new ArrayList<PackageVO>();
            packageVOs.add(0,
                    determineDefaultPackage(packageVOs, designerDataModel.getProcesses()));
        }

        return packageVOs;
    }

    private PackageVO determineDefaultPackage(List<PackageVO> packageList,
            List<ProcessVO> processes) {
        Map<String, PackageVO> processPackage = new HashMap<String, PackageVO>();
        PackageVO defaultPackage = new PackageVO();
        List<ProcessVO> defaultProcesses = new ArrayList<ProcessVO>();
        for (PackageVO pkg : packageList) {
            for (ProcessVO proc : pkg.getProcesses())
                processPackage.put(proc.getProcessName(), pkg);
        }
        for (ProcessVO proc : processes) {
            if (processPackage.get(proc.getProcessName()) == null)
                defaultProcesses.add(proc);
        }
        defaultPackage.setProcesses(defaultProcesses);

        defaultPackage.setExternalEvents(new ArrayList<ExternalEventVO>(0));
        defaultPackage.setPackageId(new Long(0));
        defaultPackage.setPackageName(PackageVO.DEFAULT_PACKAGE_NAME);
        return defaultPackage;
    }

    public List<ProcessVO> getProcesses(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadProcesses(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Processes", workflowProject);
            }
        }
        return designerDataModel.getProcesses();
    }

    public List<ProcessVO> getAllProcesses(boolean reload) {
        List<ProcessVO> allProcesses = new ArrayList<ProcessVO>();
        List<ProcessVO> latestProcesses = getProcesses(reload);
        for (ProcessVO latestProcess : latestProcesses) {
            allProcesses.add(latestProcess);
            ProcessVO prevVersion = latestProcess;
            while ((prevVersion = prevVersion.getPrevVersion()) != null) {
                allProcesses.add(prevVersion);
            }
        }
        return allProcesses;
    }

    public void removeProcess(ProcessVO processVO) {
        designerDataModel.getProcesses().remove(processVO);
        if (packageVOs != null) {
            for (PackageVO packageVO : packageVOs)
                packageVO.getProcesses().remove(processVO);
        }
    }

    public ProcessVO getProcess(Long processId) {
        for (ProcessVO processVO : designerDataModel.getProcesses()) {
            if (processVO.getProcessId().equals(processId))
                return processVO;
        }
        return null; // not found
    }

    public ProcessVO getProcess(String processName, String version) {
        for (ProcessVO processVO : getAllProcesses(false)) {
            if (processVO.getProcessName().equals(processName)
                    && processVO.getVersionString().equals(version))
                return processVO;
        }
        return null; // not found
    }

    public ProcessVO getLatestProcess(String processName) {
        for (ProcessVO processVO : getProcesses(false)) {
            if (processVO.getProcessName().equals(processName))
                return processVO;
        }
        return null; // not found
    }

    public ProcessVO retrieveProcess(String name, int version) {
        ProcessVO proc = getProcess(name, WorkflowProcess.getVersionString(version));
        if (proc == null)
            return null;
        try {
            return designerDataAccess.getProcess(proc.getProcessId(), null);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Retrieve Process", workflowProject);
            return null;
        }
    }

    public ProcessVO loadProcess(WorkflowProcess processVersion) {
        try {
            return designerDataAccess.getProcess(processVersion.getId(),
                    processVersion.getProcessVO());
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Load Process", workflowProject);
            return processVersion.getProcessVO();
        }
    }

    public List<ActivityImplementorVO> getActivityImplementors(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadActivityImplementors(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Activity Impls", workflowProject);
            }
        }
        List<ActivityImplementorVO> implementors = designerDataModel.getActivityImplementors();
        Collections.sort(implementors, new Comparator<ActivityImplementorVO>() {
            public int compare(ActivityImplementorVO aivo1, ActivityImplementorVO aivo2) {
                return aivo1.getImplementorClassName().compareTo(aivo2.getImplementorClassName());
            }
        });
        return implementors;
    }

    public ActivityImplementorVO getActivityImplementor(String implClass) {
        for (ActivityImplementorVO activityImpl : designerDataModel.getActivityImplementors()) {
            if (activityImpl.getImplementorClassName().equals(implClass))
                return activityImpl;
        }
        return null;
    }

    public List<ExternalEventVO> getExternalEvents(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadExternalEvents(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load External Events", workflowProject);
            }
        }
        List<ExternalEventVO> externalEvents = designerDataModel.getExternalEvents();
        Collections.sort(externalEvents, new Comparator<ExternalEventVO>() {
            public int compare(ExternalEventVO eevo1, ExternalEventVO eevo2) {
                return eevo1.getEventName().compareTo(eevo2.getEventName());
            }
        });
        return externalEvents;
    }

    public List<TaskVO> getTaskTemplates(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadTaskTemplates(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Task Templates", workflowProject);
            }
        }
        List<TaskVO> taskTemplates = designerDataModel.getTaskTemplates();
        Collections.sort(taskTemplates, new Comparator<TaskVO>() {
            public int compare(TaskVO tvo1, TaskVO tvo2) {
                return tvo1.getTaskName().compareTo(tvo2.getTaskName());
            }
        });
        return taskTemplates;
    }

    public List<TaskCategory> getTaskCategories(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadTaskCategories(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Task Names", workflowProject);
            }
        }
        return designerDataModel.getTaskCategories();
    }

    public List<String> getRoleNames(boolean reload) {
        if (reload) {
            try {
                designerDataModel.reloadRoleNames(designerDataAccess);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Role Names", workflowProject);
            }
        }
        List<String> roleNames = designerDataModel.getRoleNames();
        Collections.sort(roleNames);
        return roleNames;
    }

    public List<String> getProcessNames(boolean reload) {
        List<ProcessVO> processVOs = getProcesses(reload);
        List<String> processNames = new ArrayList<String>();
        for (ProcessVO processVO : processVOs) {
            processNames.add(processVO.getProcessName());
        }
        Collections.sort(processNames);
        return processNames;
    }

    public boolean processNameExists(PackageVO pkgVO, String processName) {
        if (pkgVO.getProcesses() != null)
            for (ProcessVO process : pkgVO.getProcesses()) {
                if (process.getProcessName().equals(processName))
                    return true;
            }
        return false;
    }

    public ProcessInstanceVO getProcessInstance(Long processInstanceId) {
        try {
            return getDesignerDataAccess().getProcessInstanceBase(processInstanceId, null);
        }
        catch (DataAccessOfflineException ex) {
            if (workflowProject.isFilePersist()) {
                PluginMessages.log(ex);
                MessageDialog.openError(MdwPlugin.getShell(), "Retrieve Process Instance",
                        "Server appears to be offline: " + ex.getMessage());
            }
            else {
                PluginMessages.uiError(ex, "Retrieve Process Instance", workflowProject);
            }
            return null;
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Retrieve Process Instance", workflowProject);
            return null;
        }
    }

    private List<RuleSetVO> userRuleSets;

    public void organizeRuleSets() {
        // designerDataModel rule_sets will have been previously loaded
        // use these since they've already been retrieved, even though we
        // maintain a separate cache
        allRuleSets = getDesignerDataModel().getAllRuleSets();

        userRuleSets = new ArrayList<RuleSetVO>();
        rulesetCustomAttributes = new HashMap<String, CustomAttributeVO>();
        for (RuleSetVO rs : allRuleSets) {
            userRuleSets.add(rs);
            if (rs.getLanguage() != null && !rulesetCustomAttributes.containsKey(rs.getLanguage()))
                rulesetCustomAttributes.put(rs.getLanguage(),
                        getDesignerDataModel().getRuleSetCustomAttribute(rs.getLanguage()));
        }
    }

    public void reloadRuleSets(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        getDesignerDataModel().reloadRuleSets(dao);
    }

    public List<RuleSetVO> getRuleSets(boolean reload) {
        if (reload) {
            try {
                reloadRuleSets(designerDataAccess);
                organizeRuleSets();
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load RuleSets", workflowProject);
            }
        }
        return userRuleSets;
    }

    private List<RuleSetVO> allRuleSets;

    public List<RuleSetVO> getAllRuleSets(boolean reload) {
        if (reload) {
            try {
                reloadRuleSets(designerDataAccess);
                organizeRuleSets();
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load RuleSets", workflowProject);
            }
        }
        return allRuleSets;
    }

    private Map<String, CustomAttributeVO> rulesetCustomAttributes;

    public CustomAttributeVO getRuleSetCustomAttribute(String language) {
        return rulesetCustomAttributes.get(language);
    }

    public void setRuleSetCustomAttribute(CustomAttributeVO customAttribute) {
        rulesetCustomAttributes.put(customAttribute.getCategorizer(), customAttribute);
    }

    public String getPrivileges() {
        return getDesignerDataModel().getPrivileges();
    }

    public CustomAttributeVO getAssetCustomAttribute(String language) {
        return getRuleSetCustomAttribute(language);
    }

    public void setAssetCustomAttribute(CustomAttributeVO customAttrVO) {
        try {
            getDesignerDataAccess().setCustomAttribute(customAttrVO);
            setRuleSetCustomAttribute(customAttrVO);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Set Custom Attribute", workflowProject);
        }
    }

    public void setAttributes(String owner, Long ownerId, List<AttributeVO> attrs) {
        try {
            Map<String, String> attributes = null;
            if (attrs != null) {
                attributes = new HashMap<String, String>();
                for (AttributeVO attr : attrs)
                    attributes.put(attr.getAttributeName(), attr.getAttributeValue());
            }
            getDesignerDataAccess().setAttributes(owner, ownerId, attributes);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Set Attributes", workflowProject);
        }
    }

    public void setAttribute(String owner, Long ownerId, String name, String value) {
        try {
            getDesignerDataAccess().setAttribute(owner, ownerId, name, value);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Set Attribute", workflowProject);
        }
    }

    public void auditLog(Action action, WorkflowElement element) throws DataAccessException {
        auditLog(action, element.getActionEntity(), element.getId(), element.getLabel());
    }

    public void auditLog(Action action, Entity entity, Long entityId, String comments)
            throws DataAccessException {
        UserActionVO userAction = new UserActionVO(workflowProject.getUser().getUsername(), action,
                entity, entityId, comments);
        userAction.setSource("Eclipse/RCP Designer");
        getDesignerDataAccess().auditLog(userAction);
    }

    public int getSchemaVersion() {
        return designerDataModel.getDatabaseSchemaVersion();
    }

    private int supportedSchemaVersion;

    public int getSupportedSchemaVersion() {
        return supportedSchemaVersion;
    }

    public void setSupportedSchemaVersion(int supported) {
        this.supportedSchemaVersion = supported;
    }

    public void reloadUserPermissions() {
        try {
            designerDataModel.reloadPriviledges(getDesignerDataAccess(),
                    workflowProject.getUser().getUsername());
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Reload Permissions", workflowProject);
        }
    }

    // TODO attribute overflow
    public Map<String, List<String>> getTaskVariableMappings() throws DataAccessException {
        Map<String, List<String>> ownerVars = new TreeMap<String, List<String>>(
                PluginUtil.getStringComparator());

        DatabaseAccess db = new DatabaseAccess(
                workflowProject.getMdwDataSource().getJdbcUrlWithCredentials());
        try {
            db.openConnection();
            if (workflowProject.checkRequiredVersion(5, 2)) {
                String query = "select t.task_name, a.attribute_value \n "
                        + "from task t, attribute a \n" + "where a.attribute_owner = 'TASK' \n"
                        + "and t.task_id = a.attribute_owner_id \n"
                        + "and a.attribute_name = 'Variables' \n" + "order by a.attribute_id desc";
                ResultSet rs = db.runSelect(query, null);
                while (rs.next()) {
                    String taskName = rs.getString("task_name");
                    if (!ownerVars.containsKey(taskName)) {
                        List<String> vars = new ArrayList<String>();
                        String varString = rs.getString("attribute_value");
                        if (varString != null && varString.trim().length() > 0) {
                            for (String var : varString.split(";")) {
                                int first = var.indexOf(',');
                                int second = var.indexOf(',', first + 1);
                                int third = var.indexOf(',', second + 1);
                                if (!var.substring(second + 1, third)
                                        .equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED))
                                    vars.add(first > 0 ? var.substring(0, first) : var);
                            }
                        }
                        Collections.sort(vars, PluginUtil.getStringComparator());
                        ownerVars.put(taskName, vars);
                    }
                }
            }
            else {
                String query = "select t.task_name, v.variable_name \n "
                        + "from task t, variable v, variable_mapping vm \n "
                        + "where vm.mapping_owner = 'TASK' \n "
                        + "and vm.mapping_owner_id = t.task_id \n "
                        + "and v.variable_id = vm.variable_id \n "
                        + "order by t.task_name, v.variable_name";
                ResultSet rs = db.runSelect(query, null);
                while (rs.next()) {
                    String taskName = rs.getString("task_name");
                    List<String> vars = ownerVars.get(taskName);
                    if (vars == null) {
                        vars = new ArrayList<String>();
                        ownerVars.put(taskName, vars);
                    }
                    String var = rs.getString("variable_name");
                    if (!vars.contains(var))
                        vars.add(var);
                }
            }
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }

        return ownerVars;
    }

    public RuleSetVO getRuleSet(Long ruleSetId) {
        for (RuleSetVO ruleSet : getAllRuleSets(false)) {
            if (ruleSet.getId().equals(ruleSetId))
                return ruleSet;
        }
        return null;
    }

    public void loadVariableTypes() {
        try {
            VariableTypeCache.loadCache(getVariableTypes(true));
        }
        catch (DataAccessException ex) {
            PluginMessages.log(ex);
        }
    }

    // store a separate copy since VariableTypeCache is static
    private List<VariableTypeVO> varTypes;

    public List<VariableTypeVO> getVariableTypes(boolean reload) {
        if (reload || varTypes == null) {
            try {
                if (MdwPlugin.getActiveWorkbenchWindow() != null)
                    designerDataModel.reloadVariableTypes(designerDataAccess);
                List<VariableTypeVO> cleanedUpVarTypes = new ArrayList<VariableTypeVO>();
                if (workflowProject.checkRequiredVersion(6, 0)) {
                    for (VariableTypeVO varType : designerDataModel.getVariableTypes()) {
                        switch (varType.getVariableType()) {
                            case "java.lang.String[]":
                            case "java.lang.Integer[]":
                            case "java.lang.Long[]":
                            case "java.util.Map":
                            case "com.centurylink.mdw.model.FormDataDocument":
                            case "com.centurylink.mdw.common.service.Jsonable":
                                break;
                            default:
                                cleanedUpVarTypes.add(varType);
                        }
                    }
                }
                else {
                    for (VariableTypeVO varType : designerDataModel.getVariableTypes()) {
                        switch (varType.getVariableType()) {
                            case "java.lang.Exception":
                            case "com.centurylink.mdw.model.Jsonable":
                                break;
                            default:
                                cleanedUpVarTypes.add(varType);
                        }
                    }
                }
                designerDataModel.setVariableTypes(cleanedUpVarTypes);
                varTypes = designerDataModel.getVariableTypes();
                Collections.sort(varTypes, new Comparator<VariableTypeVO>() {
                    public int compare(VariableTypeVO varType1, VariableTypeVO varType2) {
                        return varType1.getVariableTypeId().compareTo(varType2.getVariableTypeId());
                    }
                });
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Load Variable Types", workflowProject);
            }
        }
        return varTypes;
    }

    public VariableTypeVO getVariableType(String varTypeName) {
        for (VariableTypeVO varType : varTypes) {
            if (varType.getVariableType().equals(varTypeName))
                return varType;
        }
        return null;
    }

    public RuleSetVO loadRuleSet(Long ruleSetId) throws DataAccessException, RemoteException {
        return designerDataAccess.getRuleSet(ruleSetId);
    }
}
