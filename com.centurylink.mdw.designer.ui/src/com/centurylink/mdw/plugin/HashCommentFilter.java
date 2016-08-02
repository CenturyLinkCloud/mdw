/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.util.HashMap;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class HashCommentFilter implements ICompareFilter
{
  public void setInput(Object input, Object ancestor, Object left, Object right)
  {
  }

  @SuppressWarnings("rawtypes")
  public IRegion[] getFilteredRegions(HashMap lineComparison)
  {
    String line = (String)lineComparison.get(THIS_LINE);
    if (line != null)
    {
      int hash = line.indexOf('#');
      if (hash >= 0)
      {
        IRegion region = new Region(hash, line.length() - hash);
        return new IRegion[]{region};
      }
    }
    return new IRegion[0];
  }

  public boolean isEnabledInitially()
  {
    return true;
  }

  public boolean canCacheFilteredRegions()
  {
    return true;
  }
}
