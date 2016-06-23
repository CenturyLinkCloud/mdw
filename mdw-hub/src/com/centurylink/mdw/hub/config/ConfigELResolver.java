/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.hub.config.interfaces.InterfaceInstanceEnactor;
import com.centurylink.mdw.hub.config.interfaces.InterfaceList;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.workflow.Interface;
import com.centurylink.mdw.workflow.InterfaceInstance;

public class ConfigELResolver extends ELResolver {
    public ConfigELResolver() {
    }

    // run-time overrides

    @Override
    public Class<?> getType(ELContext elContext, Object base, Object property)
    throws NullPointerException, PropertyNotFoundException, ELException {
        if (base instanceof InterfaceInstanceEnactor && property instanceof String) {
            elContext.setPropertyResolved(true);
            return String.class;
        }
        else {
            return null;
        }
    }

    @Override
    public Object getValue(ELContext elContext, Object base, Object property)
    throws NullPointerException, PropertyNotFoundException, ELException {
        if (base != null && base instanceof InterfaceList) {
            if (property instanceof String) {
                elContext.setPropertyResolved(true);
                InterfaceList interfaceList = (InterfaceList) base;
                return interfaceList.getInterface((String) property);
            }
        }
        else if (base != null && base instanceof InterfaceInstanceEnactor) {
            if (property instanceof String) {
                elContext.setPropertyResolved(true);
                InterfaceInstanceEnactor enactor = (InterfaceInstanceEnactor) base;
                ConfigManager configManager = (ConfigManager) FacesVariableUtil.getValue("configManager");
                try {
                    return enactor.getInterfaceInstance(configManager.getInterfaces().getInterface(
                            (String) property));
                }
                catch (Exception ex) {
                    throw new ELException(ex.getMessage(), ex);
                }
            }
        }

        return null;
    }

    @Override
    public void setValue(ELContext elContext, Object base, Object property, Object value)
    throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException {
        if (base instanceof InterfaceInstanceEnactor && property instanceof String && value instanceof String) {
            elContext.setPropertyResolved(true);
            InterfaceInstanceEnactor enactor = (InterfaceInstanceEnactor) base;
            ConfigManager configManager = (ConfigManager) FacesVariableUtil.getValue("configManager");
            Interface iface = configManager.getInterfaces().getInterface((String) property);
            try {
                enactor.setInterfaceInstance(iface, jsonToInterfaceInstance(iface, (String) value));
            }
            catch (Exception ex) {
                throw new ELException(ex.getMessage(), ex);
            }
        }
    }

    // design-time overrides

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if (base != null)
            return null;

        return String.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext elContext, Object base, Object property)
    throws NullPointerException, PropertyNotFoundException, ELException {
        return true;
    }

    /**
     * @param json
     * @return
     */
    private InterfaceInstance jsonToInterfaceInstance(Interface iface, String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        InterfaceInstance ifaceInst = InterfaceInstance.Factory.newInstance();
        ifaceInst.setInterfaceName(jsonObj.getString("name"));
        ifaceInst.setEndPointValue(jsonObj.getString("endPoint"));
        if (iface.getProtocol().equals("BUS"))
            ifaceInst.setTopic(jsonObj.getString("topic"));
        ifaceInst.setLogRequest(jsonObj.getBoolean("logRequest"));
        ifaceInst.setLogResponse(jsonObj.getBoolean("logResponse"));
        if (iface.getRequestXsdUrl() != null)
            ifaceInst.setValidateRequest(jsonObj.getBoolean("validateRequest"));
        if (iface.getResponseXsdUrl() != null)
            ifaceInst.setValidateResponse(jsonObj.getBoolean("validateResponse"));
        ifaceInst.setStubMode(jsonObj.getBoolean("stubMode"));
        if (ifaceInst.getStubMode())
            ifaceInst.setStubXml(jsonObj.getString("stubbedXml"));
        return ifaceInst;
    }

}
