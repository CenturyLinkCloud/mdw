/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.centurylink.mdw.common.ApplicationContext;

public class HttpHelper
{
  private String url;
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

  private Integer timeout = new Integer(15000);
  public Integer getTimeout() { return timeout; }
  public void setTimeout(Integer timeout) { this.timeout = timeout; }

  private String response;
  public String getResponse() { return response; }

  private int statusCode;
  public int getStatusCode() { return statusCode; }

  private int responseTime;  // ms
  public int getResponseTime() { return responseTime; }

  public void sendMessage() throws HttpException, IOException, UnsupportedEncodingException
  {
    if (timeout == null || timeout.longValue() == 0)
    {
      timeout = new Integer(15000);
    }

    HttpClient httpClient = new HttpClient();
    PostMethod postMethod = new PostMethod(url);
    postMethod.getParams().setParameter("http.socket.timeout", timeout);
    postMethod.getParams().setParameter("http.connection.timeout", timeout);
    if (headers != null)
    {
      for (String header : headers.split(","))
      {
        int eq = header.indexOf('=');
        if (eq > 0)
          postMethod.setRequestHeader(header.substring(0, eq).trim(), header.substring(eq + 1).trim());
      }
    }
    RequestEntity reqEntity = new StringRequestEntity(requestMessage,"text/xml", "UTF-8");
    postMethod.setRequestEntity(reqEntity);
    long before = System.currentTimeMillis();
    statusCode = httpClient.executeMethod(postMethod);
    response = postMethod.getResponseBodyAsString();
    responseTime= (int)(System.currentTimeMillis() - before);
  }
}
