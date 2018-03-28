/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.camel.CamelRouteCache;
import com.centurylink.mdw.camel.RoutesDefinitionRuleSet;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.WorkflowHandler;
import com.centurylink.mdw.workflow.adapter.ObjectAdapterActivity;

public class CamelRouteAdapter extends ObjectAdapterActivity {

    private static final String ROUTE_DEF = "RouteDefinition";
    private static final String ROUTE_DEF_VER = "RouteDefinition_assetVersion";
    private static final String ROUTE_ID = "RouteId";
    private static final String ASYNC = "Asynchronous";
    public static final String CUSTOM_ATTRIBUTES = "CustomAttributes";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public boolean isSynchronous() {
        try {
            String asyncAttr = getAttributeValueSmart(ASYNC);
            return !Boolean.parseBoolean(asyncAttr);
        }
        catch (PropertyException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return true;
    }


    @Override
    protected Object getRequestData() throws ActivityException {
        Object var = super.getRequestData();
        if (!(var instanceof DocumentReference))
            throw new ActivityException("Request variable must be document type: " + getAttributeValue(REQUEST_VARIABLE));

        // currently camel WorkflowHandler can only accept string message types
        // TODO implement camel converters for the MDW document types
        return getDocumentContent((DocumentReference)var);
    }

    @Override
    protected Object invoke(Object pConnection, Object request)
    throws AdapterException, ConnectionException {
        try {
            String routeDefAttr = getAttributeValueSmart(ROUTE_DEF);
            String routeDefVer = getAttributeValueSmart(ROUTE_DEF_VER);
            if (routeDefAttr == null)
                throw new AdapterException("Missing attribute: " + ROUTE_DEF);

            RoutesDefinition routesDef = getRoutesDefinition(routeDefAttr, routeDefVer);

            RouteDefinition routeDef = null;
            if (routesDef.getRoutes().size() == 0) {
                throw new AdapterException("No routes found in " + routesDef);
            }
            if (routesDef.getRoutes().size() > 1) {
                String routeId = getAttributeValueSmart(ROUTE_ID);
                if (routeId == null)
                    throw new AdapterException(ROUTE_ID + " attribute required when route definition contains more than one route");
                for (RouteDefinition route : routesDef.getRoutes()) {
                    if (routeId.equals(route.getId()))
                      routeDef = route;
                }
                if (routeDef == null)
                    throw new AdapterException("Cannot find route ID=" + routeId + " in route definition asset '" + ROUTE_DEF + "'.");
            }
            else {
                routeDef = routesDef.getRoutes().get(0);
            }

            Map<String,Object> headers = getHeaders();
            if (headers == null)
                headers = new HashMap<String,Object>();
            headers.put("routeId", routeDef.getId());

            WorkflowHandler handler = getWorkflowHandler(routeDefAttr);
            if (handler == null)
                throw new EventException("No workflow handler for: " + routeDefAttr + ".  Make sure that the mdw-camel bundle is started");

            return handler.invoke(request, headers);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
    }

    @Override
    protected void handleAdapterSuccess(Object response)
    throws ActivityException, ConnectionException, AdapterException {
        // TODO handle non-string responses from WorkflowHandler
        // by implementing Camel converters for MDW document types
        super.handleAdapterSuccess(response);
    }

    protected WorkflowHandler getWorkflowHandler(String assetName) throws EventException {
        EventServices eventMgr = ServiceLocator.getEventServices();
        return eventMgr.getWorkflowHandler(assetName, getHandlerParameters());
    }

    protected Map<String,String> getHandlerParameters() {
        return null;
//        Map<String,String> params = new HashMap<String,String>();
//        params.put("process", getProcessDefinition().getLabel().replaceAll(" ", ""));
//        params.put("attributeId", getAttributeValue(WorkAttributeConstant.LOGICAL_ID));
//        return params;
    }

    /**
     * Returns the latest version whose attributes match the custom attribute
     * criteria specified via "CustomAttributes".
     * Override to apply additional or non-standard conditions.
     * @param version
     */
    protected RoutesDefinition getRoutesDefinition(String name, String version) throws AdapterException {
        String modifier = "";
        Map<String,String> params = getHandlerParameters();
        if (params != null) {
            for (String paramName : params.keySet()) {
                if (modifier.length() == 0)
                    modifier += "?";
                else
                    modifier += "&amp;";

                modifier += paramName + "=" + params.get(paramName);
            }
        }

        Map<String,String> customAttrs = null;
        String customAttrString = getAttributeValue(CUSTOM_ATTRIBUTES);
        if (!StringHelper.isEmpty(customAttrString)) {
            customAttrs = StringHelper.parseMap(customAttrString);
        }

        RoutesDefinitionRuleSet rdrs;
        if (version == null)
            rdrs = CamelRouteCache.getRoutesDefinitionRuleSet(name, modifier, customAttrs);
        else
            rdrs = CamelRouteCache.getRoutesDefinitionRuleSet(new AssetVersionSpec(name, version), modifier, customAttrs);

        if (rdrs == null) {
            throw new AdapterException("Unable to load Camel route: " + name + modifier);
        }
        else {
            super.logdebug("Using RoutesDefinition: " + rdrs.getRuleSet().getLabel());
            return rdrs.getRoutesDefinition();
        }
    }

    protected Map<String,Object> getHeaders() {
        return null;
    }

    @Override
    protected Object openConnection() throws ConnectionException, AdapterException {
        // not used
        return null;
    }
    @Override
    protected void closeConnection(Object connection) {
        // not used
    }

}
