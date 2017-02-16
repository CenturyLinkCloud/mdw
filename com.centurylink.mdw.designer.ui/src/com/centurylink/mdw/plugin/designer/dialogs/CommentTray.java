/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class CommentTray extends DialogTray {
    private String comment = "";

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private Text commentText;

    public Text getCommentText() {
        return commentText;
    }

    public CommentTray() {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        new Label(composite, SWT.NONE).setText("Comments");

        commentText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = 250;
        gd.heightHint = 75;
        commentText.setLayoutData(gd);
        commentText.setTextLimit(1000);
        commentText.setText(comment);
        commentText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                comment = commentText.getText().trim();
            }
        });
        return composite;
    }
}