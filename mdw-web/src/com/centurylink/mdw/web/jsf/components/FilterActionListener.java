/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.event.FacesListener;

public interface FilterActionListener extends FacesListener
{
  public void processFilterAction(FilterActionEvent event);
}
