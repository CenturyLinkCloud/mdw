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

package com.centurylink.mdw.tests.services;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.rest.JsonXmlRestService;

/**
 * Payload should be either JSON or XML, conforming with model for Employee.java class from this pkg
 */
@Path("/HybridService")
public class HybridService extends JsonXmlRestService
{
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        return null;
    }

    /**
     * If the expected response back from the getJson() method is not null, then we need to override getXml() in
     * specific service class to convert to XML response.  Steps would include calling super.getXml(), then creating
     * new JSON object specific to the expected Jsonable class for the response, and then calling
     * JaxbTranslator.realToString(JsonableObject) to marshall it and get the XML string back to return.
     *
     * Here's an example
     *
     * @Override
     * public String getXml(XmlObject xml, Map<String, String> metaInfo) throws ServiceException {
     *      String response = super.getXml(xml, metaInfo);
     *      if (response != null)
     *          response = getJaxbTranslator(pkg).realToString(new MyJsonableClass(response));
     *
     *      return response;
     * }
     */
}
