/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.attachments;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Wraps a model Attachment instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public abstract class AttachmentItem extends ListItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Attachment attachment;
  public Attachment getAttachment() { return attachment; }
  
  private String masterRequestId;
  public String getMasterRequestId() { return masterRequestId; }
  public void setMasterRequestId(String id) { this.masterRequestId = id; }
  
  /**
   * Add the attachment.
   */
  public abstract void add();

  /**
   * Delete the attachment.
   */
  public abstract void delete();
  

  public AttachmentItem(Attachment attachment)
  {
    this.attachment = attachment;
  }

  /**
   * No-arg constructor for adding new attachments.
   */
  public AttachmentItem()
  {
    this.attachment = new Attachment();
  }

  public void clear()
  {
    attachment = new Attachment();
  }

  public Long getId()
  {
    return attachment.getId();
  }

  public Date getCreatedDate()
  {
    return attachment.getCreatedDate();
  }

  public void setCreatedDate(Date d)
  {
    attachment.setCreatedDate(d);
  }

  public String getCreatedBy()
  {
    return attachment.getCreatedBy();
  }

  public void setCreatedBy(String s)
  {
    attachment.setCreatedBy(s);
  }  
  
  public Date getModifiedDate()
  {
    if (attachment.getModifiedDate() == null)
      return attachment.getCreatedDate();
    else
      return attachment.getModifiedDate();
  }

  public void setModifiedDate(Date d)
  {
    attachment.setModifiedDate(d);
  }

  public String getModifiedBy()
  {
    if (attachment.getModifiedBy() == null)
      return attachment.getCreatedBy();
    else
      return attachment.getModifiedBy();
  }

  public void setModifiedBy(String s)
  {
    attachment.setModifiedBy(s);
  }

  public String getName()
  {
    return attachment.getAttachmentName();
  }

  public void setName(String s)
  {
    attachment.setAttachmentName(s);
  }

  public String getLocation()
  {
    return attachment.getAttachmentLocation();
  }

  public void setLocation(String s)
  {
    attachment.setAttachmentLocation(s);
  }

  public String getContentType()
  {
    return attachment.getAttachmentContentType();
  }

  public void setContentType(String s)
  {
    attachment.setAttachmentContentType(s);
  }

  public Long getOwnerId()
  {
    return attachment.getOwnerId();
  }

  public void setOwnerId(Long ownerId)
  {
    attachment.setOwnerId(ownerId);
  }
  
  public String getOwner()
  {
    return attachment.getOwnerType();
  }

  public void setOwner(String owner)
  {
    attachment.setOwnerType(owner);
  }

  public String getDownloadUrl()
  {
    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    return propMgr.getStringProperty(PropertyNames.ATTACHMENTS_DOWNLOAD_SERVLET_URL);
  }

  public String getOnClickHandlerArg()
  {
    try
    {
      return getDownloadUrl()
        + "?masterRequestId=" + getMasterRequestId()
        + "&ownerId=" + getOwnerId()
        + "&fileName=" + URLEncoder.encode(getName(), "UTF-8")
        + "&contentType=" + getContentType();
    }
    catch (UnsupportedEncodingException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

}
