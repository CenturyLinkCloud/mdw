package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.asset.Pagelet.Widget;

public interface WidgetAdapter {

    /**
     * Widget value has been initialized.
     * Adapt widget.value to different type as needed.
     * Example: set widget.value to json array for picklist widget.
     */
    void didInit(Widget widget);

    /**
     * Widget value is about to be set on persistable model.
     * Restore widget.value to generic type before saving.
     * Example: reset widget.value to serialized json array for picklist widget.
     */
    void willUpdate(Widget widget);
}
