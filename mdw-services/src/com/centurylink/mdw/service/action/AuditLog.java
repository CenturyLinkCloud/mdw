/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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

    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        try {
            if (request == null)
                throw new ServiceException("Missing parameter: 'userAction'.");
            UserAction userAction = new UserAction(request);
            UserServices userServices = ServiceLocator.getUserServices();
            userServices.auditLog(userAction);
            return null;  // success
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Object request, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)request, metaInfo);
    }
}