/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.util;


public class WebLink implements Comparable<WebLink>
{
  private String label;
  public String getLabel() { return label; }

  private String url;
  public String getUrl() { return url; }

  public WebLink(String label, String url)
  {
    this.label = label;
    this.url = url;
  }

  public int compareTo(WebLink other)
  {
    return this.getLabel().compareTo(other.getLabel());
  }
}
