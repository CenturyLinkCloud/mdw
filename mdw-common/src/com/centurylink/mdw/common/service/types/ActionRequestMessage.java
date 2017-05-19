/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.common.service.types;

import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.xml.XmlBeanWrapper;

public class ActionRequestMessage extends XmlBeanWrapper implements Jsonable {

    public ActionRequestMessage() {
        super(ActionRequestDocument.Factory.newInstance());
        setActionRequest(getActionRequestDocument().addNewActionRequest());
    }

    public ActionRequestMessage(ActionRequestDocument actionRequestDoc) {
        super(actionRequestDoc);
    }

    public ActionRequestMessage(String actionRequest) throws XmlException {
        super();
        fromXml(actionRequest);
    }

    public ActionRequestMessage(JSONObject jsonObj) throws JSONException, XmlException {
        this();
        JSONObject action = jsonObj.getJSONObject("action");
        setAction(action.getString("name"));
        if (jsonObj.has("parameters")) {
            JSONArray parameters = jsonObj.getJSONArray("parameters");
            for (int i = 0; i < parameters.length(); i++) {
                JSONObject parameter = parameters.getJSONObject(i);
                String paramName = JSONObject.getNames(parameter)[0];
                addParameter(paramName, parameter.getString(paramName));
            }
        }
    }

    public void fromXml(String xml) throws XmlException {
        XmlOptions xmlOptions = super.getXmlLoadOptions();
        xmlOptions.setDocumentType(ActionRequestDocument.type);
        setXmlBean(ActionRequestDocument.Factory.parse(xml, xmlOptions));
    }

    public ActionRequestDocument getActionRequestDocument() {
        return (ActionRequestDocument) getXmlBean();
    }

    public ActionRequest getActionRequest() {
        if (getActionRequestDocument() == null)
            return null;
        return getActionRequestDocument().getActionRequest();
    }

    public void setActionRequest(ActionRequest actionRequest) {
        getActionRequestDocument().setActionRequest(actionRequest);
    }

    public Action getAction() {
        if (getActionRequest() == null)
            return null;
        return getActionRequest().getAction();
    }

    public String getActionName() {
        if (getAction() == null)
            return null;
        return getAction().getName();
    }

    public List<Parameter> getParameters() {
        if (getAction() == null)
            return null;
        return getAction().getParameterList();
    }

    public void setAction(Action action) throws XmlException {
        if (getActionRequest() == null)
            throw new XmlException("Missing ActionRequest");
        getActionRequest().setAction(action);
    }

    public void setAction(String actionName) throws XmlException {
        if (getActionRequest() == null)
            throw new XmlException("Missing ActionRequest");
        if (getAction() == null)
            setAction(getActionRequest().addNewAction());
        getAction().setName(actionName);
    }

    public void addParameter(String name, String value) throws XmlException {
        if (getAction() == null)
            throw new XmlException("Missing Action");
        Parameter param = getAction().addNewParameter();
        param.setName(name);
        param.setStringValue(value);
        param.setType("java.lang.String");
    }

    public String getParameterValue(String name) {
        if (getParameters() == null)
            return null;
        for (Parameter parameter : getParameters()) {
            if (parameter.getName().equals(name))
                return parameter.getStringValue();
        }
        return null;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject actionRequest = create();
        JSONObject action = create();
        action.put("name", getActionName());
        List<Parameter> params = getParameters();
        if (params != null && params.size() > 0) {
            JSONArray parameters = new JSONArray();
            for (Parameter param : params) {
                JSONObject parameter = create();
                parameter.put(param.getName(), param.getStringValue());
                parameters.put(parameter);
            }
            action.put("parameters", parameters);
        }
        actionRequest.put("Action", action);
        return actionRequest;
    }

    public String getJsonName() { return "actionRequest"; }

}
