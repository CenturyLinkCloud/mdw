/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.detail;

import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.taskmgr.ui.detail.Detail;
import com.centurylink.mdw.taskmgr.ui.detail.ModelWrapper;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Default order detail has no functionality out-of-the-box except the
 * ability to display a workflow snapshot and a task list for an order.
 */
public class OrderDetail extends Detail
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String masterRequestId; // To hold the actual orderId or Master RequestId

  public String getMasterRequestId()
  {
    return masterRequestId;
  }

  public void setMasterRequestId(String masterRequestId)
  {
    this.masterRequestId = masterRequestId;
  }

  public OrderDetail(DetailUI detailUI)
  {
    super(detailUI);
  }

  public String getOrderId()
  {
    if (getModelWrapper() == null)
      return null;

    return getModelWrapper().getWrappedId();
  }

  /**
   * Default implementation does nothing but provide the orderId.
   */
  protected void retrieveInstance(final String orderId) throws UIException
  {
    EventManager evntMgr = ServiceLocator.getEventManager();
    List<DocumentVO> docVOList;
    Long documentId = null;
    this.masterRequestId = orderId;
    try
    {
      docVOList = evntMgr.findDocuments(0L, "Order", orderId, null, "N/A", 0L);
      if (docVOList == null || docVOList.isEmpty())
      {
        documentId = evntMgr.createDocument("Order", 0L, "N/A", 0L, orderId, null, " ");
      } else
      {
        documentId = docVOList.get(0).getDocumentId();
      }
    }
    catch (DataAccessException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    final String documentIdStr = documentId == null ? null : documentId.toString();

    setModelWrapper(new ModelWrapper()
    {
      public Object getWrappedInstance()
      {
        return null;
      }

      public String getWrappedId()
      {
        return documentIdStr;
      }
    });

  }

  /**
   * Default implementation returns just the base portion of the URL.
   */
  public String getWorkflowSnapshotUrl()
  {
    String url = null;
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      url = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "workflow.snapshot.image.url");
    }
    catch(PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return url;
  }

}
