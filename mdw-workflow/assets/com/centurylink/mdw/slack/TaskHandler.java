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
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Note;
import com.centurylink.mdw.model.task.TaskAction;
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
            
            String comment = null;
            
            // TODO: TaskActionValidator?
            TaskServices taskServices = ServiceLocator.getTaskServices();
            taskServices.performAction(instanceId, action, userId, assigneeId, comment, null, true);

            JSONObject json = new JSONObject();
            json.put("response_type", "in_channel");
            json.put("replace_original", false);
            if (request.getMessageTs() != null) {
                json.put("thread_ts", request.getMessageTs());
                json.put("reply_broadcast", true);
            }
            String messageText;
            if (assigneeId != null)
                messageText = "Assigned to " + assigneeId + " by " + request.getUser() + ".";
            else
                messageText = "Action: " + action + " performed by " + request.getUser() + ".";
            json.put("text", messageText);
            
            new Thread(new Runnable() {
                public void run() {
                    Map<String,String> indexes = new HashMap<>();
                    indexes.put("slack:response_url", request.getResponseUrl());
                    if (request.getMessageTs() != null)
                        indexes.put("slack:message_ts", request.getMessageTs());
                    try {
                        taskServices.updateIndexes(instanceId, indexes);
                        Note comment = new Note();
                        comment.setCreated(Date.from(Instant.now()));
                        comment.setCreateUser("mdw");
                        comment.setContent(messageText);
                        comment.setOwnerType(OwnerType.TASK_INSTANCE);
                        comment.setOwnerId(instanceId);
                        comment.setName("slack_message");
                        ServiceLocator.getCollaborationServices().createNote(comment);
                    }
                    catch(Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
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

    @Override
    public JSONObject handleEvent(SlackEvent event) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }
}
