package com.centurylink.mdw.model.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * For aggregation service responses, an InstanceCount Jsonable returns
 * identity and count information for each type in the breakdown.
 */
@ApiModel(value="Aggregate", description="Identity and value info for a breakdown type")
public interface Aggregate {

    long getId();
    String getName();

    @ApiModelProperty(value="Value for an aggregate")
    long getValue();

    @ApiModelProperty(value="Count for a particular aggregate")
    long getCount();
}
