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
package com.centurylink.mdw.service.rest;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.services.rest.JsonRestService;

public class Bulletins extends JsonRestService {

    @Override
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {
        JSONObject json = new JSONObject();
        JSONArray bulletinsArr = new JSONArray();
        json.put("bulletins", bulletinsArr);
        for (Bulletin bulletin : SystemMessages.getBulletins().values()) {
            bulletinsArr.put(bulletin.getJson());
        }
        return json;
    }
}
