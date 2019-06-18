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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.message.HttpMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Path("/HttpMessages")
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
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        HttpMessage requestMessage = new HttpMessage(content);
        String response = null;
        int code = -1;
        HttpHelper httpClient = null;

        try {
            if (StringUtils.isBlank(requestMessage.getRequestMessage())) {
                response = "Missing payload for HTTP POST method \nRequest: " + content;
            }
            else {
                httpClient = new HttpHelper(new URL(requestMessage.getUrl()));
                httpClient.setHeaders(headers);
                httpClient.getConnection().setReadTimeout(requestMessage.getTimeout() * 1000);
                httpClient.getConnection().setConnectTimeout(requestMessage.getTimeout() * 1000);
                response = httpClient.post(requestMessage.getRequestMessage());
                code = httpClient.getResponseCode();
            }
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + " \nURL: " + requestMessage.getUrl();
        }
        catch (Exception ex) {
            if (httpClient != null) {
                response = ex.getMessage() + " \nResponse: " + httpClient.getResponse();
                code = httpClient.getResponseCode();
            }
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("Post Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }

    @Override
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        String response = null;
        int code = -1;
        HttpMessage requestMessage = new HttpMessage();
        HttpHelper httpClient = null;

        try {
            Query query = getQuery(path, headers);
            if (query.getFilter("timeOut") != null || Integer.parseInt(query.getFilter("timeOut")) != 0) {
                requestMessage.setTimeout(Integer.parseInt(query.getFilter("timeOut")));
            }
            httpClient = new HttpHelper(new URL(query.getFilter("url")));
            httpClient.setFollowRedirects(false);
            httpClient.setHeaders(headers);
            httpClient.setConnectTimeout(requestMessage.getTimeout());
            httpClient.setReadTimeout(requestMessage.getTimeout());
            response = httpClient.get();
            code = httpClient.getResponseCode();
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + " \nURL: " + requestMessage.getUrl();
        }
        catch (Exception ex) {
            if (httpClient != null) {
                response = ex.getMessage() + " \nResponse: " + httpClient.getResponse();
                code = httpClient.getResponseCode();
                if (code != 200)
                    throw new ServiceException(response);
            }
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("Get Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }

    @Override
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        HttpMessage requestMessage = new HttpMessage(content);
        String response = null;
        int code = -1;
        HttpHelper httpClient = null;

        try {
            if (StringUtils.isBlank(requestMessage.getRequestMessage())) {
                response = "Missing payload for HTTP PUT method \nRequest: " + content;
            }
            else {
                httpClient = new HttpHelper(new URL(requestMessage.getUrl()));
                httpClient.setHeaders(headers);
                httpClient.setConnectTimeout(requestMessage.getTimeout());
                httpClient.setReadTimeout(requestMessage.getTimeout());

                response = httpClient.put(requestMessage.getRequestMessage());
                code = httpClient.getResponseCode();
            }
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + " \nURL: " + requestMessage.getUrl();
        }
        catch (Exception ex) {
            if (httpClient != null) {
                response = ex.getMessage() + " \nResponse: " + httpClient.getResponse();
                code = httpClient.getResponseCode();
            }
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("PUT Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }

    @Override
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        String response = null;

        HttpMessage requestMessage;
        if (content == null)
            requestMessage = new HttpMessage();
        else {
            requestMessage = new HttpMessage(content);
        }

        int code = -1;
        HttpHelper httpClient = null;

        try {
            Query query = getQuery(path, headers);
            if (query.getFilter("timeOut") != null || Integer.parseInt(query.getFilter("timeOut")) != 0) {
                requestMessage.setTimeout(Integer.parseInt(query.getFilter("timeOut")));
            }
            httpClient = new HttpHelper(new URL(query.getFilter("url")));
            httpClient.setConnectTimeout(requestMessage.getTimeout());
            httpClient.setReadTimeout(requestMessage.getTimeout());

            response = httpClient.delete(query.getFilter("requestMessage"));
            code = httpClient.getResponseCode();
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + " \nURL: " + requestMessage.getUrl();
        }
        catch (Exception ex) {
            if (httpClient != null) {
                response = ex.getMessage() + " \nResponse: " + httpClient.getResponse();
                code = httpClient.getResponseCode();
            }
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("DELETE Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }

    @Override
    public JSONObject patch(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        long before = java.lang.System.currentTimeMillis();
        HttpMessage requestMessage = new HttpMessage(content);
        String response = null;
        int code = -1;
        HttpHelper httpClient = null;

        try {
            if (StringUtils.isBlank(requestMessage.getRequestMessage())) {
                response = "Missing payload for HTTP PATCH method \nRequest: " + content;
            }
            else {
                httpClient = new HttpHelper(new URL(requestMessage.getUrl()));
                httpClient.setHeaders(headers);
                httpClient.setConnectTimeout(requestMessage.getTimeout());
                httpClient.setReadTimeout(requestMessage.getTimeout());

                response = httpClient.patch(requestMessage.getRequestMessage());
                code = httpClient.getResponseCode();
            }
        }
        catch (MalformedURLException e) {
            response = e.getMessage() + " \nURL: " + requestMessage.getUrl();
        }
        catch (Exception ex) {
            if (httpClient != null) {
                response = ex.getMessage() + " \nResponse: " + httpClient.getResponse();
                code = httpClient.getResponseCode();
            }
        }
        finally{
            int responseTime= (int)(java.lang.System.currentTimeMillis() - before);
            if (logger.isDebugEnabled())
                logger.debug("PATCH Call replied with a response: [" + response + "] in time  = " + responseTime);
            requestMessage.setResponseTime(responseTime);
            if (StringUtils.isBlank(response))
                response = "Error: Please check Server side logs";
            requestMessage.setResponse(response);
            requestMessage.setStatusCode(code);
        }
        return requestMessage.getJson();
    }
}