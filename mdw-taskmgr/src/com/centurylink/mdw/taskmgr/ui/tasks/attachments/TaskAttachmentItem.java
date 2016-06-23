/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attachments;

import java.io.File;
import java.util.List;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.bpm.MessageDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.attachments.AttachmentItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskAttachmentItem extends AttachmentItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskAttachmentItem()
  {
  }

  public TaskAttachmentItem(Attachment attachment)
  {
    super(attachment);
  }

  public void add()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.addAttachment(getName(), getLocation(), getContentType(),
    		  user.getCuid(),getOwner(),getOwnerId());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void delete()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      if (getLocation() == null) {
    	  return;
      }
      if (getLocation().startsWith(MiscConstants.DEFAULT_ATTACHMENT_LOCATION)) {
    	  EventManager eventMgr = RemoteLocator.getEventManager();
    	  Long documentId = getOwnerId();
    	  DocumentVO documnetVo = eventMgr.getDocumentVO(documentId);
    	  XmlObject xmlObject = XmlObject.Factory.parse(documnetVo.getContent(), Compatibility.namespaceOptions());
			if (xmlObject instanceof MessageDocument) {
				MessageDocument emailDoc = (MessageDocument) xmlObject;
				List<com.centurylink.mdw.bpm.Attachment> attachmentList = emailDoc.getMessage().getAttachmentList();
				int index = 0;
				for (com.centurylink.mdw.bpm.Attachment dbAttachment : attachmentList) {
					if (dbAttachment.getFileName().equals(getName())) {
						break;
					}
					index++;
				}
				attachmentList.remove(index);
				eventMgr.updateDocumentContent(documentId, emailDoc.xmlText());
			}

      } else {
    	  String fileToDelete = getLocation() + getName();
    	  if (logger.isInfoEnabled())
    		  logger.info("Deleting: " + fileToDelete);

    	  boolean success = (new File(fileToDelete)).delete();
    	  if (!success)
    	  {
    		  logger.severe("Unable to delete file: " + fileToDelete);
    	  }
      }
      taskMgr.removeAttachment(getId(), user.getUserId());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  @Override
  public String getOnClickHandlerArg()
  {
    return super.getOnClickHandlerArg();
  }
}
