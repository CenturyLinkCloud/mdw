/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.model.event;

import java.io.Serializable;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.xml.XmlPath;

public class ExternalEvent implements Serializable, Comparable<ExternalEvent>, Jsonable {

    private Long id;
    private String eventName;
    private String eventHandler;
    private XmlPath xpath;
    private String eventMessagePattern;
    private Boolean isContentRouting = true;

    public ExternalEvent(){
        xpath = null;
    }

    public ExternalEvent(Long id, String name, String handler, String messagePattern) {
        this.id = id;
        eventName = name;
        eventHandler = handler;
        xpath = null;
        eventMessagePattern = messagePattern;
    }

    /**
     * @return the eventData
     */
    public String getEventHandler() {
        return eventHandler;
    }
    /**
     * @param eventHandler the eventData to set
     */
    public void setEventHandler(String eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * @return the eventName
     */
    public String getEventName() {
        return eventName;
    }
    /**
     * @param eventName the eventName to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getMessagePattern() {
        return eventMessagePattern;
    }

    public void setMessagePattern(String eventMessagePattern) {
        this.eventMessagePattern = eventMessagePattern;
    }

    public Boolean isContentRouting() { return isContentRouting == null || isContentRouting == true; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public XmlPath getXpath() throws XmlException {
        if (xpath==null) xpath = new XmlPath(eventMessagePattern);
        return xpath;
    }

    public String getSimpleName() {
        eventHandler.replace('?', '_').replace('/', '-');
        String simpleName = eventName.replace('/', '-') ;
        return FileHelper.stripDisallowedFilenameChars(simpleName);
    }

    @Override
    public int compareTo(ExternalEvent other) {
        return this.getEventName().compareTo(other.getEventName());
    }

    /**
     * Currently this is only used for File-based and VCS-based persistence
     * (esp. createExternalEvent()).
     */
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public ExternalEvent(JSONObject json) throws JSONException {
        this.eventMessagePattern = json.getString("path");
        this.eventHandler = json.getString("handlerClass");
        this.isContentRouting = "path".equalsIgnoreCase(json.optString("routing")) ? false : true;
    }

    /**
     * TODO: When/if eventHandlers become full-fledged assets, we can decouple
     * asset name from eventName.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("path", eventMessagePattern);
        json.put("handlerClass", eventHandler);
        json.put("routing", isContentRouting ? "content" : "path");
        return json;
    }

    public String getJsonName() {
        return this.eventName;
    }
}
