/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

public class ExtensionModuleException extends Exception
{
  private static final long serialVersionUID = 1L;
  
  public ExtensionModuleException(String message)
  {
    super(message);
  }
  
  public ExtensionModuleException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
