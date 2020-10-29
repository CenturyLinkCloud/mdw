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
