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
package com.centurylink.mdw.services.rest;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;

public abstract class JsonXmlRestService extends JsonRestService implements XmlService {

    /**
     * If the expected response back from the getJson() method is not null, then need to override getXml() in
     * concrete service class to convert to XML response.  Steps would include calling super.getXml(), then creating
     * new JSON object specific to the expected Jsonable class for the response, and then calling
     * JaxbTranslator.realToString(JsonableObject) to marshall it and get the XML string back to return.
     *
     * Here's an example
     *
     * @Override
     * public String getXml(XmlObject xml, Map<String, String> metaInfo) throws ServiceException {
     *      String response = super.getXml(xml, metaInfo);
     *      if (response != null) {
     *          try {
     *              response = getJaxbTranslator(getPkg(metaInfo)).realToString(new Employee(new JSONObject(response)));
     *          }
     *          catch (Exception e) {
     *              throw new ServiceException(e.getMessage());
     *          }
     *      }
     *      return response;
     */
    @Override
    public String getXml(XmlObject xml, Map<String, String> metaInfo) throws ServiceException {
        try {
            Package pkg = getPkg(metaInfo);
            JSONObject jsonObj = null;
            if (xml != null)
                jsonObj = ((Jsonable)getJaxbTranslator(pkg).realToObject(xml.xmlText())).getJson();

            return getJson(jsonObj, metaInfo);
        }
        catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    static String JAXB_TRANSLATOR_CLASS = "com.centurylink.mdw.jaxb.JaxbElementTranslator";
    protected DocumentReferenceTranslator getJaxbTranslator(Package pkg) throws Exception {
        return (DocumentReferenceTranslator)SpringAppContext.getInstance().getVariableTranslator(JAXB_TRANSLATOR_CLASS, pkg);
    }

    protected Package getPkg(Map<String, String> metaInfo) {
        Package pkg = null;
        String[] pathSegments = metaInfo.get(Listener.METAINFO_REQUEST_PATH) != null ? metaInfo.get(Listener.METAINFO_REQUEST_PATH).split("/") : null;
        if (pathSegments != null) {
            String pkgName = null;
            for (int i = 0; i < pathSegments.length - 1; i++) {
                String pathSegment = pathSegments[i];
                if (!pathSegment.startsWith(pathSegment.substring(0,1).toLowerCase()))
                    break;
                if (i == 0)
                    pkgName = pathSegment;
                else
                    pkgName += "." + pathSegment;
            }
            pkg = PackageCache.getPackage(pkgName);
        }
        return pkg;
    }
}
