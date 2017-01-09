/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses the application.xml file to extract the specified context root.
 */
public class AppXmlContextRootFinder extends DefaultHandler
{
  private boolean inModule = false;
  private boolean inWeb = false;
  private boolean inContextRoot = false;
  private String moduleId;
  private String contextRoot;
  public String getContextRoot() { return contextRoot; }
  private String moduleToFind;
  
  AppXmlContextRootFinder(String moduleToFind)
  {
    this.moduleToFind = moduleToFind;
  }
  
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attrs) 
    throws SAXException
  {
    if (qName.equals("module"))
    {
      inModule = true;
      moduleId = attrs.getValue("id");
    }
    else if (qName.equals("web"))
    {
      inWeb = true;
    }
    else if (qName.equals("context-root"))
    {
      inContextRoot = true;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    if (inModule && inWeb && inContextRoot && moduleToFind != null && moduleToFind.equals(moduleId))
    {
      contextRoot = new String(ch).substring(start, start + length);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
  {
    if (qName.equals("module"))
    {
      inModule = false;
      moduleId = null;
    }
    else if (qName.equals("web"))
    {
      inWeb = false;
    }
    else if (qName.equals("context-root"))
    {
      inContextRoot = false;
    }
  }
}
