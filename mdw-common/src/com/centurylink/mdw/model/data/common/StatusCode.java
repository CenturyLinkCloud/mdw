/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.common;

import java.io.Serializable;

public class StatusCode implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String description;
	private Long id;

    /**
     * Returns the desc of the  type
     * @return String
     */
    public String getDescription() { return this.description; }

    /**
     * Sets the desc of the  type
     * @param pDesc
     */
    public void setDescription(String pDesc) { this.description = pDesc; }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}