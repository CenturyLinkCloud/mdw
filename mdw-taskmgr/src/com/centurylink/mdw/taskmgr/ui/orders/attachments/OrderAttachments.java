/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class OrderAttachments extends SortableList {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

	public OrderAttachments(ListUI listUI) {
		super(listUI);
	}

	@Override
	protected DataModel<ListItem> retrieveItems() throws UIException {

		OrderDetail orderDetail = DetailManager.getInstance().getOrderDetail();
	    String masterRequestId = orderDetail.getOrderId();
	    // retrieve the attachments for this order from the workflow
	    try
	    {
	      TaskManager taskMgr = RemoteLocator.getTaskManager();
	      String location = null;
	      String attachPropLocation = WebUtil.getAttachmentLocation();
	      if (MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equals(attachPropLocation)) {
	    	  location = MiscConstants.ATTACHMENT_LOCATION_PREFIX+masterRequestId;
	      } else {
	    	  location = attachPropLocation+masterRequestId;
	      }
	      Collection<Attachment> attachments = taskMgr.getAttachments(null,location);
	      return new ListDataModel<ListItem>(convertOrderAttachments(attachments, masterRequestId));
	    }
	    catch (Exception ex)
	    {
	      String msg = "Problem retrieving order Attachments for : "
	        + masterRequestId + ".";
	      logger.severeException(msg, ex);
	      throw new UIException(msg, ex);
	    }
	}

	/**
	   * Converts a collection of task instance attachments retrieved from the workflow.
	   *
	   * @param taskInstanceAttachments - collection retrieved from db
	   * @param masterRequestId
	   * @return list of ui data model items
	   */
	  protected List<ListItem> convertOrderAttachments(Collection<Attachment> orderAttachments, String masterRequestId)
	  {
	    List<ListItem> rowList = new ArrayList<ListItem>();
	    for (Attachment orderAttachment : orderAttachments) {
	    	OrderAttachmentItem item = new OrderAttachmentItem(orderAttachment);
	    	item.setMasterRequestId(masterRequestId);
	    	rowList.add(item);
	    }
	    return rowList;
	  }

}
