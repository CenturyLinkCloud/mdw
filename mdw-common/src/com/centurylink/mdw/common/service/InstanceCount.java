/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * For aggregation service responses, an InstanceCount Jsonable returns
 * identity and count information for each type in the breakdown.
 */
@ApiModel(value="Aggregation count", description="Identity and identity info for a breakdown type")
public interface InstanceCount {

    @ApiModelProperty(value="Count for a particular aggregate")
    public long getCount();
}
