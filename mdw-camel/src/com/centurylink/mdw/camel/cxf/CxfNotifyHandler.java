/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.cxf;

import org.apache.camel.Message;
import org.apache.camel.component.cxf.CxfPayload;

import com.centurylink.mdw.camel.MdwCamelException;
import com.centurylink.mdw.camel.event.DefaultNotifyHandler;

public class CxfNotifyHandler extends DefaultNotifyHandler {

    @Override
    public String getRequestDocumentType(Message request) throws MdwCamelException {
        return CxfPayload.class.getName();
    }

    @Override
    public Object initializeRequestDocument(Message request) throws MdwCamelException {
        return (CxfPayload<?>)request.getBody();
    }

}
