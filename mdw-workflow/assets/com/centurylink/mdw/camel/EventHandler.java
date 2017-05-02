/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import org.apache.camel.Message;

public interface EventHandler {

    /**
     * Return the request document object type.
     * @return the class name of the object returned by getRequestDocument()
     */
    public String getRequestDocumentType(Message request) throws MdwCamelException;

    /**
     * Parse the request message body and return its object representation.
     * The returned object should be a supported MDW Document type.
     * @param request - the request message
     * @return the document object representing the request body
     */
    public Object initializeRequestDocument(Message request) throws MdwCamelException;

    /**
     * Build the response based on the the message content.
     * @param code status code
     * @param message status message
     * @return the string response
     */
    public String getResponse(int code, String message);
}
