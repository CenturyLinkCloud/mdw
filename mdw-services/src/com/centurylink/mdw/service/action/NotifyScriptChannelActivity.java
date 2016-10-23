/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * External event handler for processing incoming requests.
 */
public class NotifyScriptChannelActivity extends ExternalEventHandlerBase
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  /**
   * Processes the incoming external event message.  Perform custom logic like
   * parsing the request and extracting parameters as process variables, etc.
   * You might then launch a process instance or notify a waiting activity.
   *
   * @param request the raw request message
   * @param xmlBean the document parsed from request
   * @param metaInfo metadata for the request
   * @return a string containing the event response message
   */
  public String handleEventMessage(String request, Object xmlBean, Map<String,String> metaInfo)
      throws EventHandlerException
  {
	 logger.info("\nInside NotifyScriptChannelActivity.handleEventMessage() :" + request);

	 if (logger.isDebugEnabled())
      logger.debug("Event Request:\n" + request);

     String resourceID = "";
     String response = "";
     String ownerID = "" ;
     ActionRequestDocument xmlbean = (ActionRequestDocument)
     		((XmlObject)xmlBean).changeType(ActionRequestDocument.type);
     for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
       if (param.getName().equals("RESPONSE"))
    	   response = param.getStringValue();
       else if (param.getName().equals("ID")) {
            resourceID = param.getStringValue();
        }  else if (param.getName().equals("OWNER_ID")) {
        	ownerID = param.getStringValue();
        }
    }

    try {
    	EventManager eventMgr = ServiceLocator.getEventManager();
        String eventName = "SCRIPT_CHANNEL_ACTIVITY:" + ownerID + "_" + resourceID;
        Long eventInstId = new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
        eventMgr.notifyProcess(eventName, eventInstId, request, 0);
    } catch (Exception ex) {
        logger.severeException(ex.getMessage(), ex);
        throw new EventHandlerException(ex.getMessage());
    }
    ListenerHelper helper = new ListenerHelper();
    return helper.createStandardResponse(0, response , resourceID);

  }

}

