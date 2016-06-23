/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.bpm.ConfigurationChangeRequestDocument;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ConfigurationHelper;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class FileEdit
{
  private static final Logger _logger = Logger.getLogger(FileEdit.class.getName());

  private String _filePath;
  public String getFilePath() { return _filePath; }
  public void setFilePath(String filePath) throws IOException
  {
    _filePath = filePath;
    readFile();
  }
  public String getFileName()
  {
    if (_filePath == null)
      return _filePath;

    return _filePath.substring(_filePath.lastIndexOf('/') + 1);
  }

  private String _contents;
  public String getContents() { return _contents; }
  public void setContents(String contents) { _contents = contents; }

  private boolean _reactToChange = true;
  public boolean isReactToChange() { return _reactToChange; }
  public void setReactToChange(boolean reactToChange) { _reactToChange = reactToChange; }

  private boolean _applyGlobally;
  public boolean isApplyGlobally() { return _applyGlobally; }
  public void setApplyGlobally(boolean applyGlobally) { _applyGlobally = applyGlobally; }

  public void readFile() throws IOException
  {
    BufferedReader reader = null;
    try
    {
      reader = new BufferedReader(new FileReader(_filePath));
      StringBuffer buffer = new StringBuffer();
      String line;
      while ((line = reader.readLine()) != null)
      {
        buffer.append(line).append('\n');
      }
      _contents = buffer.toString();
    }
    finally
    {
      if (reader != null)
        reader.close();
    }
  }

  public void saveFile()
  {
    String fileName = getFileName();
    _logger.info("Saving property file: " + fileName);

    try
    {

      if (isApplyGlobally())
      {
        ConfigurationChangeRequestDocument doc = ConfigurationChangeRequestDocument.Factory.newInstance();
        doc.addNewConfigurationChangeRequest();
        doc.getConfigurationChangeRequest().setFileContents(_contents);
        doc.getConfigurationChangeRequest().setFileName(fileName);
        doc.getConfigurationChangeRequest().setReactToChange(isReactToChange());
        JMSServices.getInstance().broadcastTextMessage(JMSDestinationNames.CONFIG_HANDLER_TOPIC, doc.toString(), new Integer(0));
        addMessage("Property file " + fileName + " change has been broadcast successfully.");
      }
      else
      {
        ConfigurationHelper.applyConfigChange(fileName, _contents, isReactToChange());
        addMessage("Property file " + fileName + " has been updated successfully.");
      }

      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, Action.Change, Entity.File, new Long(0), fileName);
      userAction.setSource("FilePanel");
      EventManager eventManager = RemoteLocator.getEventManager();
      eventManager.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      _logger.log(Level.SEVERE, ex.getMessage(), ex);
    }

  }

  /**
   * Add a message to the faces context.
   * @param message the message summary
   */
  public void addMessage(String message)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(message, "");
    facesContext.addMessage(null, facesMessage);
  }

  public String toString()
  {
    return "filePath: " + _filePath + "\n"
      + "reactToChange: " + _reactToChange + "\n"
      + "applyGlobally: " + _applyGlobally + "\n"
      + "contents:\n---------\n" + _contents + "\n";
  }


}
