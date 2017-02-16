/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.message.HttpMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/HttpMessages")
@Api("Http Helper to send Message")
public class HttpMessages extends JsonRestService {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Role;
    }

    @Override
    @ApiOperation(value="http post call",
    notes="Request must contain a valid URL, payload and user.", response=HttpMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        HttpMessage requestMessage = new HttpMessage(content);
        String response = "Error: Please check Server side logs";
        int code = -1;
        HttpHelper httpClient = null;

        try {
            if (StringHelper.isEmpty(requestMessage.getRequestMessage())) {
                response = "Missing payload for HTTP POST method " + content;
            }
            else {
                User userVO = ServiceLocator.getUserManager().getUser(requestMessage.getUser());
                if (userVO == null)
                    throw new ServiceException("User not found: " + requestMessage.getUser());
                httpClient = new HttpHelper(new URL(requestMessage.getUrl()));
                httpClient.setHeaders(headers);
                if (requestMessage.getTimeout() == null || requestMessage.getTimeout() == 0)
                {
                    requestMessage.setTimeout(new Integer(15000));
                }
                response = httpClient.post(requestMessage.getRequestMessage(), requestMessage.getTimeout(), requestMessage.getTimeout());
                code = httpClient.getResponseCode();
            }
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + requestMessage.getUrl();
        }
        catch (Exception ex) {
            response = ex.getMessage() + " :\n "+ httpClient.getResponse();
            code = httpClient.getResponseCode();
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("Post Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }

    @Override
    @ApiOperation(value="http Get call",
    notes="Request must contain a valid URL and user",
    response=HttpMessage.class)
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        String response = "Error: Please check Server side logs";
        int code = -1;
        HttpMessage requestMessage = new HttpMessage();
        HttpHelper httpClient = null;

        try {
            Query query = getQuery(path, headers);
            String userCuid = headers.get(Listener.AUTHENTICATED_USER_HEADER);
            User userVO = ServiceLocator.getUserManager().getUser(userCuid);
            if (userVO == null)
                throw new ServiceException("User not found: " + userCuid);
            if (query.getFilter("timeOut") == null || Integer.parseInt(query.getFilter("timeOut")) == 0)
            {
                requestMessage.setTimeout(new Integer(15000));
            }
            else {
                requestMessage.setTimeout(Integer.parseInt(query.getFilter("timeOut")));
            }
            httpClient = new HttpHelper(new URL(query.getFilter("url")));
            httpClient.setHeaders(headers);
            httpClient.setConnectTimeout(requestMessage.getTimeout());
            httpClient.setReadTimeout(requestMessage.getTimeout());
            response = httpClient.get();
            code = httpClient.getResponseCode();
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + requestMessage.getUrl();
        }
        catch (Exception ex) {
            response = ex.getMessage() + " :\n "+ httpClient.getResponse();
            code = httpClient.getResponseCode();
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("Get Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }
}