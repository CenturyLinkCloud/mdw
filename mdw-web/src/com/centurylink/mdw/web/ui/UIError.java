/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;

/**
 * Represents a system error presentation to the webapp user.
 */
public class UIError implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  private String _message;
  public String getMessage()
  {
    if (_message != null)
    {
      return _message;
    }
    else if (getException() != null)
    {
      return findCause(getException()).getMessage();
    }
    else
    {
      return "Unknown Error";
    }
  }
  public void setMessage(String s)
  {
    _message = s;
  }

  private Throwable _exception;
  public Throwable getException()
  {
    if (_exception == null)
    {
      _exception = getUncaught();
      // log uncaught exception
      if (_exception != null)
      {
        System.err.println(_message);
        _exception.printStackTrace();
      }
    }
    
    return _exception;
  }
  public void setException(Throwable ex)
  {
    _exception = ex;
  }

  public UIError()
  {
  }

  public UIError(String message)
  {
    _message = message;
  }

  public UIError(Exception ex)
  {
    _exception = ex;
  }

  public UIError(String message, Throwable ex)
  {
    _message = message;
    _exception = ex;
  }

  public String getStackTrace()
  {
    if (getException() == null)
    {
      return "No StackTrace available";
    }
    {
      return getStackTrace(getException());
    }
  }
  
  private Throwable getUncaught()
  {
    FacesContext context = FacesContext.getCurrentInstance();
    Map<String,Object> requestMap = context.getExternalContext().getRequestMap();
    return (Throwable) requestMap.get("javax.servlet.error.exception");    
  }

  private String getStackTrace(Throwable t)
  {
    StackTraceElement[] elems = t.getStackTrace();
    StringBuffer sb = new StringBuffer();
    sb.append(t.toString());
    sb.append("\n");
    for (int i = 0; i < elems.length; i++)
    {
      sb.append(elems[i].toString());
      sb.append("\n");
    }

    if (t.getCause() != null)
    {
      sb.append("\n\nCaused by:\n");
      sb.append(getStackTrace(t.getCause()));
    }

    return sb.toString();
  }

  private Throwable findCause(Throwable t)
  {
    if (t.getCause() == null)
      return t;
    else
      return findCause(t.getCause());
  }
}
