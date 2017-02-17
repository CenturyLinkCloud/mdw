/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.message.JmsMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/JmsMessages")
@Api("Jms Helper to send Message")
public class JmsMessages extends JsonRestService {
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
    @ApiOperation(value="jms call",
    notes="Request must contain a valid endpoint, queue name, payload and user.", response=JmsMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        JmsMessage requestMessage = new JmsMessage(content);
        String response = null;
        int code = -1;
        JMSServices jmsService = JMSServices.getInstance();

        try {
            if (StringHelper.isEmpty(requestMessage.getRequestMessage())) {
                response = "Missing payload for HTTP POST method \nRequest: " + content;
            }
            else {
                String contextUrl = StringHelper.isEmpty(requestMessage.getEndpoint()) || requestMessage.getEndpoint().equals("<Internal>") ? null : requestMessage.getEndpoint();
                response = jmsService.invoke(contextUrl, requestMessage.getQueueName(), requestMessage.getRequestMessage(), requestMessage.getTimeout());
                code = 0;
            }
        }
        catch (Exception ex) {
            response = ex.getMessage() + " \nSent Message: "+ content;
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("JMS Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringHelper.isEmpty(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }
}