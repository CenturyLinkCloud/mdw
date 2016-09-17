/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.event;

import java.io.Serializable;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.xml.XmlPath;

public class ExternalEventVO implements Serializable, Comparable<ExternalEventVO>, Jsonable {

    private Long id;
    private String eventName;
    private String eventHandler;
    private XmlPath xpath;

    public ExternalEventVO(){
    	xpath = null;
    }

    public ExternalEventVO(Long id, String name, String handler) {
        this.id = id;
        eventName = name;
        eventHandler = handler;
        xpath = null;
    }

	/**
	 * @return the eventData
	 */
	public String getEventHandler() {
		return eventHandler;
	}
	/**
	 * @param eventData the eventData to set
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

	public XmlPath getXpath() throws XmlException {
		if (xpath==null) xpath = new XmlPath(eventName);
		return xpath;
	}

    public String getSimpleName() {
        String simpleName;
        if (eventHandler.startsWith("START_PROCESS") || eventHandler.startsWith("NOTIFY_PROCESS")) {
            simpleName = eventName.replace('/', '-') + "=" + eventHandler.replace('?', '_').replace('/', '-');
        }
        else {
            simpleName = eventName.replace('/', '-') + "=";
            int lastDot = eventHandler.lastIndexOf('.');
            if (lastDot > 0)
                simpleName += eventHandler.substring(lastDot + 1); // without package
            else
                simpleName += eventHandler;
        }

        return FileHelper.stripDisallowedFilenameChars(simpleName);
    }

    @Override
    public int compareTo(ExternalEventVO other) {
        return this.getEventName().compareTo(other.getEventName());
    }

    /**
     * Currently this is only used for File-based and VCS-based persistence (esp. createExternalEvent()).
     */
    private String packageName;
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public ExternalEventVO(JSONObject json) throws JSONException {
        this.eventName = json.getString("path");
        this.eventHandler = json.getString("handlerClass");
    }

    /**
     * TODO: When/if eventHandlers become full-fledged assets, we can decouple asset name from eventName.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("path", eventName);
        json.put("handlerClass", eventHandler);
        return json;
    }

    public String getJsonName() {
        return this.eventName;
    }
}
