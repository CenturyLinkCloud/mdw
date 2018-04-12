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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class MdwEndpoint extends DefaultEndpoint {

    public enum RequestType {
        // producers
        process,
        notify,
        // consumers
        service,
        workflow
    }

    public MdwEndpoint(String endpointUri, String remaining, Component component) {
        super(endpointUri, component);
        RequestType type = null;
        if (RequestType.process.toString().equals(remaining)) {
            type = RequestType.process;
        }
        else if ((RequestType.notify.toString().equals(remaining))) {
            type = RequestType.notify;
        }
        else if (remaining.startsWith(RequestType.workflow.toString())) {
            type = RequestType.workflow;
            int firstSlash = remaining.indexOf('/');
            asset = remaining.substring(firstSlash + 1);
        }
        else {
            type = RequestType.service;
            int firstSlash = remaining.indexOf('/');
            if (firstSlash == -1)
            {
              protocol = remaining;
              path = null;
            }
            else
            {
              protocol = remaining.substring(0, firstSlash);
              path = remaining.substring(firstSlash + 1);
            }
        }
        this.requestType = type;
    }

    public Producer createProducer() throws Exception {
        return new MdwProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new MdwConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }

    private RequestType requestType;
    public RequestType getRequestType() { return requestType; }

    private String masterRequestId;
    public void setMasterRequestId(String id) { this.masterRequestId = id; }
    public String getMasterRequestId() { return masterRequestId; }

    private String handlerClass;
    public String getHandlerClass() { return handlerClass; }
    public void setHandlerClass(String handlerClass) { this.handlerClass = handlerClass; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String eventId;
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    private Map<String,String> namespaces;
    public Map<String,String> getNamespaces() { return namespaces; }
    public void setNamespaces(Map<String,String> namespaces) { this.namespaces = namespaces; }

    // for MdwConsumer
    private String protocol;
    public String getProtocol() { return protocol; }

    private String path;
    public String getPath() { return path; }

    private String asset;
    public String getAsset() { return asset; }

    @Override
    public boolean isLenientProperties() {
        return true;  // workflow url properties may be unknown
    }

    private Map<String,String> workflowParams;
    @Override
    public void configureProperties(Map<String,Object> options) {
        if (requestType.equals(RequestType.workflow)) {
            workflowParams = new HashMap<String,String>();
            for (String name : options.keySet())
                workflowParams.put(name, options.get(name).toString());
        }
    }

    public Map<String,String> getWorkflowParams() { return workflowParams; }

}
