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
package com.centurylink.mdw.common.service;

import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public interface InstanceList<E> {

    @ApiModelProperty(value="Retrieve date (UTC)")
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
