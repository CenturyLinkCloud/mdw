package com.centurylink.mdw.model.asset;

import java.util.List;

import com.centurylink.mdw.model.asset.Pagelet.Widget;

@FunctionalInterface
public interface WidgetProvider {

    List<Widget> getWidgets(String implCategory);

}
