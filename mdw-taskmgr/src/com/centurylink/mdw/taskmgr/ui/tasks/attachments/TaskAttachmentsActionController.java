/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attachments;

import java.util.Collection;

import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.attachments.AttachmentsActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class TaskAttachmentsActionController extends AttachmentsActionController
{
  public String getManagedBeanName()
  {
    return "taskAttachmentItem";
  }

  public String getNavigationOutcome()
  {
    return "go_taskAttachments";
  }
  
  @Override
  public String getAttachmentDocumentOwner() throws PropertyException 
  {
	 return getOwner();
  }
  
  public Long getProcessInstanceId() throws Exception
  {
	  TaskManager taskMgr = RemoteLocator.getTaskManager();
	  TaskInstanceVO taskInstanceVO = taskMgr.getTaskInstance(getAttachmentDocumentOwnerId());
	  return taskInstanceVO.getOwnerId();
  }
  
  public String getAttachmentProtocol() 
  {
	 return "TASK";
  }
  
  public Long getOwnerId() throws UIException
  { 
	try {
		String attachmentLocation = WebUtil.getAttachmentLocation();
		if (attachmentLocation.startsWith(MiscConstants.DEFAULT_ATTACHMENT_LOCATION)) { 
			TaskManager taskMgr = RemoteLocator.getTaskManager();
			Collection<Attachment> attachmentList = taskMgr.getAttachments(null, 
					MiscConstants.ATTACHMENT_LOCATION_PREFIX+DetailManager.getInstance().getTaskDetail().getInstanceId());
			Long documentId = 0L;
			for (Attachment attachment : attachmentList) {
				if (OwnerType.DOCUMENT.equals(attachment.getOwnerType())) {
					documentId = attachment.getOwnerId();
				}
			}
			return documentId;
		} else {
			return Long.parseLong(DetailManager.getInstance().getTaskDetail().getInstanceId()); 
		}
	} catch (Exception ex) {
		throw new UIException(ex.getMessage());
	}
  }
  
  public String getMasterRequestId() throws UIException
  {
    return DetailManager.getInstance().getTaskDetail().getFullTaskInstance().getOrderId();
  }
  
  public String getActionControllerName()
  {
    return "taskAttachmentsActionController";
  }

	@Override
  public Long getAttachmentDocumentOwnerId() throws UIException {
		return Long.parseLong(DetailManager.getInstance().getTaskDetail().getInstanceId()); 
  }

	@Override
  public String getOwner() throws PropertyException {
		String attachmentLocation = WebUtil.getAttachmentLocation();
		if (attachmentLocation.startsWith(MiscConstants.DEFAULT_ATTACHMENT_LOCATION)) { 
			return OwnerType.DOCUMENT;
		} else {
			return OwnerType.TASK_INSTANCE;
		}
  }

	@Override
	public String getAttachmentLocationEntityId() throws UIException {
		return DetailManager.getInstance().getTaskDetail().getInstanceId();
	}
}
