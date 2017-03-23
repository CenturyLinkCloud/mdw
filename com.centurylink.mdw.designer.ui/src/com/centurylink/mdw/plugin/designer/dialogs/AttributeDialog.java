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
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class AttributeDialog extends TrayDialog {
    private AttributeVO attributeVO;

    public AttributeDialog(Shell shell, AttributeVO attributeVO) {
        super(shell);
        this.attributeVO = attributeVO;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 10;
        composite.setLayout(layout);
        composite.getShell().setText("Attribute");

        if (attributeVO.getAttributeId() != null) {
            // attribute id
            new Label(composite, SWT.NONE).setText("ID:");
            Text idText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.BEGINNING);
            gd.widthHint = 200;
            idText.setLayoutData(gd);
            idText.setText(attributeVO.getAttributeId().toString());
        }

        // attribute name
        new Label(composite, SWT.NONE).setText("Name:");
        Text nameText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 400;
        nameText.setLayoutData(gd);
        if (attributeVO.getAttributeName() != null)
            nameText.setText(attributeVO.getAttributeName());

        // attribute value
        Label valueLabel = new Label(composite, SWT.NONE);
        gd = new GridData(GridData.BEGINNING);
        gd.verticalAlignment = SWT.TOP;
        valueLabel.setLayoutData(gd);
        valueLabel.setText("Value:");
        Text valueText = new Text(composite,
                SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 400;
        gd.heightHint = 400;
        valueText.setLayoutData(gd);
        if (attributeVO.getAttributeValue() != null)
            valueText.setText(attributeVO.getAttributeValue());

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
        okButton.forceFocus();
    }

}
