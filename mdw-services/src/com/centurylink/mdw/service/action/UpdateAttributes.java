/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;

/**
 * Updates override attributes.
 * @deprecated @use com.centurylink.mdw.service.rest.Attributes.
 */
@Deprecated
public class UpdateAttributes implements JsonService {

    public String getJson(JSONObject requestJson, Map<String,String> metaInfo) throws ServiceException {
        String ownerType = (String)metaInfo.get("ownerType");
        if (ownerType == null)
            throw new ServiceException("Missing parameter: ownerType");
        String ownerId = (String)metaInfo.get("ownerId");
        if (ownerId == null)
            throw new ServiceException("Missing parameter: ownerId");
        if (requestJson == null)
            throw new ServiceException("Missing JSON object: attributes");

        try {
            Map<String,String> attrs = new HashMap<String,String>();
            String[] names = JSONObject.getNames(requestJson);
            if (names != null) {
                for (String name : names)
                  attrs.put(name, requestJson.getString(name));
            }

            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();

            workflowServices.setAttributes(ownerType, Long.parseLong(ownerId), attrs);

            // TODO audit log

            return null;
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }

}
