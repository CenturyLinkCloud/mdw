/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.message.HttpMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Message")
@Api("HttpHelper Post Message")
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
     * For creating a new message
     */
    @Override
    @ApiOperation(value="Create an instance message",
        notes="Message must contain a valid user.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Message", paramType="body", dataType="com.centurylink.mdw.model.message.InstanceNote")})
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



}