/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.bam;

public class ComponentRelation {

	private Long componentARowid;
	private Long componentBRowid;
	private String relationType;
	public Long getComponentARowid() {
		return componentARowid;
	}
	public void setComponentARowid(Long componentARowid) {
		this.componentARowid = componentARowid;
	}
	public Long getComponentBRowid() {
		return componentBRowid;
	}
	public void setComponentBRowid(Long componentBRowid) {
		this.componentBRowid = componentBRowid;
	}
	public String getRelationType() {
		return relationType;
	}
	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}


}
