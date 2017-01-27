/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class DocumentationEditorValueProvider extends ArtifactEditorValueProvider
{
  public static final String MARKDOWN = "Markdown";
  public static final String MS_WORD = "MS Word";

  private WorkflowElement element;

  public DocumentationEditorValueProvider(WorkflowElement element)
  {
    super(element);
    this.element = element;
  }

  public byte[] getArtifactContent()
  {
    String text = element.getAttribute(getAttributeName());
    if (MS_WORD.equals(getLanguage()))
    {
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
          return null;
        }
      }
      else
      {
        return decodeBase64(text);
      }
    }
    else
    {
      if (text == null)
        return " ".getBytes();
      else
        return text.getBytes();
    }

  }

  @Override
  public boolean isBinary()
  {
    return MS_WORD.equals(getLanguage());
  }

  public String getArtifactTypeDescription()
  {
    return "Documentation";
  }

  public String getEditLinkLabel()
  {
    boolean hasContent = element.getAttribute(getAttributeName()) != null;
    String action = element.isReadOnly() ? "View" : "Edit";
    return "<A>" + action + " Documentation</A>" + (hasContent ? " *" : "");
  }

  public List<String> getLanguageOptions()
  {
    List<String> languages = new ArrayList<String>();
    languages.add(MARKDOWN);
    languages.add(MS_WORD);
    return languages;
  }

  public String getDefaultLanguage()
  {
    return MARKDOWN;
  }

  public String getLanguage()
  {
    return MARKDOWN;
  }

  public void languageChanged(String newLanguage)
  {
  }

  public String getAttributeName()
  {
    return WorkAttributeConstant.DOCUMENTATION;
  }

}