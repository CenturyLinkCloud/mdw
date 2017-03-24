/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
