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
package com.centurylink.mdw.slack;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Comment;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class TaskHandler implements ActionHandler, EventHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public JSONObject handleRequest(String userId, String id, SlackRequest request)
            throws ServiceException {
        Long instanceId;
        try {
            instanceId = Long.parseLong(id);
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid id: " + id);
        }

        if (request.getActions() != null) {
            // request is direct action
            String action = request.getActions().get(0); // TODO multiples?
            if (action == null)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Missing action");

            String assigneeId = null;
            if (action.equalsIgnoreCase(TaskAction.ASSIGN)) {
                assigneeId = request.getValue();
            }
            else if (action.equalsIgnoreCase(TaskAction.CLAIM)) {
                assigneeId = userId;
            }

            // TODO: TaskActionValidator?
            TaskServices taskServices = ServiceLocator.getTaskServices();

            // TODO: Retrieve template based on returned task instance.
            // If template is configured to send slack notices for this action, do not send the default below.
            taskServices.performAction(instanceId, action, userId, assigneeId, null, null, true);

            JSONObject json = new JSONObject();
            json.put("response_type", "in_channel");
            json.put("replace_original", false);
            if (request.getMessageTs() != null) {
                json.put("thread_ts", request.getMessageTs());
                json.put("reply_broadcast", true);
            }
            String messageText;
            if (assigneeId != null)
                messageText = "*Assigned* to " + assigneeId + " by " + request.getUser() + ".";
            else
                messageText = "Action: *" + action + "* performed by " + request.getUser() + ".";
            json.put("text", messageText);

            new Thread(() -> {
                Map<String,String> indexes = new HashMap<>();
                indexes.put("slack:response_url", request.getResponseUrl());
                if (request.getMessageTs() != null) {
                    logger.debug("Saving slack:message_ts=" + request.getMessageTs() + " for task " + instanceId);
                    indexes.put("slack:message_ts", request.getMessageTs());
                }
                try {
                    taskServices.updateIndexes(instanceId, indexes);
                    Comment comment = new Comment();
                    comment.setCreated(Date.from(Instant.now()));
                    comment.setCreateUser("mdw");
                    comment.setContent(new MarkdownScrubber(messageText).toMarkdown());
                    comment.setOwnerType(OwnerType.TASK_INSTANCE);
                    comment.setOwnerId(instanceId);
                    comment.setName("slack_message");
                    ServiceLocator.getCollaborationServices().createComment(comment);
                }
                catch(Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }).start();

            return json;
        }
        else {
            // options request
            String name = request.getName();
            if ("Assign".equals(name)) {
                User user = UserGroupCache.getUser(userId);
                UserServices userServices = ServiceLocator.getUserServices();
                try {
                   UserList assignees = userServices.findWorkgroupUsers(user.getGroupNames(), request.getValue());
                   JSONObject json = new JSONObject();
                   JSONArray optionsArr = new JSONArray();
                   json.put("options", optionsArr);
                   for (User assignee : assignees.getUsers()) {
                       JSONObject option = new JSONObject();
                       option.put("text", assignee.getName());
                       option.put("value", assignee.getCuid());
                       optionsArr.put(option);
                   }
                   return json;
                }
                catch (DataAccessException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad options request: " + name);
        }
    }

    /**
     * Messages posted on the "tasks" channel.
     */
    @Override
    public JSONObject handleEvent(SlackEvent event) throws ServiceException {
        if (event.getCallbackId() != null && event.getCallbackId().indexOf('_') > 0 && event.getCallbackId().indexOf('/') > 0 && event.getTs() != null) {
            Long instanceId = Long.parseLong(event.getCallbackId().substring(event.getCallbackId().lastIndexOf('/') + 1));
            new Thread(() -> {
                Map<String,String> indexes = new HashMap<>();
                logger.debug("Saving slack:message_ts=" + event.getTs() + " for task " + instanceId);
                indexes.put("slack:message_ts", event.getTs());
                try {
                    ServiceLocator.getTaskServices().updateIndexes(instanceId, indexes);
                }
                catch (Exception ex) {
                    logger.severeException("Error updating indexes for task " + instanceId + ": " + ex, ex);
                }
            }).start();
        }
        else if (event.getThreadTs() != null && event.getUser() != null) {
            new Thread(() -> {
                try {
                    TaskServices taskServices = ServiceLocator.getTaskServices();
                    Query query = new Query();
                    query.setFilter("index", "slack:message_ts=" + event.getThreadTs());
                    List<TaskInstance> instances = taskServices.getTasks(query).getTasks();
                    for (TaskInstance instance : instances) {
                        // add a corresponding note
                        Comment comment = new Comment();
                        comment.setCreated(Date.from(Instant.now()));
                        // TODO: lookup (or cache) users
                        comment.setCreateUser(event.getUser().equals("U4V5SG5PU") ? "Donald Oakes" : event.getUser());
                        comment.setContent(event.getText());
                        comment.setOwnerType(OwnerType.TASK_INSTANCE);
                        comment.setOwnerId(instance.getTaskInstanceId());
                        comment.setName("slack_message");
                        ServiceLocator.getCollaborationServices().createComment(comment);
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }).start();
        }
        return null;
    }
}
