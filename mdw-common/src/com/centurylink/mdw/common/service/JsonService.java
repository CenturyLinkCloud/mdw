/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.Map;

import org.json.JSONObject;

public interface JsonService extends TextService, RegisteredService {
    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException;
}
