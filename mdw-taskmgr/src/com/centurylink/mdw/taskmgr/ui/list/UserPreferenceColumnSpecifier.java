/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class UserPreferenceColumnSpecifier implements ColumnSpecifier
{
  public List<ItemUI> getColumns(ListUI listUi)
  {
    List<ItemUI> all = listUi.getVisibleColumns();
    // Get conceal columns (display = false)
    List<ItemUI> concealCol = listUi.getConcealColumns();

    AuthenticatedUser user = FacesVariableUtil.getCurrentUser();

    Map<String,String> prefs = user.getAttributes();
    String columnsPref = prefs.get(listUi.getId() + ":columns");
    if (columnsPref == null)
    {
      all.removeAll(concealCol);
      return all;
    }

    List<ItemUI> some = new ArrayList<ItemUI>();

    StringTokenizer st = new StringTokenizer(columnsPref, ", ");
    while (st.hasMoreTokens())
    {
      String prefsColAttr = st.nextToken();
      for (ItemUI itemUi : all)
      {
        if (itemUi.getName().length() != 0 && prefsColAttr.equals(itemUi.getAttribute()))
          some.add(itemUi);
      }
    }

    // add mandatory columns regardless
    for (ItemUI itemUi : all)
    {
      if (itemUi.getName().length() == 0 || itemUi.getImage() != null)
      {
        if (itemUi.getLinkAction() != null)
          some.add(itemUi);
        else if (itemUi.isCheckbox())
          some.add(0, itemUi);
      }
    }

    return some;
  }

}
