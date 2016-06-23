/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

public class TabContainer extends DojoLayout
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.TabContainer";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.TabContainerRenderer";

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[1];
    values[0] = super.saveState(context);
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
  }
}
