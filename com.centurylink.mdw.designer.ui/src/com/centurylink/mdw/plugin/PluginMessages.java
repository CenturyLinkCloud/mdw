/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ListDialog;

import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.designer.dialogs.MdwMessageDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class PluginMessages
{
  public static Map<Integer,String> MESSAGE_LEVELS = new HashMap<Integer,String>();
  public static final int INFO_MESSAGE = 1;
  public static final int CONNECTION_MESSAGE = 2;
  public static final int DATA_INTEGRITY_MESSAGE = 3;
  public static final int VALIDATION_MESSAGE = 4;
  public static final int WARNING_MESSAGE = 10;
  public static final int ERROR_MESSAGE = 20;
  public static final int NO_MESSAGES = 99;

  static
  {
    MESSAGE_LEVELS.put(new Integer(INFO_MESSAGE), "Info");
    MESSAGE_LEVELS.put(new Integer(CONNECTION_MESSAGE), "Connection");
    MESSAGE_LEVELS.put(new Integer(DATA_INTEGRITY_MESSAGE), "Data Integrity");
    MESSAGE_LEVELS.put(new Integer(VALIDATION_MESSAGE), "Validation");
    MESSAGE_LEVELS.put(new Integer(WARNING_MESSAGE), "Warning");
    MESSAGE_LEVELS.put(new Integer(ERROR_MESSAGE), "Error");
    MESSAGE_LEVELS.put(new Integer(NO_MESSAGES), "None");
  }


  public static void uiError(String message, String title)
  {
    uiMessage(message, title, ERROR_MESSAGE);
  }

  public static void uiMessage(String message, String title, int level)
  {
    if (getActiveWorkbenchWindow() != null)
      uiMessage(getActiveWorkbenchWindow().getShell(), message, title, null, level);
    else
      uiMessage((Shell)null, message, title, null, level);
  }

  public static void uiError(Shell shell, String message, String title, WorkflowProject project)
  {
    uiMessage(shell, message, title, project, ERROR_MESSAGE);
  }

  public static void uiMessage(Shell shell, String message, String title, WorkflowProject project, int level)
  {
    log(message);
    boolean sendMessage = level >= getReportingLevel();
    if (shell != null)
    {
      MdwMessageDialog messageDialog = new MdwMessageDialog(shell, title, message, level);
      messageDialog.open();
      sendMessage = messageDialog.isReportMessage();
    }
    if (sendMessage)
      PluginUtil.sendError(null, title, message, project, level);
  }

  public static void uiError(Throwable t, String title)
  {
    uiError(t, title, null);
  }

  public static void uiError(Throwable t, String title, WorkflowProject project)
  {
    uiMessage(t, title, project, ERROR_MESSAGE);
  }

  public static void uiMessage(Throwable t, String title, WorkflowProject workflowProject, int level)
  {
    if (getActiveWorkbenchWindow() != null)
      uiMessage(getActiveWorkbenchWindow().getShell(), t, title, workflowProject, level);
    else
      uiMessage(null, t, title, null, level);
  }

  public static void uiError(Throwable t, String message, String title, WorkflowProject project)
  {
    uiMessage(t, message, title, project, ERROR_MESSAGE);
  }

  public static void uiMessage(Throwable t, String message, String title, WorkflowProject workflowProject, int level)
  {
    if (getActiveWorkbenchWindow() != null)
      uiMessage(getActiveWorkbenchWindow().getShell(), t, message, title, workflowProject, level);
    else
      uiMessage(null, t, message, title, null, level);
  }

  public static void uiError(String message, String title, WorkflowProject project)
  {
    uiMessage(message, title, project, ERROR_MESSAGE);
  }

  public static void uiMessage(String message, String title, WorkflowProject workflowProject, int level)
  {
    if (getActiveWorkbenchWindow() != null)
      uiMessage(getActiveWorkbenchWindow().getShell(), message, title, workflowProject, level);
    else
      uiMessage((Shell)null, message, title, null, level);
  }

  public static void uiError(Shell shell, Throwable t, String title)
  {
    uiMessage(shell, t, title, null, ERROR_MESSAGE);
  }

  public static void uiError(Shell shell, Throwable t, String title, WorkflowProject project)
  {
    uiMessage(shell, t, title, project, ERROR_MESSAGE);
  }

  public static void uiMessage(Shell shell, Throwable t, String title, WorkflowProject project, int level)
  {
    uiMessage(shell, t, null, title, project, level);
  }

  public static void uiMessage(Shell shell, Throwable t, String message, String title, WorkflowProject project, int level)
  {
    log(t);

    if (t instanceof ValidationException || t.getCause() instanceof ValidationException)
    {
      ValidationException valEx = t instanceof ValidationException ? (ValidationException) t : (ValidationException) t.getCause();
      StringBuffer messageBuf = new StringBuffer();
      valEx.fillInErrors(messageBuf, 3);
      message = messageBuf.toString();
      title = "Validation Error: " + title;
      level = VALIDATION_MESSAGE;
    }

    if (message == null)
      message = PluginMessages.getUserMessage(t);
    boolean sendMessage = level >= getReportingLevel();
    if (shell != null)
    {
      MdwMessageDialog messageDialog = new MdwMessageDialog(shell, title, message, level);
      messageDialog.open();
      sendMessage = messageDialog.isReportMessage();
    }
    if (sendMessage)
      PluginUtil.sendError(t, title, message, project, level);
  }

  public static int uiList(String message, String title, List<?> items)
  {
    Shell shell = getActiveWorkbenchWindow().getShell();
    if (shell == null)
      return -1;

    return uiList(shell, message, title, items);
  }

  public static int uiList(Shell shell, String message, String title, List<?> items)
  {
    return uiList(shell, message, title, items, null);
  }

  public static int uiList(Shell shell, String message, String title, List<?> items, final List<?> flaggedItems)
  {
    ListDialog listDialog = new ListDialog(shell);
    listDialog.setAddCancelButton(true);
    listDialog.setContentProvider(new ArrayContentProvider());

    int width = 0;
    if (items.size() > 0 && items.get(0) instanceof WorkflowElement)
    {
      // calculate dialog width
      for (Object item : items)
      {
        WorkflowElement element = (WorkflowElement) item;
        String label = (element instanceof WorkflowAsset && element.getProject().checkRequiredVersion(5, 5) && flaggedItems == null) ? element.getLabelWithPackage() : element.getLabel();
        if (flaggedItems != null && flaggedItems.contains(item))
          label += " *";
        if (label.length() > width)
          width = label.length();
      }
      listDialog.setLabelProvider(new LabelProvider()
      {
        public Image getImage(Object element)
        {
          return ((WorkflowElement)element).getIconImage();
        }
        public String getText(Object element)
        {
          String flag = flaggedItems != null && flaggedItems.contains(element) ? " *" : "";
          if (element instanceof WorkflowAsset && ((WorkflowAsset)element).getProject().checkRequiredVersion(5, 5) && flaggedItems == null)
            return ((WorkflowAsset)element).getLabelWithPackage() + flag;
          else
            return ((WorkflowElement)element).getLabel() + flag;
        }
      });
    }
    else
    {
      for (Object item : items)
      {
        if (item.toString().length() > width)
          width = item.toString().length();
      }
      listDialog.setLabelProvider(new LabelProvider()
      {
        public String getText(Object element)
        {
          return element.toString();
        }
      });
    }
    listDialog.setInput(items);
    listDialog.setTitle(title);
    listDialog.setMessage(message);
    if (width != 0)
      listDialog.setWidthInChars(width + 2);
    return listDialog.open();
  }

  public static void log(Throwable e)
  {
    log(new Status(IStatus.ERROR, MdwPlugin.getPluginId(), IStatus.ERROR, "Error", e));
  }

  public static void log(IStatus status)
  {
    MdwPlugin.getDefault().getLog().log(status);
  }

  public static void log(String message)
  {
    log(new Status(IStatus.ERROR, MdwPlugin.getPluginId(), IStatus.ERROR, message, null));
  }

  public static void log(String message, int level)
  {
    log(new Status(level, MdwPlugin.getPluginId(), level, message, null));
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow()
  {
    return MdwPlugin.getActiveWorkbenchWindow();
  }

  public static int getReportingLevel()
  {
    int level = MdwPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.PREFS_MDW_REPORTING_LEVEL);
    if (level == 0)
      level = PreferenceConstants.PREFS_DEFAULT_MDW_REPORTING_LEVEL;
    return level;
  }

  public static Throwable getRootCause(Throwable t)
  {
    while (t.getCause() != null)
    {
      t = t.getCause();
    }
    return t;
  }

  public static String getUserMessage(Throwable t)
  {
    String origMsg = t.getMessage();
    if (origMsg == null || origMsg.trim().length() == 0)
      origMsg = getMessage(t);

    String message = getMessage(getRootCause(t));
    if (message == null || message.trim().length() == 0)
      message = origMsg;
    if (message == null || message.trim().length() == 0)
      message = "An unexpected error was encountered.";
    return message;
  }

  public static String getMessage(Throwable t)
  {
    String s = t.getClass().getSimpleName();
    String message = t.getLocalizedMessage();
    return (message != null) ? (s + ": " + message) : s;
  }

  public static boolean isConnectionException(Exception ex)
  {
    String userMsg = getUserMessage(ex);
    if (userMsg != null && userMsg.indexOf("ORA-01017") >= 0)
      return true;
    if (userMsg != null && userMsg.indexOf("Network Adapter could not establish the connection") >= 0)
      return true;

    return false;
  }

  public static boolean isDataIntegrityException(Exception ex)
  {
    String userMsg = getUserMessage(ex);
    if (userMsg != null && (userMsg.indexOf("ORA-02292") >= 0 || userMsg.indexOf("The process has instances") >= 0))
      return true;

    return false;

  }

}
