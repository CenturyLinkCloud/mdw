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
    notes="Request must contain a valid endpoint, queue name, payload and authenticated user.", response=JmsMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        JmsMessage requestMessage = new JmsMessage(content);
        String response = null;
        int code = -1;
        JMSServices jmsService = JMSServices.getInstance();

        try {
            if (StringHelper.isEmpty(requestMessage.getRequestMessage())) {
                response = "Missing payload for JMS Message \nRequest: " + content;
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
                logger.debug("JMS Call replied within time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringHelper.isEmpty(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }
}