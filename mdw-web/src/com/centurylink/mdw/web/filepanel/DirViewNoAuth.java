/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.File;

public class DirViewNoAuth extends DirView
{
  @Override
  protected boolean userCanEdit(File directory, File file)
  {
    return isEditable();  // strictly driven by the faces-config global setting
  }
}
