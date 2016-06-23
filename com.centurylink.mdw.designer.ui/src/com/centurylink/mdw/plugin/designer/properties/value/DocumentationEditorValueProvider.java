/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

public class DocumentationEditorValueProvider extends ArtifactEditorValueProvider
{
  private WorkflowElement element;
  private boolean epfSupport;

  public DocumentationEditorValueProvider(WorkflowElement element, boolean epfSupport)
  {
    super(element);
    this.element = element;
    this.epfSupport = epfSupport;
  }

  public byte[] getArtifactContent()
  {
    String text = element.getAttribute(getAttributeName());
    if (text == null || text.trim().length() == 0)
    {
      try
      {
        URL localUrl = PluginUtil.getLocalResourceUrl("templates/word/Empty.docx");
        File templateFile = new File(new URI(localUrl.toString()));
        return PluginUtil.readFile(templateFile);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Open Documentation", getProject());
      }
    }

    return decodeBase64(text);
  }

  @Override
  public boolean isBinary()
  {
    return true;
  }

  public String getArtifactTypeDescription()
  {
    return "Documentation";
  }

  public String getEditLinkLabel()
  {
    boolean hasContent = element.getAttribute(getAttributeName()) != null;
    return "<A>Open Documentation</A>" + (hasContent ? " *" : "");
  }

  public List<String> getLanguageOptions()
  {
    List<String> languages = new ArrayList<String>();
    languages.add("MS Word");
    if (epfSupport)
      languages.add("HTML");
    return languages;
  }

  public String getDefaultLanguage()
  {
    return "MS Word";
  }

  public String getLanguage()
  {
    return "MS Word";
  }

  public void languageChanged(String newLanguage)
  {
  }

  public String getAttributeName()
  {
    return WorkAttributeConstant.DOCUMENTATION;
  }

}