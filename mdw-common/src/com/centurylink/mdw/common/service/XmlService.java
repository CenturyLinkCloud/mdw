/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.Map;

public interface XmlService extends TextService, RegisteredService {
    public static final String CONTENT = "content";

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException;
}
