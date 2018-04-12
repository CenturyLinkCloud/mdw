/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
