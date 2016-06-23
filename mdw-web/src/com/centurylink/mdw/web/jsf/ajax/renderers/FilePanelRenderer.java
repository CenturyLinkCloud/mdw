/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.filepanel.FileView;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.ajax.components.FilePanel;
import com.centurylink.mdw.web.jsf.dojo.renderers.DojoRenderer;

public class FilePanelRenderer extends DojoRenderer
{

  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderJavascriptResource(facesContext, component, "../script/AjaxRequest.js");
    renderJavascriptResource(facesContext, component, "../script/StringBuffer.js");
    renderJavascriptResource(facesContext, component, "../script/Search.js");
    renderJavascriptResource(facesContext, component, "../script/options.js");
    renderJavascriptResource(facesContext, component, "../script/tail.js");
    renderJavascriptResource(facesContext, component, "../script/Tools.js");

    renderDojoRequire(facesContext, component, "dijit.layout.ContentPane");

    FilePanel filePanel = (FilePanel) component;

    FileView fileView = getFileView();
    fileView.setFilePath(filePanel.getFilePath());
    fileView.setBufferLines(filePanel.getBufferLines());
    fileView.setEscape(filePanel.isEscape());
    fileView.setTailMode(false);

    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement("div", component);
    writer.writeAttribute("id", filePanel.getId(), "id");
    writer.writeAttribute("dojoType", "dijit.layout.ContentPane", null);
    writer.writeAttribute("layoutAlign", "client", null);
    writer.writeAttribute("bufferLines", new Integer(filePanel.getBufferLines()), "bufferLines");
    writer.writeAttribute("refetchThreshold", new Integer(filePanel.getRefetchThreshold()), null);
    writer.writeAttribute("tailInterval", new Integer(filePanel.getTailInterval()), null);
    writer.writeAttribute("sliderIncrementLines", new Integer(filePanel.getSliderIncrementLines()), null);
    writer.writeAttribute("systemAdminUser", String.valueOf(filePanel.isSystemAdminUser()), null);
    writer.writeAttribute("standAloneMode", String.valueOf(filePanel.isStandAloneMode()), null);
    writer.writeAttribute("totalLines", new Integer(fileView.getLineCount()), null);
    writer.writeAttribute("lineIndex", new Integer(fileView.getLineIndex()), null);
    writer.writeAttribute("bufferFirstLine", new Integer(fileView.getBufferFirstLine()), null);
    writer.writeAttribute("bufferLastLine", new Integer(fileView.getBufferLastLine()), null);
    writer.writeAttribute("class", filePanel.getStyleClass(), "styleClass");
    writer.writeAttribute("fileViewFontSize", filePanel.getFontSize(), "fileViewFontSize");
    writer.writeAttribute("tabindex", "0", null);
    writer.startElement("pre", component);
    writer.writeAttribute("class", filePanel.getStyleClass(), "styleClass");
    writer.writeAttribute("style", "font-size:" + filePanel.getFontSize() + ";", null);
    writer.write(fileView.getView());
  }

  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement("pre");
    writer.endElement("div");
  }

  private FileView getFileView()
  {
    return (FileView) FacesVariableUtil.getValue("fileView");
  }
}
