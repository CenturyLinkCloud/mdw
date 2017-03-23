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
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;

public class DocumentValue implements JsonService {

    public static final String PARAM_DOC_ID = "id";

    /**
     * Gets the straight value as text.
     */
    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }

    /**
     * Gets document content in String-serialized form.
     * For XML and JSON doc vars, this reparses and serializes to ensure consistent formatting.
     */
    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        String docId = metaInfo.get(PARAM_DOC_ID);
        if (docId == null)
            throw new ServiceException("Missing parameter: id is required.");

        return ServiceLocator.getWorkflowServices().getDocumentStringValue(Long.valueOf(docId));
    }
}
