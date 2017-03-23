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
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ExternalEventSection extends PropertySection {
    private ExternalEvent externalEvent;

    public ExternalEvent getExternalEvent() {
        return externalEvent;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor handlerClassPropertyEditor;
    private PropertyEditor handlerLinkPropertyEditor;
    private PropertyEditor messagePatternPropertyEditor;
    private PropertyEditor savePropertyEditor;
    private PropertyEditor externalEventHelpPropertyEditor;
    private PropertyEditor mdwXPathSyntaxHelpPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        externalEvent = (ExternalEvent) selection;

        idPropertyEditor.setElement(externalEvent);
        idPropertyEditor.setValue(externalEvent.getId());

        if(!externalEvent.getProject().checkRequiredVersion(6, 0)) {
            handlerClassPropertyEditor.setElement(externalEvent);
            handlerClassPropertyEditor.setValue(externalEvent.getEventHandler());
            handlerClassPropertyEditor.setEditable(!externalEvent.isReadOnly());

            handlerLinkPropertyEditor.setElement(externalEvent);

            messagePatternPropertyEditor.setElement(externalEvent);
            messagePatternPropertyEditor.setValue(externalEvent.getName());
            messagePatternPropertyEditor.setEditable(!externalEvent.isReadOnly());

            savePropertyEditor.setElement(externalEvent);
            savePropertyEditor.setLabel("Save");
            savePropertyEditor.setEditable(!externalEvent.isReadOnly());

            externalEventHelpPropertyEditor.setElement(externalEvent);
            externalEventHelpPropertyEditor.setValue("/MDWHub/doc/listener.html");

            mdwXPathSyntaxHelpPropertyEditor.setElement(externalEvent);
            mdwXPathSyntaxHelpPropertyEditor.setValue("/MDWHub/doc/xpath.html");
        }
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        externalEvent = (ExternalEvent) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(externalEvent, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        if(!externalEvent.getProject().checkRequiredVersion(6, 0)) {
            // handler class combo
            handlerClassPropertyEditor = new PropertyEditor(externalEvent, PropertyEditor.TYPE_TEXT);
            handlerClassPropertyEditor.setLabel("Handler Class");
            handlerClassPropertyEditor.setWidth(475);
            handlerClassPropertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    externalEvent.setEventHandler((String) newValue);
                }
            });
            handlerClassPropertyEditor.render(composite);

            // handler class link
            handlerLinkPropertyEditor = new PropertyEditor(externalEvent, PropertyEditor.TYPE_LINK);
            handlerLinkPropertyEditor.setLabel("Open Handler Source Code");
            handlerLinkPropertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    externalEvent.getProject().viewSource(externalEvent.getEventHandlerClassName());
                }
            });
            handlerLinkPropertyEditor.render(composite);

            // message pattern text field
            messagePatternPropertyEditor = new PropertyEditor(externalEvent, PropertyEditor.TYPE_TEXT);
            messagePatternPropertyEditor.setLabel("Message Pattern");
            messagePatternPropertyEditor.setWidth(300);
            messagePatternPropertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    externalEvent.setName((String) newValue);
                }
            });
            messagePatternPropertyEditor.render(composite);

            // save button
            savePropertyEditor = new PropertyEditor(externalEvent, PropertyEditor.TYPE_BUTTON);
            savePropertyEditor.setLabel("Save");
            savePropertyEditor.setWidth(65);
            savePropertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    saveExternalEvent();
                }
            });
            savePropertyEditor.render(composite);

            // external event handler help link
            externalEventHelpPropertyEditor = new PropertyEditor(externalEvent,
                    PropertyEditor.TYPE_LINK);
            externalEventHelpPropertyEditor.setLabel("External Event Handler Help");
            externalEventHelpPropertyEditor.render(composite);

            // mdw xpath help link
            mdwXPathSyntaxHelpPropertyEditor = new PropertyEditor(externalEvent,
                    PropertyEditor.TYPE_LINK);
            mdwXPathSyntaxHelpPropertyEditor.setLabel("MDW XPath Syntax Help");
            mdwXPathSyntaxHelpPropertyEditor.setVerticalIndent(-15);
            mdwXPathSyntaxHelpPropertyEditor.render(composite);
        }
    }

    private void saveExternalEvent() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                DesignerProxy designerProxy = externalEvent.getProject().getDesignerProxy();
                designerProxy.saveExternalEvent(externalEvent);
                externalEvent.fireElementChangeEvent(ChangeType.RENAME,
                        externalEvent.getName());
            }
        });
    }
}