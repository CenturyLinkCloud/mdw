/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.jaxb.JaxbElementTranslator;
import com.centurylink.mdw.listener.RegressionTestEventHandler;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.handler.ServiceRequestHandler;
import com.centurylink.mdw.service.Content;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.util.ResourceFormatter.Format;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;

public class ActionRequestHandler extends ServiceRequestHandler {

    protected static final String SERVICE_PROVIDER_IMPL_PACKAGE = "com.centurylink.mdw.service.action";

    public static final String REFRESH_CACHE = "RefreshCache";
    public static final String SAVE_CONFIG = "SaveConfig";

    public static final String CONTENT_PARAM = "content";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String handleEventMessage(String msg, Object msgObj, Map<String, String> metaInfo)
            throws EventHandlerException {
        if (msgObj instanceof XmlObject) {
            // XML request
            metaInfo.put(Listener.METAINFO_FORMAT, Format.xml.toString());
            metaInfo.put(Listener.METAINFO_CONTENT_TYPE, "text/xml");
            if ("ActionRequest".equals(XmlPath.getRootNodeName((XmlObject)msgObj))) {

                ActionRequestDocument actionRequestDoc = (ActionRequestDocument) ((XmlObject)msgObj).changeType(ActionRequestDocument.type);

                ActionRequest actionRequest = actionRequestDoc.getActionRequest();

                if (actionRequest == null) {
                    try {
                        com.centurylink.mdw.common.service.types.ActionRequest jaxbActionRequest =
                                (com.centurylink.mdw.common.service.types.ActionRequest) new JaxbElementTranslator().getJaxbObject(msg);

                        com.centurylink.mdw.common.service.types.Action action = jaxbActionRequest.getAction();
                        com.centurylink.mdw.common.service.types.Content content = jaxbActionRequest.getContent();

                        Map<String, Object> parameters = new HashMap<String, Object>();
                        for (com.centurylink.mdw.common.service.types.Parameter parameter : action.getParameter()) {
                            parameters.put(parameter.getName(), parameter.getValue());
                        }
                        parameters.put(CONTENT_PARAM, content);

                        XmlService service = (XmlService) getActionServiceInstance(action.getName(), metaInfo);

                        if (service == null)
                            return createErrorResponse("Unable to handle action service: " + action.getName(), Format.xml);

                        String response = service.getXml(parameters, metaInfo);
                        if (response == null)
                            return createSuccessResponse(Format.xml);
                        else
                            return response;
                    }
                    catch (ServiceException ex) {
                        logger.severeException(ex.getMessage(), ex);
                        if (ex.getErrorCode() >= 400)
                            metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
                        return createResponse(ex.getErrorCode(), ex.getMessage(), Format.xml);
                    }
                    catch (Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                        return createErrorResponse(ex, Format.xml);
                    }
                }
                else {
                    Action action = actionRequest.getAction();
                    if (action == null || action.getName() == null)
                        throw new EventHandlerException("Missing Action in request");
                    // compatibility for regression test handler
                    if (action.getName().equals("RegressionTest")) {
                        RegressionTestEventHandler handler = new RegressionTestEventHandler();
                        return handler.handleEventMessage(msg, msgObj, metaInfo);
                    }
                    // compatibility for instance level handler
                    // TODO parameters becomes <String,Object> so type is known -- needed by instance level handler
                    if (action.getName().equals("PerformInstanceLevelAction")) {
                        InstanceLevelActionHandler handler = new InstanceLevelActionHandler();
                        return handler.handleEventMessage(msg, msgObj, metaInfo);
                    }

                    try {
                        Map<String,Object> parameters = new HashMap<String,Object>();
                        for (Parameter parameter : action.getParameterList()) {
                            // TODO: handle all document types
                            if (JSONObject.class.getName().equals(parameter.getType()))
                                parameters.put(parameter.getName(), new JSONObject(parameter.getStringValue()));
                            else
                                parameters.put(parameter.getName(), parameter.getStringValue());
                        }

                        Content content = actionRequest.getContent();
                        if (content != null)
                            parameters.put(CONTENT_PARAM, content);

                        requestId = (String)parameters.get("requestId");

                        // compatibility for individually-registered handlers
                        if (action.getName().equals("RefreshProcessCache"))
                            action.setName(REFRESH_CACHE);
                        else if (action.getName().equals("SaveConfig"))
                            action.setName(SAVE_CONFIG);

                        XmlService service = (XmlService) getActionServiceInstance(action.getName(), metaInfo);

                        if (service == null)
                            return createErrorResponse("Unable to handle action service: " + action.getName(), Format.xml);

                        return service.getXml(parameters, metaInfo);
                    }
                    catch (ServiceException ex) {
                        logger.severeException(ex.getMessage(), ex);
                        if (ex.getErrorCode() >= 400)
                            metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
                        return createResponse(ex.getErrorCode(), ex.getMessage(), Format.xml);
                    }
                    catch (Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                        return createErrorResponse(ex, Format.xml);
                    }
                }
            }
            else {
                // non <ActionRequest> XML -- action is determined by request path
                String action = metaInfo.get(Listener.METAINFO_REQUEST_PATH);
                int slash = action.indexOf('/');
                if (slash > 0 && slash < action.length() - 1)
                    action = action.substring(0, slash);

                try {
                    XmlService service = (XmlService) getActionServiceInstance(action, metaInfo);
                    Map<String,Object> parameters = new HashMap<String,Object>();
                    parameters.put(CONTENT_PARAM, msgObj);
                    return service.getXml(parameters, metaInfo);
                }
                catch (ServiceException ex) {
                    logger.severeException(ex.getMessage(), ex);
                    if (ex.getErrorCode() >= 400)
                        metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
                    return createResponse(ex.getErrorCode(), ex.getMessage(), Format.xml);
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    return createErrorResponse(ex, Format.xml);
                }
            }
        }
        else if (msgObj instanceof JSONObject) {
            try {
                // JSON request
                metaInfo.put(Listener.METAINFO_FORMAT, Format.json.toString());
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, "application/json");
                JSONObject jsonObj = (JSONObject) msgObj;
                String action = null;
                Map<String,Object> parameters = new HashMap<String,Object>();
                if ((jsonObj.has("Action") && jsonObj.get("Action") instanceof JSONObject) ||
                        (jsonObj.has("action") && jsonObj.get("action") instanceof JSONObject)) {
                    JSONObject actionObj = jsonObj.has("Action") ? jsonObj.getJSONObject("Action") : jsonObj.getJSONObject("action");
                    if (actionObj.has("name")) {
                        action = actionObj.getString("name");
                        if (actionObj.has("parameters")) {
                            Object paramsObj = actionObj.get("parameters");
                            if (paramsObj instanceof JSONArray) {
                                JSONArray params = (JSONArray) paramsObj;
                                // TODO: does this ever really work?
                                for (int i = 0; i < params.length(); i++) {
                                    JSONObject param = params.getJSONObject(i);
                                    String paramName = JSONObject.getNames(param)[0];
                                    String value = param.getString(paramName);
                                    parameters.put(paramName, value);
                                }
                            }
                            else {
                                // params is a JSONObject
                                JSONObject params = (JSONObject) paramsObj;
                                String[] names = JSONObject.getNames(params);
                                if (names != null) {
                                    for (String name : names)
                                        parameters.put(name, params.get(name));
                                }
                            }
                        }
                        // top level entities
                        String[] jsonNames = JSONObject.getNames(jsonObj);
                        if (jsonNames != null) {
                            if (jsonNames.length > 1) {
                                for (int i = 0; i < jsonNames.length; i++) {
                                    String paramName = jsonNames[i];
                                    if (!paramName.equals("Action") && !paramName.equals("action")) {
                                        JSONObject value = jsonObj.getJSONObject(paramName);
                                        parameters.put(paramName, value);
                                    }
                                }
                            }
                        }
                    }
                    requestId = (String)parameters.get("requestId");
                    if (action == null)
                        throw new ServiceException("JSON request does not have a named Action element");
                }
                else {
                    // non {Action} JSON -- action is determined by request path
                    action = metaInfo.get(Listener.METAINFO_REQUEST_PATH);
                    int slash = action.indexOf('/');
                    if (slash > 0 && slash < action.length() - 1)
                        action = action.substring(0, slash);

                    parameters.put(CONTENT_PARAM, msgObj);
                }

                JsonService service = (JsonService) getActionServiceInstance(action, metaInfo);
                if (service == null)
                    return createErrorResponse("Unable to handle action service: " + action, Format.json);

                String resp = service.getJson(parameters, metaInfo);
                if (resp == null)
                    return createSuccessResponse(Format.json);
                else
                    return resp;
            }
            catch (ServiceException ex) {
                logger.severeException(ex.getMessage(), ex);
                if (ex.getErrorCode() >= 400)
                    metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
                return createResponse(ex.getErrorCode(), ex.getMessage(), Format.json);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                return createErrorResponse(ex, Format.json);
            }
        }
        else {
            return null;
        }
    }

    protected TextService getActionServiceInstance(String action, Map<String,String> headers) throws ServiceException {
        if (Listener.METAINFO_PROTOCOL_REST.equals(headers.get(Listener.METAINFO_PROTOCOL))) {
            // try new packaging first
            try {
                return getServiceInstance(REST_SERVICE_PROVIDER_PACKAGE, action, headers);
            }
            catch (ServiceException ex) {
                if (!(ex.getCause() instanceof ClassNotFoundException))
                    throw ex;  // otherwise fall back to old packaging below
            }
        }
        return getServiceInstance(SERVICE_PROVIDER_IMPL_PACKAGE, action, headers);
    }
}