/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.common;

import java.io.Serializable;
import java.util.Date;

public class Attachment implements Serializable {

	private static final long serialVersionUID = 1L;

    public static final Integer STATUS_ATTACHED = new Integer(1);
    public static final Integer STATUS_DETTACHED = new Integer(2);
	
    private String attachmentName;
    private String attachmentLocation;
    private String contentType;
    private Long id;
    private String ownerType;
    private Long ownerId;
	private Date createdDate, modifiedDate;
	private String createdBy, modifiedBy;
    
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String name) { this.attachmentName = name; }

    public String getAttachmentLocation() { return this.attachmentLocation; }
    public void setAttachmentLocation(String location) { this.attachmentLocation = location; }

    public String getAttachmentContentType() { return contentType; }
    public void setAttachmentContentType(String contentType) { this.contentType = contentType; }

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getOwnerType() { return ownerType; }
	public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

	public Long getOwnerId() { return ownerId; }
	public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

	public Date getCreatedDate() { return createdDate; }
	public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

	public Date getModifiedDate() { return modifiedDate; }
	public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	public String getModifiedBy() { return modifiedBy; }
	public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

}
