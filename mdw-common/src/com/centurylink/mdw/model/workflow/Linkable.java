package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public interface Linkable extends Jsonable {

    JSONObject getSummaryJson();

    String getQualifiedLabel();

    default String getObjectName() {
        String simpleName = getClass().getSimpleName();
        return String.valueOf(simpleName.charAt(0)).toLowerCase() + simpleName.substring(1);
    }

}
