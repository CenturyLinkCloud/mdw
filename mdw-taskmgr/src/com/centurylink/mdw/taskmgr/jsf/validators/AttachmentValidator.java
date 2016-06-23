/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.myfaces.custom.fileupload.UploadedFileDefaultFileImpl;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class AttachmentValidator implements Validator
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void validate(FacesContext context, UIComponent comp, Object value)
  throws ValidatorException
  {
    UploadedFileDefaultFileImpl upFile = null;

    upFile = (UploadedFileDefaultFileImpl) value;
    String fileName = upFile.getName();

    try
    {

      if (upFile.getBytes().length == 0)
      {
        logger.severe("File does not exist filenane: " + fileName);

        FacesMessage message = new FacesMessage();
        message.setDetail(" File does not exist or is zero length.");
        message.setSummary(" File does not exist or is zero length.");
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
        throw new ValidatorException(message);
      }

    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);

      FacesMessage message = new FacesMessage("File name not valid: " + fileName);
      message.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(message);
    }
  }
}
