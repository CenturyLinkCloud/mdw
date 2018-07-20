/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.List;

import org.apache.camel.Message;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.cxf.binding.soap.SoapHeader;
import org.w3c.dom.Node;

import com.centurylink.mdw.camel.MdwCamelException;
import com.centurylink.mdw.camel.DefaultProcessLaunchHandler;

public class CxfProcessLaunchHandler extends DefaultProcessLaunchHandler {

    @Override
    public String getRequestDocumentType(Message request) throws MdwCamelException {
        return CxfPayload.class.getName();
    }

    @Override
    public Object initializeRequestDocument(Message request) throws MdwCamelException {
        return (CxfPayload<?>)request.getBody();
    }

    /**
     * Assumes a 'MasterRequestID' SOAP header element.  Override for something different.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getMasterRequestId(Message request) {
        List<SoapHeader> headers = (List<SoapHeader>) ((CxfPayload<?>)request.getBody()).getHeaders();
        for (SoapHeader header : headers) {
            if (header.getName().getLocalPart().equals("MasterRequestID")) {
                Node headerNode = (Node) header.getObject();
                return headerNode.getTextContent();
            }
        }
        return null;
    }


}
