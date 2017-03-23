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
package com.centurylink.mdw.plugin.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.centurylink.mdw.plugin.MdwPlugin;

/**
 * Preference page base class
 */
public abstract class PreferencePage extends org.eclipse.jface.preference.PreferencePage
        implements IWorkbenchPreferencePage {
    protected abstract Control createContents(Composite parent);

    protected abstract void storeValues();

    protected abstract void initializeValues();

    protected abstract void initializeFromDefaults();

    protected abstract void setDefaultValues();

    public abstract boolean validate();

    public PreferencePage(String title) {
        super(title);
    }

    public void init(IWorkbench workbench) {
        setDefaultValues();
    }

    /**
     * creates composite control and sets the default layout data
     *
     * @param parent
     *            the parent of the new composite
     * @param numColumns
     *            the number of columns for the new composite
     * @return the newly-created composite
     */
    protected Composite createComposite(Composite parent, int numColumns) {
        Composite composite = new Composite(parent, SWT.LEFT);

        // GridLayout
        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        composite.setLayout(layout);

        // GridData
        GridData data = new GridData();
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);
        return composite;
    }

    protected Composite createComposite(Composite parent, int numColumns, int span) {
        Composite composite = new Composite(parent, SWT.LEFT);

        // GridLayout
        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        composite.setLayout(layout);

        // GridData
        GridData data = new GridData();
        data.horizontalSpan = span;
        composite.setLayoutData(data);
        return composite;
    }

    /**
     * utility method that creates a label instance and sets the layout data,
     * defaulting the colspan to 2
     *
     * @param parent
     *            the parent for the new label
     * @param text
     *            the text for the new label
     * @return the new label
     */
    protected Label createLabel(Composite parent, String text) {
        return createLabel(parent, text, 2);
    }

    /**
     * utility method that creates a label instance and sets the default layout
     * data
     *
     * @param parent
     *            the parent for the new label
     * @param text
     *            the text for the new label
     * @param span
     *            columns to span horizontally
     * @return the new label
     */
    protected Label createLabel(Composite parent, String text, int span) {
        Label label = new Label(parent, SWT.LEFT);
        label.setText(text);
        GridData data = new GridData();
        data.horizontalSpan = span;
        data.horizontalAlignment = GridData.FILL;
        label.setLayoutData(data);
        return label;
    }

    /**
     * utility method that creates a push button instance and sets the default
     * layout data.
     *
     * @param parent
     *            the parent for the new button
     * @param label
     *            the label for the new button
     * @return the newly-created button
     */
    protected Button createButton(Composite parent, String label) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.BEGINNING;
        button.setLayoutData(data);
        return button;
    }

    /**
     * utility method that creates a radio button instance and sets the default
     * layout data.
     *
     * @param parent
     *            the parent for the new button
     * @param label
     *            the label for the new button
     * @return the newly-created button
     */
    protected Button createRadioButton(Composite parent, String label) {
        Button button = new Button(parent, SWT.RADIO | SWT.LEFT);
        button.setText(label);
        GridData data = new GridData();
        button.setLayoutData(data);
        return button;
    }

    /**
     * creates a text field
     *
     * @param parent
     *            the parent of the new text field
     * @param width
     *            the width to make the field
     * @param span
     *            horizontal span
     * @param whether
     *            the text field should be read-only
     * @return the new text field
     */
    protected Text createTextField(Composite parent, int width, int span, boolean readOnly) {
        Text text = null;
        if (readOnly)
            text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        else
            text = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData data = new GridData();
        data.horizontalSpan = span;
        data.horizontalAlignment = GridData.BEGINNING;
        data.widthHint = width;
        data.grabExcessHorizontalSpace = true;
        text.setLayoutData(data);
        return text;
    }

    protected Text createTextField(Composite parent, int width) {
        return createTextField(parent, width, 1, false);
    }

    protected Text createTextField(Composite parent, int width, int span) {
        return createTextField(parent, width, span, false);
    }

    protected Text createIntTextField(Composite parent, int width, int span) {
        Text text = createTextField(parent, width, span);
        text.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                Text text = (Text) e.getSource();
                final String before = text.getText();
                String now = before.substring(0, e.start) + e.text + before.substring(e.end);
                if (!now.isEmpty()) {
                    try {
                        Integer.parseInt(now);
                    }
                    catch (NumberFormatException ex) {
                        e.doit = false;
                    }
                }
            }
        });

        return text;
    }

    protected Combo createCombo(Composite parent, List<String> values, int width) {
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = width;
        combo.setLayoutData(gd);

        combo.removeAll();
        for (String value : values)
            combo.add(value);

        return combo;
    }

    /**
     * creates a text area
     *
     * @param parent
     *            the parent of the new text area
     * @param height
     *            the height to make the area
     * @return the new text area
     */
    protected Text createTextArea(Composite parent, int height, int span) {
        Text text = new Text(parent, SWT.MULTI | SWT.BORDER);
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
        gd.heightHint = height;
        gd.horizontalSpan = span;
        text.setLayoutData(gd);
        return text;
    }

    /**
     * creates a password field specific for this application
     *
     * @param parent
     *            the parent of the new password field
     * @return the new text field
     */
    protected Text createPasswordField(Composite parent, int width) {
        Text text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.BEGINNING;
        data.widthHint = width;
        data.grabExcessHorizontalSpace = true;
        text.setLayoutData(data);
        return text;
    }

    /**
     * creates a new checkbox instance and sets the default layout data
     *
     * @param group
     *            the composite in which to create the checkbox
     * @param label
     *            the string to set into the checkbox
     * @param span
     *            number of columns to span horizontally
     * @return the new checkbox
     */
    protected Button createCheckbox(Composite group, String label, int span) {
        Button button = new Button(group, SWT.CHECK | SWT.LEFT);
        button.setText(label);
        GridData data = new GridData();
        data.horizontalSpan = span;
        button.setLayoutData(data);
        return button;
    }

    protected Spinner createSpinner(Composite group, int min, int max) {
        Spinner spinner = new Spinner(group, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setIncrement(1);
        return spinner;
    }

    /**
     * creates a spacer of one horizontal row
     *
     * @param parent
     *            the parent in which the tab should be created
     */
    protected void createSpacer(Composite parent) {
        createSpacer(parent, 1, 10);
    }

    protected void createSpacer(Composite parent, int span, int height) {
        Label vfiller = new Label(parent, SWT.LEFT);
        GridData gridData = new GridData();
        gridData = new GridData();
        gridData.horizontalSpan = span;
        gridData.horizontalAlignment = GridData.BEGINNING;
        gridData.grabExcessHorizontalSpace = false;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.grabExcessVerticalSpace = false;
        gridData.heightHint = height;
        vfiller.setLayoutData(gridData);
    }

    /**
     * Method declared on PreferencePage
     */
    protected void performDefaults() {
        super.performDefaults();
        initializeFromDefaults();
    }

    /**
     * method declared on PreferencePage
     */
    @SuppressWarnings("deprecation")
    public boolean performOk() {
        boolean valid = validate();

        if (valid) {
            storeValues();
            MdwPlugin.flushSettings();
            MdwPlugin.getDefault().savePluginPreferences();
        }
        return valid;
    }

    /**
     * returns the preference store that belongs to our plugin
     */
    protected IPreferenceStore doGetPreferenceStore() {
        return MdwPlugin.getDefault().getPreferenceStore();
    }

    /**
     * creates and returns a widget group
     * 
     * @param parent
     *            the composite parent
     * @param label
     *            the label for the group
     * @param cols
     *            number of columns to include in layout
     * @span horizontal span columns
     */
    protected Group createGroup(Composite parent, String label, int cols, int span) {
        Group group = new Group(parent, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.numColumns = cols;
        group.setLayout(gl);
        group.setText(label);
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = span;
        group.setLayoutData(gd);

        return group;
    }

    public boolean checkDir(String dir) {
        return new File(dir).isDirectory();
    }

    public boolean checkFile(String file) {
        return new File(file).exists();
    }

    public boolean checkString(String s) {
        return s.trim().length() > 0;
    }

    public boolean checkUrl(String urlString) {
        InputStream is = null;

        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
        }
        catch (MalformedURLException ex) {
            return false;
        }
        catch (IOException ex) {
            return false;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {
                }
            }
        }
        return true;
    }

    public boolean checkInt(String s) {
        try {
            int i = Integer.parseInt(s);
            return i > 0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

}
