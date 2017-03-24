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