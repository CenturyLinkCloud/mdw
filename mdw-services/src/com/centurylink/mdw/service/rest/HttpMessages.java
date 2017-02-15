/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Message")
@Api("HttpHelper to send Message")
public class HttpHelper extends JsonRestService {
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


    /**
     * For http post
     */
    @Override
    @ApiOperation(value="http post call",
    notes="Request must contain a valid URL, body and user.", response=HttpMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        try {
            HttpMessage requestMessage = new HttpMessage(content);
            User userVO = ServiceLocator.getUserManager().getUser(requestMessage.getUser());
            if (userVO == null)
                throw new ServiceException("User not found: " + requestMessage.getUser());

            if (requestMessage.getTimeout() == null || requestMessage.getTimeout() == 0)
            {
                requestMessage.setTimeout(new Integer(15000));
            }

            HttpClient httpClient = new HttpClient();
            PostMethod postMethod = new PostMethod(requestMessage.getUrl());
            postMethod.getParams().setParameter("http.socket.timeout", requestMessage.getTimeout());
            postMethod.getParams().setParameter("http.connection.timeout", requestMessage.getTimeout());
            if (requestMessage.getHeaders()!= null)
            {
                for (String header : requestMessage.getHeaders().split(","))
                {
                    int eq = header.indexOf('=');
                    if (eq > 0)
                        postMethod.setRequestHeader(header.substring(0, eq).trim(), header.substring(eq + 1).trim());
                }
            }
            RequestEntity reqEntity = new StringRequestEntity(requestMessage.getRequestMessage(),"text/xml", "UTF-8");
            postMethod.setRequestEntity(reqEntity);
            long before = java.lang.System.currentTimeMillis();
            requestMessage.setStatusCode(httpClient.executeMethod(postMethod));
            String response = postMethod.getResponseBodyAsString();
            requestMessage.setResponse(response);
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);

            if (logger.isDebugEnabled())
                logger.debug("Replied with a response: [" + response + "] in time  = " + responseTime);

            requestMessage.setResponseTime(responseTime);
            return requestMessage.getJson();

        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    @ApiOperation(value="http Get call",
    notes="Request must contain a valid URL and user",
    response=HttpMessage.class)
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        try {
            HttpMessage requestMessage = new HttpMessage();
            Query query = getQuery(path, headers);
            String userCuid = headers.get(Listener.AUTHENTICATED_USER_HEADER);
            User userVO = ServiceLocator.getUserManager().getUser(userCuid);
            if (userVO == null)
                throw new ServiceException("User not found: " + userCuid);
            requestMessage.setUser(userCuid);
            if (query.getFilter("timeOut") == null || Integer.parseInt(query.getFilter("timeOut")) == 0)
            {
                requestMessage.setTimeout(new Integer(15000));
            }
            else {
                requestMessage.setTimeout(Integer.parseInt(query.getFilter("timeOut")));
            }
            requestMessage.setUrl(query.getFilter("url"));

            HttpClient httpClient = new HttpClient();
            GetMethod getMethod = new GetMethod(requestMessage.getUrl());
            getMethod.getParams().setParameter("http.socket.timeout", requestMessage.getTimeout());
            getMethod.getParams().setParameter("http.connection.timeout", requestMessage.getTimeout());

            requestMessage.setHeaders(query.getFilter("headers"));

            if (requestMessage.getHeaders() != null)
            {
                for (String header : requestMessage.getHeaders().split(","))
                {
                    int eq = header.indexOf('=');
                    if (eq > 0)
                        getMethod.setRequestHeader(header.substring(0, eq).trim(), header.substring(eq + 1).trim());
                }
            }

            long before = java.lang.System.currentTimeMillis();
            requestMessage.setStatusCode(httpClient.executeMethod(getMethod));
            String response = getMethod.getResponseBodyAsString();
            requestMessage.setResponse(response);
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);

            if (logger.isDebugEnabled())
                logger.debug("Replied with a response: [" + response + "] in time  = " + responseTime);

            requestMessage.setResponseTime(responseTime);
            return requestMessage.getJson();

        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}