package com.centurylink.mdw.adapter;

import java.util.Map;

public interface HeaderAwareAdapter {

    Map<String,String> getRequestHeaders();

    Map<String,String> getResponseHeaders();

    Object invoke(Object pConnection, Object requestData, Map<String,String> requestHeaders)
    throws AdapterException,ConnectionException;

}
