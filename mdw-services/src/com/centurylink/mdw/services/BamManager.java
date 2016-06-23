/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.common.exception.DataAccessException;

public interface BamManager
{

    public String handleEventMessage(String request, XmlObject xmlbean, Map<String,String> metainfo)
    throws Exception;

    String getMainProcessName(String masterRequestId) throws DataAccessException;
    Long getMainProcessInstanceId(String masterRequestId) throws DataAccessException;
    public Long getMainProcessId(String masterRequestId) throws DataAccessException;
}
