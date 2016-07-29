/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.common;

import java.io.Serializable;

public class StatusCode implements Serializable {

	private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCode() { return id; }

    private String description;
    public String getDescription() { return this.description; }
    public void setDescription(String pDesc) { this.description = pDesc; }

    public String getName() { return description; }

}