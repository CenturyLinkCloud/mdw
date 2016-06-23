/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.adapter;

import java.util.Map;

import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;

public interface HeaderAwareAdapter {

    Map<String,String> getRequestHeaders();

    Map<String,String> getResponseHeaders();

    Object invoke(Object pConnection, Object requestData, Map<String,String> requestHeaders)
    throws AdapterException,ConnectionException;

}
