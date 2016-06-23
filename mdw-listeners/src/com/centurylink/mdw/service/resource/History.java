/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.HistoryServices;
import com.centurylink.mdw.services.ServiceLocator;

public class History implements JsonService {

    public String getJson(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {
        HistoryServices historyServices = ServiceLocator.getHistoryServices();
        try {
            String historyLength = (String)parameters.get("historyLength");
            if (historyLength == null)
                return historyServices.getHistory(0).getJson().toString(2);
            else
                return historyServices.getHistory(new Integer(historyLength).intValue()).getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }

}
