/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class TestCaseSection extends PropertySection implements IFilter {
    private AutomatedTestCase testCase;

    public AutomatedTestCase getTestCase() {
        return testCase;
    }

    private PropertyEditor testNameEditor;
    private PropertyEditor commandFileEditor;
    private PropertyEditor commandEditEditor;
    private PropertyEditor statusEditor;
    private PropertyEditor startTimeEditor;
    private PropertyEditor endTimeEditor;
    private PropertyEditor messageEditor;

    public void setSelection(WorkflowElement selection) {
        testCase = (AutomatedTestCase) selection;

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

        testNameEditor.setElement(testCase);
        testNameEditor.setValue(testCase.getName());

        commandFileEditor.setElement(testCase);
        commandFileEditor.setValue(testCase.getCommandsFile().getLocation().toOSString());

        commandEditEditor.setElement(testCase);

        statusEditor.setElement(testCase);
        statusEditor.setValue(testCase.getStatus());

        startTimeEditor.setElement(testCase);
        if (testCase.getStartTime() == null)
            startTimeEditor.setValue("");
        else
            startTimeEditor.setValue(sdf.format(testCase.getStartTime()));

        endTimeEditor.setElement(testCase);
        if (testCase.getEndTime() == null)
            endTimeEditor.setValue("");
        else
            endTimeEditor.setValue(sdf.format(testCase.getEndTime()));

        messageEditor.setElement(testCase);
        messageEditor.setValue(testCase.getMessage());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        testCase = (AutomatedTestCase) selection;

        testNameEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        testNameEditor.setLabel("Test Name");
        testNameEditor.setWidth(250);
        testNameEditor.render(composite);
        testNameEditor.setEditable(false);

        commandFileEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        commandFileEditor.setLabel("Command File");
        commandFileEditor.setWidth(600);
        commandFileEditor.render(composite);
        commandFileEditor.setEditable(false);

        commandEditEditor = new PropertyEditor(selection, PropertyEditor.TYPE_LINK);
        commandEditEditor.setLabel("Edit Command File");
        commandEditEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                WorkflowElementActionHandler handler = new WorkflowElementActionHandler();
                handler.open(testCase);
            }
        });
        commandEditEditor.render(composite);

        statusEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        statusEditor.setLabel("Status");
        statusEditor.setWidth(250);
        statusEditor.render(composite);
        statusEditor.setEditable(false);

        startTimeEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        startTimeEditor.setLabel("Start Time");
        startTimeEditor.setWidth(110);
        startTimeEditor.render(composite);
        startTimeEditor.setEditable(false);

        endTimeEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        endTimeEditor.setLabel("End Time");
        endTimeEditor.setWidth(110);
        endTimeEditor.render(composite);
        endTimeEditor.setEditable(false);

        messageEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        messageEditor.setLabel("Message");
        commandFileEditor.setWidth(600);
        messageEditor.render(composite);
        messageEditor.setEditable(false);
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof AutomatedTestCase))
            return false;

        return true;
    }
}
