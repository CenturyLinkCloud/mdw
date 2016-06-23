/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.event;

import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.HttpHelper;


public class EventTrigger
{
  private String message = "Enter the message text as you would receive it from the External System.";
  public String getMessage() { return message; }
  public void setMessage(String msg) { message = msg; }

  private String masterRequestId;
  public String getMasterRequestId() { return masterRequestId; }
  public void setMasterRequestId(String masterReqId) { masterRequestId = masterReqId; }

  private String response;
  public String getResponse() { return response; }

  public void sendMessage() throws Exception
  {
    // JMSServices.getInstance().sendTextMessage(JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, message, 0);

    // use HTTP
    String url = "http://" + ApplicationContext.getServerHost() + ":" + ApplicationContext.getServerPort()
        + "/" + ApplicationContext.getMdwWebContextRoot() + "/Services/REST";

    HttpHelper httpHelper = new HttpHelper(new URL(url));
    response = httpHelper.post(message);
  }
}
