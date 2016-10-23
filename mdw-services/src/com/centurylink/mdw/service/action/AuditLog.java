/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;

public class AuditLog implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        try {
            JSONObject jsonObject = (JSONObject) parameters.get("userAction");
            if (jsonObject == null)
                throw new ServiceException("Missing parameter: 'userAction'.");
            UserAction userAction = new UserAction(jsonObject);
            UserServices userServices = ServiceLocator.getUserServices();
            userServices.auditLog(userAction);
            return null;  // success
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}