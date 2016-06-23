/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.util.Hashtable;
import java.util.Map;

import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.provider.CacheService;

/**
 * Caches compiled web page entities such as facelets, templates and JSPs.
 */
public class WebPageCache implements CacheEnabled, CacheService
{
  private static Map<String,Object> webPages = new Hashtable<String,Object>();

  @Override
  public void refreshCache() throws CachingException
  {
    clearCache();
  }

  @Override
  public void clearCache()
  {
    webPages.clear();
  }

  public static Object getPage(String key)
  {
    if (key == null)
      return null;
    else
      return webPages.get(key);
  }

  public static void putPage(String key, Object page)
  {
    webPages.put(key, page);
  }

  public static void clear()
  {
    if (webPages != null)
      webPages.clear();
  }

}
