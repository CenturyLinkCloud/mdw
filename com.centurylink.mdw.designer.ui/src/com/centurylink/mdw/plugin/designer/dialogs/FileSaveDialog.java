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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class FileSaveDialog {
    private FileDialog dlg;

    public FileSaveDialog(Shell shell) {
        dlg = new FileDialog(shell, SWT.SAVE);
    }

    public String open() {
        String fileName = null;
        boolean done = false;

        while (!done) {
            fileName = dlg.open();
            if (fileName == null) {
                done = true;
            }
            else {
                File file = new File(fileName);
                if (file.exists()) {
                    MessageBox mb = new MessageBox(dlg.getParent(),
                            SWT.ICON_WARNING | SWT.YES | SWT.NO);
                    mb.setMessage(fileName + " already exists. Do you want to replace it?");
                    done = mb.open() == SWT.YES;
                }
                else {
                    done = true;
                }
            }
        }
        return fileName;
    }

    public String getFileName() {
        return dlg.getFileName();
    }

    public String[] getFileNames() {
        return dlg.getFileNames();
    }

    public String[] getFilterExtensions() {
        return dlg.getFilterExtensions();
    }

    public String[] getFilterNames() {
        return dlg.getFilterNames();
    }

    public String getFilterPath() {
        return dlg.getFilterPath();
    }

    public void setFileName(String string) {
        dlg.setFileName(string);
    }

    public void setFilterExtensions(String[] extensions) {
        dlg.setFilterExtensions(extensions);
    }

    public void setFilterNames(String[] names) {
        dlg.setFilterNames(names);
    }

    public void setFilterPath(String string) {
        dlg.setFilterPath(string);
    }

    public Shell getParent() {
        return dlg.getParent();
    }

    public int getStyle() {
        return dlg.getStyle();
    }

    public String getText() {
        return dlg.getText();
    }

    public void setText(String string) {
        dlg.setText(string);
    }
}
