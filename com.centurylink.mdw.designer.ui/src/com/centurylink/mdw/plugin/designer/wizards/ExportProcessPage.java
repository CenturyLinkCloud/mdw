/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

public class ExportProcessPage extends ImportExportPage
{
  public ExportProcessPage()
  {
    super("Export MDW Process", "Export XML file for an MDW process.");
  }

  protected String getDefaultFileName()
  {
    return getProcess().getName() + "-" + getProcess().getVersionString() + ".xml";
  }
}
