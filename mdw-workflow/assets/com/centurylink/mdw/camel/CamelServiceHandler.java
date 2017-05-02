/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.services.event.ServiceHandler;

/**
 * TODO needs to spawn one thread per request
 */
public class CamelServiceHandler extends CamelHandler implements ServiceHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public CamelServiceHandler(MdwEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    // TODO consider threading
    public Object invoke(String request, Map<String,String> metaInfo) {
        try {
            Map<String,Object> headers = new HashMap<String,Object>();
            if (metaInfo != null) {
                for (String name : metaInfo.keySet())
                    headers.put(name, metaInfo.get(name));
            }
            Exchange exchange = prepareCamelExchange(request, headers);
            getProcessor().process(exchange);
            return exchange.getOut().getBody(String.class);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);

            // build the response as a string for now as this is fastest
            if (ListenerHelper.isJson(request)) {
                return jsonResponse(ex);
            }
            else {
                return xmlResponse(ex);
            }
        }
    }

    public String getProtocol() {
        return getEndpoint().getProtocol();
    }

    public String getPath() {
        return getEndpoint().getPath();
    }

    protected String jsonResponse(Throwable t) {
        return "{ 'error': '" + t + "'}";
    }

    protected String xmlResponse(Throwable t) {
        return "<mdw:Error xmlns:mdw=\"http://mdw.centurylink.com/services\">\n"
             + "  <mdw:Message>" + t + "</mdw:Message>\n"
             + "</mdw:Error>";
    }

}
