/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class SwitchButton extends org.mihalis.opal.switchButton.SwitchButton {
    public SwitchButton(Composite parent, int style) {
        super(parent, style);
        setInsideMargin(20, 3);
        setButtonBorderColor(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        setFocusColor(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        setSelectedBackgroundColor(
                parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        setUnselectedBackgroundColor(getSelectedBackgroundColor());
        setUnselectedForegroundColor(getSelectedForegroundColor());
        setButtonBackgroundColor1(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        setButtonBackgroundColor2(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        setRound(false);
    }

    @Override
    public boolean getSelection() {
        return !super.getSelection();
    }
}
