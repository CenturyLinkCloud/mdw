/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public interface InstanceList<E> {

    @ApiModelProperty(value="Retrieve date")
    public Date getRetrieveDate();
    @ApiModelProperty(value="List count")
    public int getCount();
    @ApiModelProperty(value="Total count for paginated results")
    public long getTotal(); // for pagination

    @ApiModelProperty(value="List items")
    public List<? extends Jsonable> getItems();
    @ApiModelProperty(hidden=true)
    public int getIndex(String id);

}
