/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MdwInputDialog extends TrayDialog {
    private String message;

    public String getMessage() {
        return message;
    }

    private String input;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    private Text inputTextArea;
    private boolean multiLine;

    private String title = "MDW Input";

    public void setTitle(String title) {
        this.title = title;
    }

    private int width = 200;

    public void setWidth(int w) {
        this.width = w;
    }

    private int height;

    public void setHeight(int h) {
        this.height = h;
    }

    public MdwInputDialog(Shell shell, String message, boolean multiLine) {
        super(shell);
        this.message = message;
        this.multiLine = multiLine;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText(title);

        new Label(composite, SWT.NONE).setText(message);

        int style = multiLine ? SWT.MULTI | SWT.BORDER : SWT.BORDER;
        inputTextArea = new Text(composite, style);
        GridData gd = new GridData(GridData.CENTER);
        gd.widthHint = width;
        if (height != 0)
            gd.heightHint = height;
        else if (multiLine)
            gd.heightHint = 40;
        inputTextArea.setLayoutData(gd);

        if (input != null)
            inputTextArea.setText(input);

        return composite;
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        // set the input
        input = inputTextArea.getText();
        if (input.trim().length() == 0)
            input = null;
        setReturnCode(OK);
        close();
    }
}