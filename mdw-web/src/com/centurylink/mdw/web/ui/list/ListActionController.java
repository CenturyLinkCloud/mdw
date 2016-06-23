/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

import com.centurylink.mdw.web.ui.UIException;

public interface ListActionController
{
  /**
   * Called to perform specific logic when an action link is clicked.
   *
   * @param action a logical name for the action to be performed
   * @param listItem the list item corresponding to the clicked link
   * @return the jsf navigation outcome
   */
  public String performAction(String action, ListItem listItem)
    throws UIException;
}
