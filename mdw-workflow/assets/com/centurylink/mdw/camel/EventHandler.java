/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.camel;

import org.apache.camel.Message;
import com.centurylink.mdw.model.workflow.Package;

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

    public Package getPackage();
    void setPackage(Package pkg);
}
