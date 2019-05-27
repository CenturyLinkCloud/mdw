package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public interface Linkable extends Jsonable {

    /**
     * @param detail level to include in json
     */
    JSONObject getSummaryJson(int detail);

    String getQualifiedLabel();

    default String getObjectName() {
        String simpleName = getClass().getSimpleName();
        return String.valueOf(simpleName.charAt(0)).toLowerCase() + simpleName.substring(1);
    }

    /**
     * Prefix to include with dump().
     */
    default String prefix() {
        return "";
    }


}
