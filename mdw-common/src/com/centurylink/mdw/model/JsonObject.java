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

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import io.limberest.config.LimberestConfig;

public class JsonObject extends io.limberest.json.JsonObject {

    public static void configure(PropertyManager propertyManager) {
        LimberestConfig.JsonFormat format = io.limberest.json.JsonObject.getFormat();
        format.prettyIndent = propertyManager.get(PropertyNames.MDW_JSON_PRETTY_INDENT, 2);
        format.orderedKeys = propertyManager.get(PropertyNames.MDW_JSON_ORDERED_KEYS, true);
        format.falseValuesOutput = propertyManager.get(PropertyNames.MDW_JSON_FALSE_VALUES_OUTPUT, false);
    }

    public JsonObject() {
    }

    public JsonObject(String source) {
        super(source);
    }
}
