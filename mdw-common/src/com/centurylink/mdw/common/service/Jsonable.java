/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import org.json.JSONException;
import org.json.JSONObject;

import io.swagger.annotations.ApiModelProperty;

public interface Jsonable {

    static final String GENERIC_ARRAY = "genericArray";

    @ApiModelProperty(hidden=true)
    public JSONObject getJson() throws JSONException;

    /**
     * May be pluralized by adding 's'.
     */
    @ApiModelProperty(hidden=true)
    public String getJsonName();
}
