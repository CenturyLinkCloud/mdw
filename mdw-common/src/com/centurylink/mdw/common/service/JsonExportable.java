/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marker interface for exportable Json services.
 */
public interface JsonExportable {

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException;

}
