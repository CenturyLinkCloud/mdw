/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attachments;

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
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class TaskAttachments extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskAttachments(ListUI listUI)
  {
    super(listUI);
  }

  private long mTaskInstanceId;
  public long getTaskInstanceId()
  {
    return mTaskInstanceId;
  }
  public void setTaskInstanceId(long id)
  {
    mTaskInstanceId = id;
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {
    TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
    setTaskInstanceId(Long.parseLong(taskDetail.getInstanceId()));
    String masterRequestId = taskDetail.getMasterRequestId();

    // retrieve the attachments for this task instance from the workflow
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      String attachPropLocation = WebUtil.getAttachmentLocation();
      String location = null;
      if (MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equals(attachPropLocation)) {
    	  location = MiscConstants.ATTACHMENT_LOCATION_PREFIX+taskDetail.getInstanceId();
      } else {
    	  location = attachPropLocation+masterRequestId+"/"+taskDetail.getInstanceId();
      }
      Collection<Attachment> attachments = taskMgr.getAttachments(null,location);
      return new ListDataModel<ListItem>(convertTaskInstanceAttachments(attachments, masterRequestId));
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Task Attachments for instance: "
        + taskDetail.getInstanceId() + ".";
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
  protected List<ListItem> convertTaskInstanceAttachments(Collection<Attachment> taskInstanceAttachments, String masterRequestId)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (Attachment taskInstanceAttachment : taskInstanceAttachments) {
      TaskAttachmentItem item = new TaskAttachmentItem(taskInstanceAttachment);
      item.setMasterRequestId(masterRequestId);
      rowList.add(item);
    }

    return rowList;
  }

}
