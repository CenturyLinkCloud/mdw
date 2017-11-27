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
package com.centurylink.mdw.services.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.task.AllowableAction;
import com.centurylink.mdw.task.BulkTaskActions;
import com.centurylink.mdw.task.ForTask;
import com.centurylink.mdw.task.TaskActionStatus;
import com.centurylink.mdw.task.TaskActionsDocument;
import com.centurylink.mdw.task.TaskActionsDocument.TaskActions;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Keeps track of the task actions permitted according to mdw-task-actions.xml.
 * If mdw-task-actions.xml cannot be found in the framework hub package or in a custom override package (bootstrap condition),
 * then no actions are allowed.
 */
public class AllowableTaskActions implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void initialize(Map<String,String> params) {
    }

    public void clearCache() {
        taskActions = null;
        myTasksBulkActions = null;
        workgroupTasksBulkActions = null;
        taskStatusAllowableActions = null;
    }

    public void loadCache() throws CachingException {
        try {
            getTaskActions();
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }


    public void refreshCache() throws Exception {
        clearCache();
        loadCache();
    }

    private static TaskActions taskActions;
    private static TaskActions getTaskActions() throws IOException, XmlException {
        if (taskActions == null) {
            File assetLoc = ApplicationContext.getAssetRoot();
            if (assetLoc == null) {
                logger.info("Task service actions are not loaded for non-VCS Assets");
                taskActions = createEmptyTaskActions();
            }
            else {
                String hubOverridePackage = ApplicationContext.getHubOverridePackage();
                String actionsAssetName = PropertyManager.getProperty(PropertyNames.MDW_TASK_ACTION_DEF);
                if (actionsAssetName == null)
                    actionsAssetName = "mdw-task-actions.xml";
                File overrideTaskActions = new File(assetLoc + "/" + hubOverridePackage + "/" + actionsAssetName);
                // allow empty namespace
                Map<String,String> nsMap = new HashMap<String,String>();
                nsMap.put("", "http://mdw.centurylink.com/task");
                XmlOptions options = new XmlOptions().setLoadSubstituteNamespaces(nsMap);
                if (overrideTaskActions.isFile()) {
                    taskActions = TaskActionsDocument.Factory.parse(overrideTaskActions, options).getTaskActions();
                    logger.info("Hub override task actions loaded from: " + overrideTaskActions);
                }
                else {
                    File standardTaskActions = new File(assetLoc + "/com/centurylink/mdw/task/mdw-task-actions.xml");
                    if (!standardTaskActions.isFile()) {
                        logger.severe("*** Standard task actions file does not exist: " + standardTaskActions + " ***");
                        taskActions = createEmptyTaskActions();
                    }
                    else {
                        taskActions = TaskActionsDocument.Factory.parse(standardTaskActions, options).getTaskActions();
                        logger.info("Hub standard task actions loaded from: " + standardTaskActions);
                    }
                }
            }
        }
        return taskActions;
    }

    private static TaskActions createEmptyTaskActions() {
        TaskActions taskActions = TaskActions.Factory.newInstance();
        BulkTaskActions bulkActions = taskActions.addNewBulkTaskActions();
        bulkActions.addNewAllowableBulkActions();
        bulkActions.addNewAssignedBulkActions();
        taskActions.addNewTaskDetailActions();
        return taskActions;
    }

    private static List<TaskAction> myTasksBulkActions; // populated lazily
    public static List<TaskAction> getMyTasksBulkActions() throws IOException, XmlException {
        if (myTasksBulkActions == null) {
            myTasksBulkActions = new ArrayList<TaskAction>();
            for (AllowableAction allowableAction : getTaskActions().getBulkTaskActions().getAssignedBulkActions().getAllowableActionList()) {
                TaskAction taskAction = convertToJsonable(allowableAction);
                if (!myTasksBulkActions.contains(taskAction))
                    myTasksBulkActions.add(taskAction);
            }
        }
        return myTasksBulkActions;
    }

    private static List<TaskAction> workgroupTasksBulkActions; // populated lazily
    public static List<TaskAction> getWorkgroupTasksBulkActions() throws IOException, XmlException {
        if (workgroupTasksBulkActions == null) {
            workgroupTasksBulkActions = new ArrayList<TaskAction>();
            for (AllowableAction allowableAction : getTaskActions().getBulkTaskActions().getAllowableBulkActions().getAllowableActionList()) {
                TaskAction taskAction = convertToJsonable(allowableAction);
                if (!workgroupTasksBulkActions.contains(taskAction))
                    workgroupTasksBulkActions.add(taskAction);
            }
        }
        return workgroupTasksBulkActions;
    }

    private static Map<String,List<TaskAction>> taskStatusAllowableActions; // populated lazily
    public static Map<String,List<TaskAction>> getTaskStatusAllowableActions() throws IOException, XmlException {
        if (taskStatusAllowableActions == null) {
            taskStatusAllowableActions = new HashMap<String,List<TaskAction>>();
            for (TaskActionStatus taskActionStatus : getTaskActions().getTaskDetailActions().getTaskStatusList()) {
                String status = taskActionStatus.getStatus();
                List<TaskAction> taskActions = new ArrayList<TaskAction>();
                taskStatusAllowableActions.put(status, taskActions);
                for (AllowableAction allowableAction: taskActionStatus.getAllowableActionList()) {
                    TaskAction taskAction = convertToJsonable(allowableAction);
                    if (!taskActions.contains(taskAction))
                        taskActions.add(taskAction);
                }
            }
        }
        return taskStatusAllowableActions;
    }

    public static List<TaskAction> getTaskDetailActions(String userCuid, TaskRuntimeContext runtimeContext)
    throws IOException, XmlException, ServiceException, DataAccessException {
        List<TaskAction> taskActions = new ArrayList<TaskAction>();
        String status = runtimeContext.getStatus();
        for (TaskAction taskAction : getTaskStatusAllowableActions().get(status)) {
            if (!taskActions.contains(taskAction)) {
                if (taskAction.getForTasks() != null && !taskAction.getForTasks().isEmpty()) {
                    // forTasks are specified, so only add the action if task logical id (or name) matches
                    for (TaskAction.ForTask forTask : taskAction.getForTasks()) {
                        if (runtimeContext.getLogicalId().equals(forTask.getTaskId()) || runtimeContext.getTaskName().equals(forTask.getTaskId()))
                            taskActions.add(taskAction);
                    }
                }
                else {
                    taskActions.add(taskAction);
                }
            }
        }

        // return filtered task actions (logic copied from TaskDetail but could stand refactoring)
        List<TaskAction> filteredActions = new ArrayList<TaskAction>();
        if (runtimeContext.getAssignee() == null || !userCuid.equalsIgnoreCase(runtimeContext.getAssignee().getCuid())) {
            // if the task is not assigned to the current user, the only possible actions are Assign, Claim and Release
            for (TaskAction action : taskActions) {
                if (action.getTaskActionName().equalsIgnoreCase(TaskAction.ASSIGN)
                        || action.getTaskActionName().equals(TaskAction.CLAIM)
                        || action.getTaskActionName().equals(TaskAction.RELEASE)) {
                    filteredActions.add(action);
                }
            }
        }
        else {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(runtimeContext.getTaskInstance());
            filteredActions = helper.filterStandardActions(taskActions);
            if (!runtimeContext.getTaskInstance().isInFinalStatus()) {
                boolean isWorkActionApplicable = false;
                for (TaskAction action : taskActions) {
                    if (action.getTaskActionName().equals(TaskAction.WORK)) {
                        isWorkActionApplicable = true;
                        break;
                    }
                }
                // if 'Work' action is applicable, no dynamic actions unless 'In Progress' status
                if (!isWorkActionApplicable || runtimeContext.getStatus().equals(TaskStatus.STATUSNAME_IN_PROGRESS)) {
                    List<TaskAction> dynamicTaskActions = helper.getCustomActions();
                    if (dynamicTaskActions != null) {
                        for (TaskAction dynamicTaskAction : dynamicTaskActions) {
                            if (!filteredActions.contains(dynamicTaskAction)) {
                                dynamicTaskAction.setCustom(true);
                                filteredActions.add(dynamicTaskAction);
                            }
                        }
                    }
                }
            }
        }

        return filteredActions;
    }

    private static TaskAction convertToJsonable(AllowableAction allowableAction) {
        TaskAction taskAction = new TaskAction();
        taskAction.setTaskActionName(allowableAction.getDomNode().getFirstChild().getNodeValue().trim());
        taskAction.setAlias(allowableAction.getAlias());
        taskAction.setRequireComment(allowableAction.getRequireComment());
        taskAction.setOutcome(allowableAction.getNavigationOutcome());
        taskAction.setAutoSave(allowableAction.getAutosave());
        List<ForTask> forTasks = allowableAction.getForTaskList();
        if (forTasks != null && !forTasks.isEmpty()) {
            List<TaskAction.ForTask> taskActionForTasks = new ArrayList<TaskAction.ForTask>();
            for (ForTask forTask : forTasks) {
                String taskId /* or name */ = forTask.getDomNode().getFirstChild().getNodeValue().trim();
                taskActionForTasks.add(taskAction.new ForTask(taskId, forTask.getDestinationList()));
            }
            taskAction.setForTasks(taskActionForTasks);
        }
        return taskAction;
    }
}
