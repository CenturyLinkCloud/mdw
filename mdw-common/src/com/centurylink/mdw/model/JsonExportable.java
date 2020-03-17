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
package com.centurylink.mdw.model;

import com.centurylink.mdw.common.service.Query;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Exportable JSON services.
 */
public interface JsonExportable {

    default Jsonable toExportJson(Query query, JSONObject json) {
        return toJsonable(query, json);
    }
    default String getExportName() { return null; }
    default JSONObject getExportFilters(Query query) { return null; }

    /**
     * @deprecated implement {@link #toExportJson(Query, JSONObject)}
     */
    @Deprecated
    default Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        return null;
    }
}
