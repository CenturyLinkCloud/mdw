/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.model;

import java.util.List;

import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Base class for providing a handle to a retrieval timer and a list of special
 * columns (those with distinct semantics for retrieval).
 */
public abstract class RetrievalDataModel extends DataModel<ListItem>
{
  private CodeTimer retrievalCodeTimer;

  public CodeTimer getRetrievalCodeTimer()
  {
    return retrievalCodeTimer;
  }

  public void setRetrievalCodeTimer(CodeTimer timer)
  {
    this.retrievalCodeTimer = timer;
  }

  private List<String> specialColumns;

  public List<String> getSpecialColumns()
  {
    return specialColumns;
  }

  public void setSpecialColumns(List<String> columns)
  {
    this.specialColumns = columns;
  }

  private List<String> indexColumns;

  public List<String> getIndexColumns()
  {
    return indexColumns;
  }

  public void setIndexColumns(List<String> indexColumns)
  {
    this.indexColumns = indexColumns;
  }

}
