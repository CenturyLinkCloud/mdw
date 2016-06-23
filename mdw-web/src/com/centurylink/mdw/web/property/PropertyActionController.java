/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.property;

import javax.el.ELContext;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.filepanel.FileEdit;
import com.centurylink.mdw.web.property.groups.PropEdit;

public class PropertyActionController
{
  public String saveFile()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    FileEdit fileEdit = (FileEdit) elContext.getELResolver().getValue(elContext, null, "fileEdit");
    fileEdit.saveFile();
    return "go_filePanel";
  }

  public String saveProp()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    PropEdit propEdit = (PropEdit) elContext.getELResolver().getValue(elContext, null, "propEdit");
    propEdit.save();
    return "go_filePanel";
  }
}
