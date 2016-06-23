/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.common;

public class Category {

	private Long id;
	private String code;
	private String description;

    /**
     * returns the categoryCode
     */
     public String getCode() { return code; }

     /**
      * Sets the categorycode
      * @param pCode
      */
     public void setCode(String pCode) { this.code = pCode; }

     /**
      * returns category description
      *
      */
     public String getDescription() { return this.description; }

     /**
      * Sets the categorycode
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
