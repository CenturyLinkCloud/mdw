/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

/**
 * UIException denotes problem in processing View definition
 * defined in the layout configuration xml file.
 */
public class UIException extends Exception
{
  private static final long serialVersionUID = -5470377999419675097L;

  public UIException(String message)
  {
    super(message);
  }
  public UIException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

}
