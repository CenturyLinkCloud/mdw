package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.message.JmsMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

@Path("/JmsMessages")
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
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        JmsMessage requestMessage = new JmsMessage(content);
        String response = null;
        int code = -1;
        JMSServices jmsService = JMSServices.getInstance();

        try {
            if (StringUtils.isBlank(requestMessage.getRequestMessage())) {
                response = "Missing payload for JMS Message \nRequest: " + content;
            }
            else {
                String contextUrl = StringUtils.isBlank(requestMessage.getEndpoint()) || requestMessage.getEndpoint().equals("<Internal>") ? null : requestMessage.getEndpoint();
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
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }
}