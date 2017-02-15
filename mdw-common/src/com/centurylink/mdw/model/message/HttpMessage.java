/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.message;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.Jsonable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="HttpMessage", description="Http Message")
public class HttpMessage implements Serializable, Jsonable {

    public HttpMessage() {}

    private String user;
    @ApiModelProperty(value="cuit of authenticated user", required=true)

    private String url;
    @ApiModelProperty(value="URL", required=true)

    public String getUrl()
    {
        if (url == null)
        {
            url = "http://" + ApplicationContext.getServerHost() + ":" + ApplicationContext.getServerPort()
            + "/" + ApplicationContext.getServicesContextRoot() + "/Services/REST";
        }
        return url;
    }
    public void setUrl(String url) { this.url = url; }

    private String headers;
    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }

    private String requestMessage;
    public String getRequestMessage() { return requestMessage; }
    public void setRequestMessage(String requestMessage) { this.requestMessage = requestMessage; }

    private Integer timeOut = new Integer(15000);
    public Integer getTimeout() { return timeOut; }
    public void setTimeout(Integer timeOut) { this.timeOut = timeOut; }

    private String response;

    public void setResponse(String response) {        this.response = response;    }
    public String getResponse() { return response; }

    private int statusCode;
    @ApiModelProperty(hidden=true)

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public int getStatusCode() { return statusCode; }

    private int responseTime;  // ms

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }
    public int getResponseTime() { return responseTime; }

    public HttpMessage(JSONObject json) throws JSONException {
        if (json.has("timeOut"))
            timeOut = json.getInt("timeOut");
        if (json.has("url"))
            url = json.getString("url");
        if (json.has("user"))
            user = json.getString("user");
        if (json.has("headers"))
            headers = json.getString("headers");
        if (json.has("requestMessage"))
            requestMessage = json.getString("requestMessage");
    }

    public String getJsonName() { return "HttpMessage"; }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("responseTime", getResponseTime());
        json.put("response", getResponse());
        json.put("statusCode", getStatusCode());
        return json;
    }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
}
