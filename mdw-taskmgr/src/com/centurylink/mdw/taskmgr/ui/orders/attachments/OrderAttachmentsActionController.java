/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.attachments;

import java.util.Collection;

import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.attachments.AttachmentsActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class OrderAttachmentsActionController extends
		AttachmentsActionController {

	@Override
	public String getNavigationOutcome() {
		return "go_orderAttachment";
	}

	@Override
	public String getManagedBeanName() {
		return "orderAttachmentItem";
	}

	@Override
	public Long getOwnerId() throws UIException {
		try {
			String attachPropLocation = WebUtil.getAttachmentLocation();
			TaskManager taskMgr = RemoteLocator.getTaskManager();
			Long documentId = 0L;
			String location = null;
	        if (MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equals(attachPropLocation)) {
	    	  location = MiscConstants.ATTACHMENT_LOCATION_PREFIX+getMasterRequestId();
	    	  Collection<Attachment> attachmentList = taskMgr.getAttachments(null, location); 
	    	  for (Attachment attachment : attachmentList) {
	    		  if (OwnerType.DOCUMENT.equals(attachment.getOwnerType())) {
	    			  documentId = attachment.getOwnerId();
	    		  }
	    	  }
	        }
			return documentId;
		} catch (Exception ex) {
			throw new UIException(ex.getMessage());
		}
	}

	@Override
	public String getMasterRequestId() throws UIException {
		return DetailManager.getInstance().getOrderDetail().getOrderId();
	}

	@Override
	public String getActionControllerName() {
		return "orderAttachmentsActionController";
	}

	@Override
	public String getAttachmentDocumentOwner() throws PropertyException {
		return "MASTER_REQUEST_ID";
	}

	@Override
	public String getAttachmentProtocol() {
		return "ORDER";
	}

	@Override
	public Long getProcessInstanceId() throws Exception {
		return new Long("0");
	}

	@Override
	public Long getAttachmentDocumentOwnerId() throws UIException {
		return new Long("0");
	}

	@Override
	public String getOwner() throws PropertyException {
		return OwnerType.DOCUMENT;
	}

	@Override
	public String getAttachmentLocationEntityId() throws UIException {
		return getMasterRequestId();
	}
}
