/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

public class TaskActions implements JsonService, XmlService {

    public static final int HARD_MAX_TASK_ACTIONS = 100;

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        JSONArray actionsJson = new JSONArray();
        try {
            for (UserActionVO taskAction : getTaskActions(parameters)) {
                actionsJson.put(taskAction.getJson());
            }
            return actionsJson.toString(2);
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        StringBuffer actionsXml = new StringBuffer();
        for (UserActionVO taskAction : getTaskActions(parameters)) {
            actionsXml.append(taskAction.toXml());
        }
        actionsXml.append("</TaskActions>");
        return actionsXml.toString();
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }

    private List<TaskActionVO> getTaskActions(Map<String,Object> parameters) throws ServiceException {
        try {
            String user = (String) parameters.get("user");
            String since = (String) parameters.get("since");
            String max = (String) parameters.get("max");
            return getTaskActions(user, since, max);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private List<TaskActionVO> getTaskActions(String user, String since, String max) throws CachingException, DataAccessException {
        int maxRows = HARD_MAX_TASK_ACTIONS;
        try {
            maxRows = Integer.parseInt(max);
            if (maxRows > HARD_MAX_TASK_ACTIONS)
                maxRows = HARD_MAX_TASK_ACTIONS;
        }
        catch (NumberFormatException ex) {
        }
        Date sinceDate = StringHelper.stringToDate(since);
        RuntimeDataAccess runtimeData = DataAccess.getRuntimeDataAccess(new DatabaseAccess(null));
        UserGroupVO[] groupVOs = UserGroupCache.getUser(user).getWorkgroups();
        String[] groups = new String[groupVOs.length];
        for (int i = 0; i < groupVOs.length; i++)
          groups[i] = groupVOs[i].getName();

        return filterTaskActions(user, maxRows, runtimeData.getUserTaskActions(groups, sinceDate));
    }

    private List<TaskActionVO> filterTaskActions(String user, int max, List<TaskActionVO> unfiltered) {
        List<TaskActionVO> filtered = new ArrayList<TaskActionVO>();

        for (int i = 0; i < unfiltered.size() && i < max; i++) {
            TaskActionVO action = unfiltered.get(i);
            boolean isAssigned = action.getAction() == Action.Assign;
            boolean isClaimed = action.getAction() == Action.Claim;
            // assignments are only notified to assignee
            if (!isClaimed && (!isAssigned || user.equals(action.getDestination())))
                filtered.add(action);
        }
        return filtered;
    }

}
