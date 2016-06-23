/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

public class PropertyEditorException extends RuntimeException
{
  private static final long serialVersionUID = -7019706135954207002L;

  public PropertyEditorException(String message)
  {
    super(message);
  }

  public PropertyEditorException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public PropertyEditorException(Throwable cause)
  {
    super(cause);
  }
}
