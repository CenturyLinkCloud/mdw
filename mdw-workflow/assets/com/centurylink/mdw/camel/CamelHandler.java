/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class CamelHandler {

    private MdwEndpoint endpoint;
    public MdwEndpoint getEndpoint() { return endpoint; }
    
    private Processor processor;
    public Processor getProcessor() { return processor; }
    
    public CamelHandler(MdwEndpoint endpoint, Processor processor) {
        this.endpoint = endpoint;
        this.processor = processor;
    }
    
    protected Exchange prepareCamelExchange(Object request, Map<String,Object> headers) {

        // create a Camel exchange
        Exchange exchange = endpoint.createExchange();
        exchange.setPattern(ExchangePattern.InOut);
        
        Message inMessage = exchange.getIn();

        if (headers != null) {
            // propagate headers
            for (String key : headers.keySet())
                inMessage.setHeader(key, headers.get(key));
        }
        
        // set the body
        inMessage.setBody(request);
        
        return exchange;        
    }

}
