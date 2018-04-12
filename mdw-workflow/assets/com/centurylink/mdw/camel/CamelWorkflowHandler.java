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
import org.apache.camel.Processor;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.event.WorkflowHandler;


// TODO one thread per request
// TODO handle non-string messages and responses by implementing converters for MDW doc types:
// http://camel.apache.org/type-converter.html

public class CamelWorkflowHandler extends CamelHandler implements WorkflowHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public CamelWorkflowHandler(MdwEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    public Object invoke(Object message, Map<String,Object> headers) throws EventException {

        try {
            Exchange exchange = prepareCamelExchange(message, headers);
            getProcessor().process(exchange);
            return exchange.getOut().getBody(String.class);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new EventException(ex.getMessage(), ex);
        }
    }

    public String getAsset() {
        return getEndpoint().getAsset();
    }

    public Map<String,String> getParameters() {
        return getEndpoint().getWorkflowParams();
    }

    public String toString() {
        String ret = getAsset();
        if (getParameters() != null) {
            ret += " {";
            for (String key : getParameters().keySet()) {
                ret += key + getParameters().get(key);
            }
            ret += "}";
        }
        return ret;
    }
}
