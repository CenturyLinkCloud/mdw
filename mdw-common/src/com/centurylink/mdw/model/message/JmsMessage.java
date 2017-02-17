/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.message;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="JmsMessage", description="Jms Message")
public class JmsMessage implements Serializable, Jsonable {

    public JmsMessage() {}

    private String user;
    @ApiModelProperty(value="cuid of authenticated user", required=true)

    private String endPoint = "<Internal>";
    @ApiModelProperty(value="endPoint", required=true)

    public String getEndpoint() { return endPoint; }
    public void setEndpoint(String endPoint) { this.endPoint = endPoint; }

    private String queueName;
    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }

    private String requestMessage;
    public String getRequestMessage() { return requestMessage; }
    public void setRequestMessage(String message) { this.requestMessage = message; }

    private Integer timeOut = new Integer(10); //seconds
    public Integer getTimeout() { return timeOut; }
    public void setTimeout(Integer timeOut) { this.timeOut = timeOut; }

    private String response;
    public String getResponse() { return response; }

    public void setResponse(String response) {        this.response = response;    }


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

    public JmsMessage(JSONObject json) throws JSONException {
        if (json.has("endPoint"))
            endPoint = json.getString("endPoint");
        if (json.has("timeOut"))
            timeOut = json.getInt("timeOut");
        if (json.has("user"))
            user = json.getString("user");
        if (json.has("queueName"))
            queueName = json.getString("queueName");
        if (json.has("requestMessage"))
            requestMessage = json.getString("requestMessage");
    }

    public String getJsonName() { return "RequestMessage"; }

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
