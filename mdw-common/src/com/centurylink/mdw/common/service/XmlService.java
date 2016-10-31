/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;

public interface XmlService extends TextService, RegisteredService {
    public String getXml(XmlObject xml, Map<String,String> metaInfo) throws ServiceException;
}
