/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class Composer extends Composite {
    private int width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private boolean readOnly;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Composer(Composite parent, int style, int width, boolean readOnly) {
        super(parent, style);
        setWidth(width);
        setReadOnly(readOnly);
    }

    public abstract void setInput(Object input);

    public abstract void setEditable(boolean editable);

    protected Text createText(int width) {
        return createText(width, 1, false);
    }

    protected Text createText(int width, int colspan) {
        return createText(width, colspan, false);
    }

    protected Text createText(int width, int colspan, boolean multi) {
        int style = multi ? SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL
                : SWT.BORDER | SWT.SINGLE;
        Text text = new Text(this, style);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = multi ? width - 8 : width;
        if (multi)
            gd.heightHint = 25;
        if (colspan > 1)
            gd.horizontalSpan = colspan;
        text.setLayoutData(gd);
        return text;
    }

    protected Label createLabel(String text) {
        return createLabel(text, 1);
    }

    protected Label createLabel(String text, int colspan) {
        Label label = new Label(this, SWT.LEFT);
        label.setText(text);
        if (colspan > 1) {
            GridData gd = new GridData(SWT.LEFT);
            gd.horizontalSpan = colspan;
            label.setLayoutData(gd);
        }
        return label;
    }

    protected void createSpacer(int width) {
        createSpacer(width, 1);
    }

    protected void createSpacer(int width, int colspan) {
        Label spacer = new Label(this, SWT.LEFT);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = width;
        if (colspan > 1)
            gd.horizontalSpan = colspan;
        spacer.setLayoutData(gd);
    }

    protected Button createButton(String label) {
        Button button = new Button(this, SWT.PUSH);
        button.setText(label);
        return button;
    }

    private List<ModifyListener> modifyListeners;

    public void addModifyListener(ModifyListener modifyListener) {
        if (modifyListeners == null)
            modifyListeners = new ArrayList<ModifyListener>();
        if (!modifyListeners.contains(modifyListener))
            modifyListeners.add(modifyListener);
    }

    public void removeModifyListener(ModifyListener modifyListener) {
        if (modifyListeners != null)
            modifyListeners.remove(modifyListener);
    }

    public void fireModify(ModifyEvent modEvent) {
        if (modifyListeners != null) {
            for (ModifyListener modifyListener : modifyListeners)
                modifyListener.modifyText(modEvent);
        }
    }

}
